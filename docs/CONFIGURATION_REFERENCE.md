# IBC Configuration Reference

This document is a complete reference for all IBC configuration settings, drawn directly from `resources/config.ini`, `resources/twsstart.sh`, and `resources/gatewaystart.sh`.

---

## 1. Two Configuration Sources and How They Interact

IBC is configured through two distinct layers that work together at startup.

### Layer 1: Shell variables in `twsstart.sh` / `gatewaystart.sh`

These shell scripts are the **entry point** for launching TWS or the Gateway. They control the runtime environment: which version of TWS/Gateway to launch, where files live on disk, and how to recover from a 2FA timeout. The variables are exported as environment variables and consumed by the IBC launch scripts (`displaybannerandlaunch.sh` / `ibcstart.sh`).

Shell variables govern infrastructure-level concerns — paths, versions, and process behavior — and are resolved before the Java process starts.

### Layer 2: `config.ini` settings

Once the Java process is running, IBC reads `config.ini` (whose location is given by the shell variable `IBC_INI`). This file governs **application-level behavior**: authentication credentials, TWS/Gateway UI automation, API precautions, command server, and diagnostics.

### Interaction rules

| Concern               | Shell variable              | config.ini setting                              | Priority                                                                   |
| --------------------- | --------------------------- | ----------------------------------------------- | -------------------------------------------------------------------------- |
| Trading mode          | `TRADING_MODE`              | `TradingMode`                                   | Shell variable wins if set                                                 |
| 2FA timeout action    | `TWOFA_TIMEOUT_ACTION`      | `ReloginAfterSecondFactorAuthenticationTimeout` | Must be consistent (see Section 5)                                         |
| Settings storage path | `TWS_SETTINGS_PATH`         | `IbDir` (deprecated)                            | `IbDir` wins if both set, but they must match for auto-restart             |
| Credentials           | `TWSUSERID` / `TWSPASSWORD` | `IbLoginId` / `IbPassword`                      | Shell variables win; config.ini values are ignored when shell vars are set |
| FIX credentials       | `FIXUSERID` / `FIXPASSWORD` | `FIXLoginId` / `FIXPassword`                    | Shell variables win                                                        |

---

## 2. Shell Variables Reference

These variables are defined at the top of `twsstart.sh` and `gatewaystart.sh`. All are exported to the environment before the IBC Java process is launched.

