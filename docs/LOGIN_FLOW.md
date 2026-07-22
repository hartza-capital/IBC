# IBC Login Flow — Technical Reference

This document describes the login subsystem of IBC as implemented in the source files under
`src/ibcalpha/ibc/`. All descriptions are derived exclusively from the code.

---

## 1. Login State Machine

### States (`LoginManager.LoginState`)

```
LOGGED_OUT
AWAITING_CREDENTIALS
LOGGING_IN
TWO_FA_IN_PROGRESS
LOGIN_FAILED
LOGGED_IN
```

### Transition diagram

```
                        ┌─────────────────────────────────────────────────────────────────┐
                        │                                                                 │
                        v                                                                 │
                  LOGGED_OUT                                                              │
                        │                                                                 │
                        │  AbstractLoginHandler.handleWindow()                            │
                        │  (WINDOW_OPENED on login frame, not a restart)                 │
                        v                                                                 │
              AWAITING_CREDENTIALS                                                        │
                        │                                                                 │
                        │  AbstractLoginHandler.initiateLogin()                           │
                        │  credentials validated, Login button clicked                   │
                        v                                                                 │
                   LOGGING_IN ──────────────────────────────────────────────────────┐    │
                        │                                                            │    │
                        │  SecondFactorAuthenticationDialogHandler.handleWindow()    │    │
                        │  (WINDOW_OPENED on 2FA dialog)                            │    │
                        v                                                            │    │
              TWO_FA_IN_PROGRESS                                                     │    │
                        │                                                            │    │
                        │  2FA dialog WINDOW_CLOSED                                 │    │
                        │  -> LoginManager.secondFactorAuthenticationDialogClosed() │    │
                        │                                                            │    │
               ┌────────┴───────────────────────────────────┐                       │    │
               │ elapsed < SecondFactorAuthenticationTimeout │  elapsed >= timeout   │    │
               v                                            v                       │    │
          (login proceeds)                   ReloginAfterSecondFactorAuthentication │    │
               │                             Timeout = true?                        │    │
               │                                     │                              │    │
               │                             yes     │  no                          │    │
               │                              ┌──────┴──────┐                      │    │
               │                              │             │                       │    │
               │                    re-initiate login   log & stop                  │    │
               │                    after 5 s           (no relogin)               │    │
               │                              │                                     │    │
               │                              └─────────────────────────────────────┘    │
               v                                                                          │
          LOGGED_IN ◄──────────────────────────────────────────────────────────────┘    │
               │   (TWS splash screen confirms login, or FIX Gateway shortcut)           │
               │                                                                         │
               │   ReloginDialogHandler: "Re-login is required" dialog                  │
               └─────────────────────────────────────────────────────────────────────────┘

LOGIN_FAILED  <── LoginFailedDialogHandler (triggers cold restart via StopTask)
```

### State-change side effects (in `LoginManager.setLoginState`)

| New state            | Side effect                                                                                 |
| -------------------- | ------------------------------------------------------------------------------------------- |
| `LOGGING_IN`         | Records `LoginStartTime = Instant.now()` if not already set.                                |
| `TWO_FA_IN_PROGRESS` | Logs "Second Factor Authentication initiated"; records `LoginStartTime` if not set.         |
| `LOGGED_IN`          | Logs "Login has completed"; cancels the `shutdownAfterTimeTask` scheduled future if active. |

### Event filtering in `AbstractLoginHandler.filterEvent`

When the login frame receives `WINDOW_OPENED`, the handler decides whether to process it based on the current state:

| Current state        | Process event? |
| -------------------- | -------------- |
| `LOGGED_IN`          | No             |
| `LOGIN_FAILED`       | Yes            |
| `LOGGING_IN`         | No             |
| `TWO_FA_IN_PROGRESS` | No             |
| anything else        | Yes            |

---

## 2. TWS vs Gateway Login Sequences

### Handler selection

The correct concrete handler is selected at runtime by `TwsListener` matching the window title and
button presence against `recogniseWindow()`:

| Handler                    | Window type | Recognition criteria                                                                                                                        |
| -------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| `LoginFrameHandler`        | `JFrame`    | Title equals "New Login" or "Login", **and** button "Log In" or "Paper Log In" present (or session is restart).                             |
| `GatewayLoginFrameHandler` | `JFrame`    | Title contains "IBKR Gateway", "IB Gateway", or "Interactive Brokers Gateway", **and** button "Login", "Log In", or "Paper Log In" present. |

### TWS sequence (`LoginFrameHandler`)

```
WINDOW_OPENED on TWS login frame
        │
        ├─ initialise()
        │     ├─ setTradingMode()           [sets Live/Paper toggle, TWS 974+]
        │     ├─ JtsIniManager.reload()     [reloads jts.ini, modified by TWS before this point]
        │     └─ attaches DocumentListener on username field
        │           -> deferred: sets "Use/store settings on server" checkbox [StoreSettingsOnServer]
        │
        ├─ setFields()
        │     ├─ sets text field [0] = IBAPIUserName
        │     └─ sets text field [1] = IBAPIPassword
        │
        ├─ preLogin()
        │     └─ validates username and password are non-empty; focuses missing field if empty
        │
        └─ doLogin()
              ├─ deferred: clicks "Login" / "Log In" / "Paper Log In" button
              │     -> state = LOGGING_IN
              └─ if TWS >= 1016 (JLabel "LOGIN" present):
                    polls every 200 ms for JLabel "SECOND FACTOR AUTHENTICATION"
                    -> when found, calls SecondFactorAuthenticationDialogHandler.handleWindow()
```

### Gateway sequence (`GatewayLoginFrameHandler`)

```
WINDOW_OPENED on Gateway login frame
        │
        ├─ initialise()
        │     ├─ selectGatewayMode()        [clicks "FIX CTCI" or "IB API"/"TWS/API" toggle]
        │     └─ if "Trading Mode" label present: setTradingMode()
        │
        ├─ setFields()
        │     ├─ FIX mode:
        │     │     field[0] = FIXUserName
        │     │     field[1] = FIXPassword
        │     │     field[2] = IBAPIUserName  (optional IBAPI overlay)
        │     │     field[3] = IBAPIPassword
        │     └─ non-FIX (IB API) mode:
        │           field[0] = IBAPIUserName
        │           field[1] = IBAPIPassword
        │
        ├─ preLogin()
        │     ├─ FIX mode: validates FIX credentials first, then optional IBAPI credentials
        │     └─ non-FIX: validates IBAPI credentials
        │
        └─ doLogin()  [inherited from AbstractLoginHandler — same as TWS]
```

### Key differences TWS vs Gateway

| Aspect                       | TWS (`LoginFrameHandler`)          | Gateway (`GatewayLoginFrameHandler`)                                                           |
| ---------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------- |
| Window title                 | "Login" or "New Login"             | Contains "IBKR Gateway", "IB Gateway", or "Interactive Brokers Gateway"                        |
| Mode selector                | Live/Paper toggle buttons          | FIX CTCI / IB API toggle, then optionally Live/Paper                                           |
| FIX credentials              | Not applicable                     | Supported (fields 0-1 for FIX, 2-3 for IBAPI)                                                  |
| `StoreSettingsOnServer` hook | Yes (DocumentListener on username) | No                                                                                             |
| `jts.ini` reload             | Yes, in `initialise()`             | No                                                                                             |
| `isUserIdDisabledOrAbsent`   | field index 0                      | field index 2 (FIX mode) or 0 (IB API mode)                                                    |
| `isPasswordDisabledOrAbsent` | field index 1                      | field index 3 (FIX mode) or 1 (IB API mode)                                                    |
| FIX Gateway shortcut         | N/A                                | `secondFactorAuthenticationDialogClosed()` sets state to `LOGGED_IN` directly for FIX sessions |

---

## 3. Two-Factor Authentication (2FA) Flow

### 3.1 Dialog detection

`SecondFactorAuthenticationDialogHandler.recogniseWindow()` matches any `JDialog` or `JFrame` whose
title contains "Second Factor Authentication" or "Security Code Card Authentication".