| Variable               | Default            | Applies To   | Description                                                                                                                                                                                                                                            |
| ---------------------- | ------------------ | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `TWS_MAJOR_VRSN`       | `1019`             | TWS, Gateway | Major version number of the TWS/Gateway installation to run. Derived from the build string (e.g. "Build 10.19.1f" → `1019`). Do not include minor version components.                                                                                  |
| `IBC_INI`              | `~/ibc/config.ini` | TWS, Gateway | Full path to the IBC `config.ini` file. Should be in a protected, encrypted location accessible only to the current user.                                                                                                                              |
| `TRADING_MODE`         | _(empty)_          | TWS, Gateway | `live` or `paper` (case-insensitive). Overrides `TradingMode` in `config.ini`. If empty, falls back to `TradingMode`; if that is also unset, defaults to `live`.                                                                                       |
| `TWOFA_TIMEOUT_ACTION` | `exit`             | TWS, Gateway | Action when 2FA acknowledgement does not lead to a successful login: `exit` terminates IBC; `restart` restarts the authentication sequence. If set to `restart`, `ReloginAfterSecondFactorAuthenticationTimeout=yes` must also be set in `config.ini`. |
| `IBC_PATH`             | `/opt/ibc`         | TWS, Gateway | Directory containing IBC scripts and JAR files.                                                                                                                                                                                                        |
| `TWS_PATH`             | `~/Jts`            | TWS, Gateway | Directory where TWS/Gateway is installed. The IB installer always uses `~/Jts`; changing this is rarely necessary.                                                                                                                                     |
| `TWS_SETTINGS_PATH`    | _(empty)_          | TWS, Gateway | Directory where TWS/Gateway stores its settings. Defaults to `TWS_PATH` when empty. Must match `IbDir` (or `IbDir` must be unset) for auto-restart to work. Preferred over the deprecated `IbDir` config.ini setting.                                  |
| `LOG_PATH`             | `~/ibc/logs`       | TWS, Gateway | Directory for IBC diagnostic log files. If empty, logs go to the terminal. If the variable is removed entirely, no logs are captured.                                                                                                                  |
| `TWSUSERID`            | _(empty)_          | TWS, Gateway | IBKR username. Overrides `IbLoginId` in `config.ini`. Not recommended: this file is not in a protected location.                                                                                                                                       |
| `TWSPASSWORD`          | _(empty)_          | TWS, Gateway | IBKR password. Overrides `IbPassword` in `config.ini`. Not recommended.                                                                                                                                                                                |
| `FIXUSERID`            | _(empty)_          | Gateway only | FIX CTCI username. Overrides `FIXLoginId` in `config.ini`. Only relevant when `FIX=yes`.                                                                                                                                                               |
| `FIXPASSWORD`          | _(empty)_          | Gateway only | FIX CTCI password. Overrides `FIXPassword` in `config.ini`. Only relevant when `FIX=yes`.                                                                                                                                                              |
| `JAVA_PATH`            | _(empty)_          | TWS, Gateway | Path to a Java installation. The TWS/Gateway installer bundles its own JRE; this should be left empty unless there is a specific reason to override it.                                                                                                |
| `HIDE`                 | _(empty)_          | TWS, Gateway | If `YES` or `TRUE` (case-insensitive), the xterm diagnostic window is iconified on startup. Has no effect when the `-inline` argument is passed to the start script.                                                                                   |

---

## 3. `config.ini` Settings Reference

Settings are read as a Java `Properties` file. Lines beginning with `#` are comments. An empty value (`Key=`) is treated identically to the key being absent — IBC always falls back to the documented default. String values are trimmed of leading and trailing whitespace.

Boolean settings accept `yes`, `true`, `no`, `false` (case-insensitive).

### 3.1 IBC Startup

| Setting | Default | Accepted Values | Description                                                                                                                                                                                |
| ------- | ------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `FIX`   | `no`    | `yes`, `no`     | Set to `yes` to run the FIX CTCI Gateway instead of the standard IB API Gateway. The IB API Gateway and FIX CTCI Gateway share the same binary; this setting selects which mode is active. |

### 3.2 Authentication

| Setting       | Default   | Accepted Values | Description                                                                                                                                |
| ------------- | --------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `IbLoginId`   | _(empty)_ | Any string      | IBKR username for TWS/Gateway login. Ignored if `TWSUSERID` is set in the start script. May be left empty to have the login dialog appear. |
| `IbPassword`  | _(empty)_ | Any string      | IBKR password. Ignored if `TWSPASSWORD` is set in the start script. Redacted in console log output.                                        |
| `Ib2FA`       | _(empty)_ | Any string      | IBKR two-factor authentication code, when applicable.                                                                                      |
| `FIXLoginId`  | _(empty)_ | Any string      | FIX CTCI username. Only used when `FIX=yes`. Ignored if `FIXUSERID` is set in the start script. Redacted in console log output.            |
| `FIXPassword` | _(empty)_ | Any string      | FIX CTCI password. Only used when `FIX=yes`. Ignored if `FIXPASSWORD` is set in the start script. Redacted in console log output.          |

### 3.3 Second Factor Authentication (2FA)

See also Section 5 for a detailed explanation of 2FA interactions.

| Setting                                         | Default   | Accepted Values   | Description                                                                                                                                                                                                                                                 |
| ----------------------------------------------- | --------- | ----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SecondFactorDevice`                            | _(empty)_ | Any string        | When multiple 2FA devices are enrolled, IBC selects the device whose name exactly matches this string. If empty, the user must manually select a device from the list.                                                                                      |
| `ReloginAfterSecondFactorAuthenticationTimeout` | `no`      | `yes`, `no`       | Whether IBC automatically restarts the login sequence when the 2FA window expires. Supersedes the deprecated `ExitAfterSecondFactorAuthenticationTimeout`. Requires IBC v3.14.0 or later.                                                                   |
| `SecondFactorAuthenticationTimeout`             | `180`     | Integer (seconds) | The 2FA timeout (in seconds) imposed by IBKR. Should not be changed unless IBKR has modified their timeout.                                                                                                                                                 |
| `SecondFactorAuthenticationExitInterval`        | `60`      | Integer (seconds) | How long IBC waits (in seconds) after the user acknowledges the IBKR Mobile 2FA alert for login to complete before terminating. Only relevant when `ReloginAfterSecondFactorAuthenticationTimeout=yes` or `ExitAfterSecondFactorAuthenticationTimeout=yes`. |
| `ExitAfterSecondFactorAuthenticationTimeout`    | `no`      | `yes`, `no`       | **DEPRECATED** — will be removed in a future release. For IBC v3.14.0+, use `ReloginAfterSecondFactorAuthenticationTimeout` instead. Controls the same behavior for older IBC versions.                                                                     |

### 3.4 Trading Mode

| Setting                            | Default | Accepted Values | Description                                                                                                                                                        |
| ---------------------------------- | ------- | --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `TradingMode`                      | `live`  | `live`, `paper` | Selects live or paper-trading account. Overridden by `TRADING_MODE` shell variable if that is set. When `paper`, either live or paper credentials may be supplied. |
| `AcceptNonBrokerageAccountWarning` | `no`    | `yes`, `no`     | Automatically accepts the paper-trading account acknowledgement dialog. When `no`, the dialog waits for the user. Until accepted, API connections are blocked.     |

### 3.5 TWS Startup

| Setting                         | Default                               | Accepted Values                                     | Description                                                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------------- | ------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `IbDir`                         | _(empty — current working directory)_ | File system path                                    | **DEPRECATED** — use `TWS_SETTINGS_PATH` in the start script instead. Directory where TWS stores its settings. On Windows, path separators must be doubled (`C:\\path\\to\\dir`). If both `IbDir` and `TWS_SETTINGS_PATH` are set to different values, auto-restart will not work.                                                                                                                |
| `StoreSettingsOnServer`         | _(unchanged)_                         | `yes`, `no`, _(empty)_                              | TWS only (ignored by Gateway). Whether TWS settings are also stored on IB's servers, enabling the same configuration across multiple machines. If empty, TWS uses the same choice as the previous session.                                                                                                                                                                                        |
| `MinimizeMainWindow`            | `no`                                  | `yes`, `no`                                         | Minimizes the TWS main window immediately after startup.                                                                                                                                                                                                                                                                                                                                          |
| `ExistingSessionDetectedAction` | `manual`                              | `primary`, `primaryoverride`, `secondary`, `manual` | Instructs TWS how to respond when an existing session for the same account is detected at login. `primary`: existing session continues, new session terminates, and existing session auto-reconnects if forcibly ended. `primaryoverride`: new session terminates existing if both are `primaryoverride`. `secondary`: new session always yields to any existing session. `manual`: user decides. |
| `LoginDialogDisplayTimeout`     | `60`                                  | Integer (seconds)                                   | How long IBC waits for the TWS login dialog to appear before automatically restarting TWS. Under normal conditions the dialog appears in under 20 seconds, but a conservative value is recommended.                                                                                                                                                                                               |
| `OverrideTwsApiPort`            | _(empty — no change)_                 | Integer                                             | If set, IBC changes the TWS API "Socket port" to this value shortly after startup. For FIX Gateway, this value is stored in `jts.ini`. Intended for specialized or dynamic-port environments. Most users should not set this.                                                                                                                                                                     |
| `OverrideTwsMasterClientID`     | _(empty — no change)_                 | Integer                                             | If set, IBC changes the TWS API "Master Client ID" to this value shortly after startup. Intended for dynamic runtime environments. Most users should not set this.                                                                                                                                                                                                                                |
| `ReadOnlyLogin`                 | `no`                                  | `yes`, `no`                                         | When `yes` and the account has IB security enrollment, IBC skips 2FA and logs in read-only (no order placement or management). Ignored for non-enrolled accounts.                                                                                                                                                                                                                                 |
| `ReadOnlyApi`                   | _(empty — no change)_                 | `yes`, `no`, _(empty)_                              | Controls the TWS/Gateway API "Read-Only" checkbox. `yes` prevents API clients from submitting, modifying, or cancelling orders. Intended for automated/Docker deployments; interactive users should configure this via Global Configuration.                                                                                                                                                      |
| `ShowAllTrades`                 | `no`                                  | `yes`, `no`                                         | TWS only. When `yes`, IBC opens the Trades log after login and selects all days so all trades are shown. For TWS 955 and later the setting cannot be auto-applied because the daily checkboxes were replaced by a non-programmable control; in that case IBC logs a warning and the user must configure the Trade History window manually.                                                        |

### 3.6 API Precautions

All settings in this section correspond to checkboxes in the API section of the TWS/Gateway Global Configuration dialog. Accepted values for all of them: `yes` (check), `no` (uncheck), empty (leave unchanged). Intended for automated deployments without persistent user settings.

| Setting                                   | Default       | Description                                                              |
| ----------------------------------------- | ------------- | ------------------------------------------------------------------------ |
| `BypassOrderPrecautions`                  | _(unchanged)_ | Bypasses the standard order precaution checks.                           |
| `BypassBondWarning`                       | _(unchanged)_ | Bypasses the warning displayed when placing bond orders.                 |
| `BypassNegativeYieldToWorstConfirmation`  | _(unchanged)_ | Bypasses the confirmation dialog for bonds with negative yield-to-worst. |
| `BypassCalledBondWarning`                 | _(unchanged)_ | Bypasses the warning for called bond orders.                             |
| `BypassSameActionPairTradeWarning`        | _(unchanged)_ | Bypasses the warning when both legs of a pair trade are the same action. |
| `BypassPriceBasedVolatilityRiskWarning`   | _(unchanged)_ | Bypasses the price-based volatility risk warning.                        |
| `BypassUSStocksMarketDataInSharesWarning` | _(unchanged)_ | Bypasses the US stocks market data display notification.                 |
| `BypassRedirectOrderWarning`              | _(unchanged)_ | Bypasses the redirect order warning.                                     |
| `BypassNoOverfillProtectionPrecaution`    | _(unchanged)_ | Bypasses the no-overfill-protection precaution warning.                  |

### 3.7 Market Data

| Setting                                         | Default       | Accepted Values             | Description                                                                                                                                                                                                                                                             |
| ----------------------------------------------- | ------------- | --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AcceptBidAskLastSizeDisplayUpdateNotification` | _(ignore)_    | `accept`, `defer`, `ignore` | Handles the dialog shown after login about US stocks bid/ask/last sizes in shares vs lots. `accept`: dismisses permanently (also sets the dual-mode API checkbox). `defer`: dismisses this time only, dialog reappears next login. `ignore`: user must handle manually. |
| `SendMarketDataInLotsForUSstocks`               | _(unchanged)_ | `yes`, `no`, _(empty)_      | Controls the API checkbox "Send market data in lots for US stocks for dual-mode API clients". Useful to undo the side-effect of `AcceptBidAskLastSizeDisplayUpdateNotification=accept`.                                                                                 |