For TWS >= 1016, the login frame itself morphs into the 2FA dialog without generating a new window
event. `AbstractLoginHandler.doLogin()` detects this by polling (every 200 ms) for a `JLabel`
whose text changes from "LOGIN" to "SECOND FACTOR AUTHENTICATION", then calls
`SecondFactorAuthenticationDialogHandler.handleWindow()` directly.

### 3.2 TOTP mode (`Ib2FA` configured)

When `WINDOW_OPENED` fires and no read-only login is required and no device selection is pending,
`doValidate2FA()` executes:

```
doValidate2FA()
    │
    ├─ state = TWO_FA_IN_PROGRESS
    ├─ reads secret = Settings.getString("Ib2FA", "")
    │     └─ if empty: logs error and returns (no code submitted)
    │
    ├─ totp = Totp.generate(secret, System.currentTimeMillis() / 30000)    [in-tree, no external library]
    │     └─ standard TOTP: 30-second time window, HMAC-SHA1, 6 digits
    │
    ├─ SwingUtils.setTextField(window, 0, totp)            [inserts code into first text field]
    │
    └─ deferred: SwingUtils.clickButton(window, "OK")      [submits the dialog]
```

The TOTP secret is read from `config.ini` via the key `Ib2FA` and decoded by the in-tree `Base32`
class (RFC 4648). Code generation is handled by the in-tree `Totp` class, which implements RFC 4226
dynamic truncation over the JDK's own HMAC-SHA1 — HMAC-SHA1 with 6 digits, fixed, with no external
dependency. The time counter is `System.currentTimeMillis() / 30000` (floor-divided, matching
RFC 6238 with T0=0 and time step=30 s).

### 3.3 Multiple 2FA devices (`SecondFactorDevice` configured)

Before attempting TOTP, the handler checks for a text area containing "Select second factor device":

```
secondFactorDeviceSelectionRequired() == true
    │
    ├─ reads secondFactorDevice = Settings.getString("SecondFactorDevice", "")
    │     └─ if empty: logs error, returns without clicking OK
    │
    ├─ iterates JList[0] model entries (trimmed)
    ├─ if entry matches SecondFactorDevice value: selects that list index
    └─ clicks "OK" to confirm device selection
```

After device selection, the 2FA dialog is expected to re-appear (driven by the selected device's
method — TOTP or IBKR Mobile), and `doValidate2FA()` is called on the next `WINDOW_OPENED` event.

### 3.4 IBKR Mobile / manual 2FA mode (no `Ib2FA` configured)

When `Ib2FA` is empty, `doValidate2FA()` logs an error and returns without filling the code field or
clicking OK. The dialog remains open, waiting for the user (or IBKR Mobile push notification) to
complete authentication manually.

The dialog close event (`WINDOW_CLOSED`) then drives the timeout logic described below.

### 3.5 Read-only login mode (`ReadOnlyLogin=true`)

When the 2FA dialog opens and `ReadOnlyLogin=true` (TWS only; not supported by Gateway):

- `doReadonlyLogin()` clicks "Enter Read Only".
- On `WINDOW_CLOSED`: state is set directly to `LOGGED_IN`.