### 3.8 Trading

| Setting                       | Default  | Accepted Values                | Description                                                                                                                                                                    |
| ----------------------------- | -------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `AllowBlindTrading`           | `no`     | `yes`, `no`                    | When `yes`, automatically dismisses the warning dialog that appears when placing orders for contracts with no market data subscription, accepting the associated risk.         |
| `ConfirmCryptoCurrencyOrders` | `manual` | `transmit`, `cancel`, `manual` | Handles the confirmation dialog for cryptocurrency orders. `transmit`: places the order automatically. `cancel`: cancels the order automatically. `manual`: user must confirm. |

### 3.9 API Trusted Connections

| Setting                  | Default   | Accepted Values              | Description                                                                                                                                                                                                                            |
| ------------------------ | --------- | ---------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TrustedTwsApiClientIPs` | _(empty)_ | Comma-separated IP addresses | **Gateway only, and only when `FIX=yes`.** Ignored in all other cases. API clients from these IP addresses can connect without triggering an "Incoming connection" popup. `127.0.0.1` is always permitted and must not be listed here. |

### 3.10 Order ID Management

| Setting                | Default         | Accepted Values                                                       | Description                                                                                                                                                                                                                                                                                                                                     |
| ---------------------- | --------------- | --------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ResetOrderIdsAtStart` | `no`            | `yes`, `true`, `no`, `false`                                          | Resets the API order ID sequence at startup so that the next `NextValidId` callback returns `1`. Does not affect clients with outstanding (incomplete) orders.                                                                                                                                                                                  |
| `ConfirmOrderIdReset`  | `ignore/ignore` | Two values separated by `/`: each is `confirm`, `reject`, or `ignore` | Governs IBC's response to the order ID reset confirmation dialog. The first value applies when the reset was triggered by `ResetOrderIdsAtStart=yes`; the second applies when the user clicks the "Reset API order ID sequence" button in the API configuration. Gateway never shows this dialog; this setting is ignored for Gateway sessions. |

### 3.11 Auto-Logoff, Auto-Restart and Cold Restart

| Setting           | Default       | Accepted Values              | Description                                                                                                                                                                                                                         |
| ----------------- | ------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `AutoLogoffTime`  | _(unchanged)_ | `HH:MM AM/PM`, _(empty)_     | Time at which TWS/Gateway shuts down tidily without restarting. If a value is provided for `AutoRestartTime`, this setting is cleared. Example: `08:00 AM`.                                                                         |
| `AutoRestartTime` | _(unchanged)_ | `HH:MM AM/PM`, _(empty)_     | Time at which TWS/Gateway shuts down and then automatically restarts without requiring re-authentication. If both `AutoLogoffTime` and `AutoRestartTime` are set, only `AutoRestartTime` takes effect.                              |
| `ColdRestartTime` | _(empty)_     | `HH:MM` (24-hour local time) | Time on Sundays at which IBC performs a full cold restart (closes TWS/Gateway and starts a new instance requiring full login including 2FA). Must be set to a time that is after 01:00 US/Eastern to comply with IBKR requirements. |

### 3.12 Tidy Closedown

| Setting       | Default   | Accepted Values              | Description                                                                                                                                                                                                                                                                                     |
| ------------- | --------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ClosedownAt` | _(empty)_ | `HH:MM` or `DayOfWeek HH:MM` | Time (and optionally day of week) at which IBC closes TWS/Gateway tidily with no restart. More flexible than `AutoLogoffTime` because it can target a specific day. Day name must match the system locale; non-Latin-1 characters must be Unicode-escaped. Example: `ClosedownAt=Friday 22:00`. |

### 3.13 Session Management

| Setting             | Default   | Accepted Values                  | Description                                                                                                                                                                                               |
| ------------------- | --------- | -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SaveTwsSettingsAt` | _(empty)_ | Time list or interval expression | Schedule for automatic saving of TWS settings. Supports specific times (`08:00 12:30 17:30`), or an interval (`Every 30 mins`, `Every 1 hours 08:00 00:00`). If empty, no scheduled saves are configured. |

### 3.14 Indian TWS Versions