`SecurityCodeDialogHandler` handles a separate JDialog that contains an "Enter Read Only" button
(recognized solely by that button's presence). It also clicks "Enter Read Only" when
`ReadOnlyLogin=true`.

### 3.6 2FA timeout and re-login logic

`LoginManager.secondFactorAuthenticationDialogClosed()` is called when the 2FA dialog's
`WINDOW_CLOSED` event fires (unless `ReadOnlyLogin=true`). It computes the elapsed time since
`LoginStartTime`:

```
elapsed = now - LoginStartTime

if elapsed < SecondFactorAuthenticationTimeout:
    # User interacted with the dialog before timeout
    if FIX Gateway: state = LOGGED_IN, return
    if reloginPermitted():
        schedule shutdown after SecondFactorAuthenticationExitInterval seconds
        (cancelled if state reaches LOGGED_IN before deadline)
    else:
        let loading continue (no action)
else:
    # Timeout: user did NOT interact with the 2FA dialog
    if reloginPermitted():
        schedule initiateLogin() after 5 seconds (re-starts full login sequence)
    else:
        log "Re-login not required", no action
```

`reloginPermitted()` resolution:

```
if "ReloginAfterSecondFactorAuthenticationTimeout" is set:
    return its boolean value
else if "ExitAfterSecondFactorAuthenticationTimeout" is set:
    return its boolean value   (legacy key, same semantics)
else:
    return false
```

`TooManyFailedLoginAttemptsDialogHandler` also checks
`ReloginAfterSecondFactorAuthenticationTimeout` before scheduling a retry after rate-limit expiry.

### 3.7 2FA mode summary

| Condition                             | Handler action                                        |
| ------------------------------------- | ----------------------------------------------------- |
| `ReadOnlyLogin=true`                  | Click "Enter Read Only"; on close set state=LOGGED_IN |
| Device list present                   | Select device from `SecondFactorDevice`; click OK     |
| `Ib2FA` non-empty                     | Generate TOTP; fill field[0]; click OK                |
| `Ib2FA` empty, no device list, not RO | Log error; dialog stays open (IBKR Mobile / manual)   |

---

## 4. Error Cases

### 4.1 Login Failed (`LoginFailedDialogHandler`)

- Recognised: `JDialog` with title containing "Login failed".
- Action: logs "Login failed", triggers a **cold restart** via `StopTask(null, true, ...)`, then
  clicks "OK" to dismiss the dialog.
- State after: not explicitly set (StopTask restarts the process).
- No config.ini setting controls this behaviour.

### 4.2 Too Many Failed Login Attempts (`TooManyFailedLoginAttemptsDialogHandler`)

- Recognised: `JDialog` containing a text area with text "Too many failed login attempts".
- The dialog message is parsed with a regex to extract the wait duration
  (format: "Please wait N minutes & M seconds").
- A 3-second buffer is added: `waitFor = parsed_duration + 3s`.
- **Conditional on `ReloginAfterSecondFactorAuthenticationTimeout=true`:**
  - Logs the scheduled re-login time.
  - Schedules `initiateLogin()` on the existing login frame after `waitFor` seconds.
  - Clicks "OK" to dismiss the dialog.
- If `ReloginAfterSecondFactorAuthenticationTimeout` is false or absent: the dialog is left open
  (no automatic retry, no dismiss).

### 4.3 Existing Session Detected (`ExistingSessionDetectedDialogHandler`)

- Recognised: `JDialog` with title containing "Existing session detected".
- Behaviour is driven by `ExistingSessionDetectedAction` (default: "manual"):

| Setting value               | Login state at detection                  | Action taken                                                                    |
| --------------------------- | ----------------------------------------- | ------------------------------------------------------------------------------- |
| `manual`                    | any                                       | Do nothing; user must choose manually.                                          |
| `secondary`                 | any                                       | Click "Cancel" or "Exit Application"; this session terminates.                  |
| `primary`/`primaryoverride` | not LOGGED_IN (session B, first attempt)  | Click "OK" / "Continue Login" / "Reconnect This Session"; set `attempted=true`. |
| `primary`/`primaryoverride` | not LOGGED_IN (session B, second attempt) | Click "Cancel" or "Exit Application"; terminate this session.                   |
| `primary`                   | LOGGED_IN (session A)                     | Click "OK" / "Continue Login" / "Reconnect This Session"; keep this session.    |
| `primaryoverride`           | LOGGED_IN (session A)                     | Click "Cancel" or "Exit Application"; cede to incoming session.                 |

Any value not in {manual, primary, primaryoverride, secondary} is treated as "secondary" with a
warning log.

### 4.4 Trading Login Handoff (`TradingLoginHandoffDialogHandler`)

- Recognised: `JDialog` with title containing "Trading Login Handoff".
- This dialog appears after an existing session conflict is resolved via 2FA, allowing a final
  confirm-or-hand-off decision.
- Behaviour is driven again by `ExistingSessionDetectedAction`:

| Setting value     | Action                                                                               |
| ----------------- | ------------------------------------------------------------------------------------ |
| `primary`         | Click "Disconnect Other Session" to keep this session.                               |
| `primaryoverride` | Call `Utils.exitWithError(INVALID_STATE, ...)` — considered an unexpected condition. |
| `secondary`       | Call `Utils.exitWithError(INVALID_STATE, ...)` — considered an unexpected condition. |
| `manual`          | Do nothing; user chooses.                                                            |
| other/invalid     | Log error only.                                                                      |

### 4.5 Re-login Required (`ReloginDialogHandler`)

- Recognised: `JDialog` with title containing "Re-login is required".
- Action: clicks "Re-login"; on success sets state to `LOGGING_IN`.
- This dialog appears on session A when session B takes over during an existing-session conflict.
- No config.ini setting controls this handler's behaviour.

---

## 5. Configuration Parameters

The following `config.ini` settings influence the login flow. All are read via `Settings.settings()`.

### Credentials

| Key           | Type   | Default | Used by               | Description                               |
| ------------- | ------ | ------- | --------------------- | ----------------------------------------- |
| `IbLoginId`   | String | `""`    | `DefaultLoginManager` | IBAPI username (TWS and non-FIX Gateway). |
| `IbPassword`  | String | `""`    | `DefaultLoginManager` | IBAPI password.                           |
| `FIXLoginId`  | String | `""`    | `DefaultLoginManager` | FIX username (Gateway FIX mode).          |
| `FIXPassword` | String | `""`    | `DefaultLoginManager` | FIX password.                             |

Credentials can alternatively be passed as command-line arguments (args[1]/args[2] for IBAPI, or
args[1..4] for FIX+IBAPI); args take precedence over settings file values.

### Trading mode and session

| Key                     | Type    | Default   | Used by                                                                                | Description                                                                                                                          |
| ----------------------- | ------- | --------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `ReadOnlyLogin`         | Boolean | `false`   | `LoginManager`, `SecurityCodeDialogHandler`, `SecondFactorAuthenticationDialogHandler` | Perform read-only login. Not supported by Gateway.                                                                                   |
| `StoreSettingsOnServer` | Boolean | (not set) | `LoginFrameHandler`                                                                    | Controls "Use/store settings on server" checkbox (TWS only). Setting is applied via deferred DocumentListener on the username field. |

### 2FA

| Key                                             | Type    | Default   | Used by                                                   | Description                                                                                                                  |
| ----------------------------------------------- | ------- | --------- | --------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `Ib2FA`                                         | String  | `""`      | `SecondFactorAuthenticationDialogHandler`                 | TOTP shared secret. When set, IBC generates and submits the TOTP code automatically.                                         |
| `SecondFactorDevice`                            | String  | `""`      | `SecondFactorAuthenticationDialogHandler`                 | Name of the 2FA device to select when multiple devices are enrolled.                                                         |
| `SecondFactorAuthenticationTimeout`             | Integer | `180`     | `LoginManager.secondFactorAuthenticationDialogClosed`     | Seconds since login start within which the 2FA dialog must be dismissed. Determines whether a re-login or exit is scheduled. |
| `SecondFactorAuthenticationExitInterval`        | Integer | `60`      | `LoginManager.secondFactorAuthenticationDialogClosed`     | Seconds to wait after early 2FA dialog close before exiting if LOGGED_IN state is not reached.                               |
| `ReloginAfterSecondFactorAuthenticationTimeout` | Boolean | `false`   | `LoginManager`, `TooManyFailedLoginAttemptsDialogHandler` | When true: re-initiate login after timeout; also enables auto-retry after rate-limit.                                        |
| `ExitAfterSecondFactorAuthenticationTimeout`    | Boolean | (not set) | `LoginManager` (legacy fallback)                          | Legacy key; consulted only when `ReloginAfterSecondFactorAuthenticationTimeout` is not set.                                  |

### Session conflict

| Key                             | Type   | Default    | Used by                                                                    | Description                                                                                           |
| ------------------------------- | ------ | ---------- | -------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `ExistingSessionDetectedAction` | String | `"manual"` | `ExistingSessionDetectedDialogHandler`, `TradingLoginHandoffDialogHandler` | How to handle an existing session. Valid values: `manual`, `primary`, `primaryoverride`, `secondary`. |