| Setting                        | Default | Accepted Values | Description                                                                                                                                                                                                                  |
| ------------------------------ | ------- | --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DismissPasswordExpiryWarning` | `no`    | `yes`, `no`     | Automatically dismisses the password expiry notification shown by Indian versions of TWS. Setting this to `yes` suppresses the warning; ensure an independent password-rotation process is in place to avoid login failures. |
| `DismissNSEComplianceNotice`   | `yes`   | `yes`, `no`     | Automatically dismisses the NSE Compliance notice shown by Indian versions of TWS.                                                                                                                                           |

### 3.15 Command Server

| Setting                | Default                    | Accepted Values                  | Description                                                                                                                                                                                                          |
| ---------------------- | -------------------------- | -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CommandServerPort`    | `0`                        | Integer, `0`                     | TCP port on which IBC listens for text commands (e.g. `STOP`, `RESTART`). `0` disables the command server entirely. The conventional port is `7462`. Must not conflict with the TWS API port or other IBC instances. |
| `ControlFrom`          | _(empty — localhost only)_ | Comma-separated IPs or hostnames | IP addresses or hostnames permitted to send commands to the command server. The local host is always permitted regardless of this setting.                                                                           |
| `BindAddress`          | _(all interfaces)_         | IP address                       | The local IP address on which the command server listens. Useful on multi-homed hosts to restrict which interface accepts command connections.                                                                       |
| `CommandPrompt`        | _(empty — no prompt)_      | Any string                       | String emitted by the command server when a connection opens and after each command completes. Useful for interactive use with tools such as telnet. Example: `CommandPrompt=>`.                                     |
| `SuppressInfoMessages` | `yes`                      | `yes`, `no`                      | Whether intermediate progress information is suppressed in command server responses.                                                                                                                                 |

### 3.16 Diagnostics

| Setting                          | Default   | Accepted Values                                                                        | Description                                                                                                                                                                                                                                                                                                      |
| -------------------------------- | --------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `LogStructureScope`              | `known`   | `known`, `unknown`, `untitled`, `all`                                                  | Which TWS windows are eligible for structure logging. `known`: only windows IBC recognizes and monitors. `unknown`: only windows IBC does not recognize (covers most TWS windows). `untitled`: only unrecognized windows with no title (typically message boxes). `all`: every window.                           |
| `LogStructureWhen`               | `never`   | `open` (alias: `yes`, `true`), `openclose`, `activate`, `never` (alias: `no`, `false`) | When eligible windows have their structure logged. `open`: once, the first time the window is encountered. `openclose`: every time the window opens or closes. `activate`: every time the window becomes active. `never`: disabled. Has a small performance impact and can significantly increase log file size. |
| `LogComponents`                  | _(empty)_ | Same as `LogStructureWhen`                                                             | **DEPRECATED** — will be removed in a future release. If set to any value, overrides both `LogStructureScope` (forced to `all`) and `LogStructureWhen` (set to this value). When set, `LogStructureScope` and `LogStructureWhen` are ignored.                                                                    |
| `IncludeStackTraceForExceptions` | `no`      | `yes`, `true`, `no`, `false`                                                           | Whether Java exception stack traces are written to the IBC log file. Useful for diagnosing unexpected behavior in IBC or TWS code.                                                                                                                                                                               |

---

## 4. Second Factor Authentication (2FA) — Detailed Interaction

The following settings work together to control what happens when IBKR's 2FA process does not complete within the allowed time.

### Settings involved

| Setting                                         | Layer          | Role                                                                                                                                                                                                                                                                            |
| ----------------------------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TWOFA_TIMEOUT_ACTION`                          | Shell variable | Controls the **shell-level** response: whether the start script restarts IBC (`restart`) or simply exits (`exit`) after IBC terminates following a 2FA timeout.                                                                                                                 |
| `ReloginAfterSecondFactorAuthenticationTimeout` | `config.ini`   | Controls the **IBC-level** response: whether IBC itself attempts to restart the login sequence internally (requires IBC v3.14.0+).                                                                                                                                              |
| `SecondFactorAuthenticationTimeout`             | `config.ini`   | The IBKR-imposed time window (seconds) during which the user must complete 2FA before IBC considers the attempt failed. Default: 180 seconds.                                                                                                                                   |
| `SecondFactorAuthenticationExitInterval`        | `config.ini`   | After the user acknowledges the IBKR Mobile alert, how long IBC waits (seconds) for login to complete before terminating. Default: 60 seconds. Only active when either `ReloginAfterSecondFactorAuthenticationTimeout=yes` or `ExitAfterSecondFactorAuthenticationTimeout=yes`. |
| `SecondFactorDevice`                            | `config.ini`   | When multiple 2FA devices are registered, IBC selects the device whose name exactly matches this value.                                                                                                                                                                         |
| `ExitAfterSecondFactorAuthenticationTimeout`    | `config.ini`   | **DEPRECATED.** Equivalent of `ReloginAfterSecondFactorAuthenticationTimeout` for IBC versions earlier than 3.14.0. IBC reads this as a fallback only if `ReloginAfterSecondFactorAuthenticationTimeout` has no value.                                                          |

### Interaction flow

```
IBKR 2FA window opens
        |
        v
User has SecondFactorAuthenticationTimeout seconds (default 180) to respond
        |
        +---> User responds in time --> Login proceeds normally
        |
        +---> Timeout expires
                    |
                    v
        ReloginAfterSecondFactorAuthenticationTimeout=yes?
                    |
            YES     |     NO
             v             v
        IBC restarts    IBC exits
        login sequence      |
             |         TWOFA_TIMEOUT_ACTION=restart?
             |              |
             |         YES  |  NO
             |          v        v
             |     start script  start script exits
             |     relaunches IBC (use crontab or
             |                    other external
             |                    mechanism)
             v
  User acknowledges IBKR Mobile alert
             |
             v
  IBC waits SecondFactorAuthenticationExitInterval seconds (default 60)
             |
        Login completes?
             |
        YES  |  NO
          v       v
      Session  IBC terminates
      active
```

### Recommended configurations

**Automated server / Docker (no user present):**

- `ReloginAfterSecondFactorAuthenticationTimeout=yes` in `config.ini`
- `TWOFA_TIMEOUT_ACTION=restart` in the start script
- Use `SecondFactorDevice` to pre-select the 2FA device

**Automated with external restart (crontab):**

- `ReloginAfterSecondFactorAuthenticationTimeout=no` in `config.ini`
- `TWOFA_TIMEOUT_ACTION=exit` in the start script
- Crontab handles periodic restart

**Interactive use:**

- Leave both settings at their defaults (`no` / `exit`)
- Handle 2FA manually via the IBKR Mobile app within the 180-second window

---

## 5. Diagnostic Settings — How to Use Them

The diagnostic settings in Section 3.16 allow inspection of TWS window structures, which is valuable when IBC behaves unexpectedly or when adding support for new TWS dialogs.

### `LogStructureScope` and `LogStructureWhen`

These two settings work together:

- `LogStructureScope` defines **which** windows are eligible.
- `LogStructureWhen` defines **when** those windows are logged.

Both must be non-default to produce useful output.

**Typical diagnostic workflows:**

| Goal                                                       | `LogStructureScope` | `LogStructureWhen` |
| ---------------------------------------------------------- | ------------------- | ------------------ |
| Investigate a specific known dialog (e.g. login)           | `known`             | `open`             |
| Investigate an unrecognized popup that IBC is not handling | `unknown`           | `open`             |
| Investigate a small unrecognized message box               | `untitled`          | `open`             |
| Capture complete window lifecycle for all windows          | `all`               | `openclose`        |
| Disable all structure logging (recommended for production) | `known`             | `never`            |

### `LogComponents` (deprecated)

When `LogComponents` is set to any value, it forces `LogStructureScope=all` and sets `LogStructureWhen` to the same value as `LogComponents`. This overrides whatever values are set for `LogStructureScope` and `LogStructureWhen`. In effect, `LogComponents=open` is equivalent to `LogStructureScope=all` plus `LogStructureWhen=open`.

Do not set `LogComponents` in new configurations; use `LogStructureScope` and `LogStructureWhen` directly.

### `IncludeStackTraceForExceptions`

Set to `yes` when investigating Java exceptions. Stack traces are written to the IBC log file and help identify which component threw an exception and the full call chain. This has negligible performance impact but increases log verbosity.

---

## 6. Special Values and Edge Cases

| Setting                                         | Special Value      | Behavior                                                                                                                                                                                           |
| ----------------------------------------------- | ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `CommandServerPort`                             | `0`                | Disables the command server entirely. IBC will not listen on any port for commands. This is the default and the correct value for deployments that do not need remote command access.              |
| `IbDir`                                         | _(empty)_          | TWS uses the current working directory at the time IBC is launched as the settings directory. This is unpredictable in scripted environments; use `TWS_SETTINGS_PATH` in the start script instead. |
| `TWS_SETTINGS_PATH`                             | _(empty)_          | Gateway/TWS uses the directory specified by `TWS_PATH`.                                                                                                                                            |
| `IbDir` vs `TWS_SETTINGS_PATH`                  | Different values   | Auto-restart will fail silently. The two must be identical, or `IbDir` must be unset.                                                                                                              |
| `TRADING_MODE` (shell)                          | _(empty)_          | Falls through to `TradingMode` in `config.ini`. If that is also unset, defaults to `live`.                                                                                                         |
| `StoreSettingsOnServer`                         | _(empty)_          | TWS preserves the same choice as the previous session (no change). Not applicable to Gateway.                                                                                                      |
| `ReadOnlyApi`                                   | _(empty)_          | The existing TWS/Gateway API read-only checkbox state is preserved unchanged.                                                                                                                      |
| `OverrideTwsApiPort`                            | _(empty)_          | The TWS API socket port is not modified.                                                                                                                                                           |
| `OverrideTwsMasterClientID`                     | _(empty)_          | The TWS Master Client ID is not modified.                                                                                                                                                          |
| `ExistingSessionDetectedAction`                 | `primaryoverride`  | A `primaryoverride` session can be overridden by a new `primary` session, but a `primary` session cannot be overridden by any session.                                                             |
| `AutoLogoffTime` + `AutoRestartTime` both set   | Both non-empty     | Only `AutoRestartTime` is applied; `AutoLogoffTime` is cleared.                                                                                                                                    |
| `AcceptBidAskLastSizeDisplayUpdateNotification` | `accept`           | Also sets the "Send market data in lots for US stocks for dual-mode API clients" API checkbox. Use `SendMarketDataInLotsForUSstocks=no` immediately after if this side-effect is not desired.      |
| `ConfirmOrderIdReset`                           | `ignore/ignore`    | IBC takes no action on the order ID reset dialog in either scenario; the user must handle it manually.                                                                                             |
| `LOG_PATH`                                      | _(empty string)_   | Log output goes to the terminal window (stdout). If the variable is completely removed from the script, no logging occurs at all.                                                                  |
| `TrustedTwsApiClientIPs`                        | `127.0.0.1` listed | Unnecessary and potentially ignored — `127.0.0.1` is always a permitted client address regardless of this setting.                                                                                 |
| `SecondFactorAuthenticationTimeout`             | `180`              | Reflects IBKR's server-imposed window. Should only be changed if IBKR modifies their own timeout, which would be communicated in IBC release notes.                                                |
| `ClosedownAt` day name                          | Non-Latin-1 locale | The day name must be Unicode-escaped. Example for Simplified Chinese Saturday: `星期六 12:00`.                                                                                                     |
