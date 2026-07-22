# IBC Startup Sequence — Technical Reference

This document describes the complete startup sequence for IBC when launching TWS or Gateway,
based on a direct reading of the source code. It covers every synchronization point, every
timeout, and the role of each subsystem involved.

---

## Table of Contents

1. [TWS Startup Sequence](#1-tws-startup-sequence)
2. [Gateway Startup Sequence](#2-gateway-startup-sequence)
3. [TWS vs Gateway — Key Differences](#3-tws-vs-gateway--key-differences)
4. [Synchronization Points](#4-synchronization-points)
5. [JtsIniManager — Role and Timing](#5-jtsinimanager--role-and-timing)
6. [TwsSettingsSaver — Automatic Save Scheduling](#6-twssettingssaver--automatic-save-scheduling)
7. [Timeout Reference](#7-timeout-reference)
8. [Complete Sequence Diagram](#8-complete-sequence-diagram)

---

## 1. TWS Startup Sequence

### Entry Point: `IbcTws.main()`

**Class:** `IbcTws`  
**Method:** `public static void main(final String[] args)`

```
IbcTws.main(args)
  ├── Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler)
  ├── checkArguments(args)          // validates 0..6 args
  ├── setupDefaultEnvironment(args, isGateway=false)
  └── load()
```

### Step 1 — Environment Initialization

**Method:** `IbcTws.setupDefaultEnvironment(args, false)`

The following singletons are initialized in this exact order:

| Call                                                                 | Class initialized    | Purpose                                            |
| -------------------------------------------------------------------- | -------------------- | -------------------------------------------------- |
| `SessionManager.initialise(false)`                                   | `SessionManager`     | Records that this is a TWS session (not Gateway)   |
| `Settings.initialise(new DefaultSettings(args))`                     | `Settings`           | Loads the `.ini` configuration file from `args[0]` |
| `LoginManager.initialise(new DefaultLoginManager(args))`             | `LoginManager`       | Loads credentials from args or config              |
| `MainWindowManager.initialise(new DefaultMainWindowManager())`       | `MainWindowManager`  | Sets up main window tracking                       |
| `TradingModeManager.initialise(new DefaultTradingModeManager(args))` | `TradingModeManager` | Determines LIVE vs PAPER trading                   |

### Step 2 — IbcTws.load(): Pre-launch Tasks (all on the main thread)

**Method:** `IbcTws.load()`

These steps execute sequentially before TWS is launched:

```
load()
  ├── printVersionInfo()              // logs IBC version to console
  ├── printProperties()              // logs Java system properties (passwords masked)
  ├── [managers].logDiagnosticMessage()  // logs config summary for each manager
  ├── startCommandServer()           // launches CommandServer on a background thread
  ├── startShutdownTimerIfRequired() // schedules StopTask if ClosedownAt or ColdRestartTime set
  ├── createToolkitListener()        // registers TwsListener on the AWT event queue
  ├── startSavingTwsSettingsAutomatically()  // schedules periodic File > Save Settings
  └── startTwsOrGateway()            // launches TWS (blocking)
```

### Step 3 — AWT Event Listener Registration

**Method:** `IbcTws.createToolkitListener()`

A `TwsListener` is registered as an `AWTEventListener` on the system `Toolkit` for
`AWTEvent.WINDOW_EVENT_MASK`. This listener intercepts all window lifecycle events
(open, close, activate, etc.) for every window that TWS creates, and dispatches them
to the registered `WindowHandler` list.

The full set of window handlers registered at this point (in registration order):

```
AcceptIncomingConnectionDialogHandler
BlindTradingWarningDialogHandler
LoginFrameHandler
GatewayLoginFrameHandler
MainWindowFrameHandler                  <-- detects TWS main window
GatewayMainWindowFrameHandler           <-- detects Gateway main window
NewerVersionDialogHandler
NewerVersionFrameHandler
NotCurrentlyAvailableDialogHandler
TipOfTheDayDialogHandler
NSEComplianceFrameHandler
PasswordExpiryWarningFrameHandler
GlobalConfigurationDialogHandler
TradesFrameHandler
ExistingSessionDetectedDialogHandler
ApiChangeConfirmationDialogHandler
SplashFrameHandler                      <-- detects Gateway splash close
SecondFactorAuthenticationDialogHandler
SecurityCodeDialogHandler
ReloginDialogHandler
NonBrokerageAccountDialogHandler
ExitConfirmationDialogHandler
TradingLoginHandoffDialogHandler
LoginFailedDialogHandler
TooManyFailedLoginAttemptsDialogHandler
ShutdownProgressDialogHandler
BidAskLastSizeDisplayUpdateDialogHandler
LoginErrorDialogHandler
CryptoOrderConfirmationDialogHandler
RestartConfirmationDialogHandler
ResetOrderIdConfirmationDialogHandler
ReconnectDataOrAccountConfirmationDialogHandler
GatewayDialogHandler
```

### Step 4 — Session Start and jts.ini Preparation

**Method:** `IbcTws.startTwsOrGateway()`

```
startTwsOrGateway()
  ├── SessionManager.startSession()      // arms login-dialog timeout timer
  ├── JtsIniManager.initialise(path)     // read/write jts.ini before TWS launches
  └── startTws()                         // calls jclient.LoginFrame.main(twsArgs)  [BLOCKING]
```

`SessionManager.startSession()` sets `_isSessionStarted = true`, reads `FIX=` from settings,
checks for the `-Drestart` JVM property, and schedules the `LoginDialogDisplayTimeout` timer
(default 60 s) unless this is a restart.

### Step 5 — TWS Process Launch

**Method:** `IbcTws.startTws()`

```java
jclient.LoginFrame.main(new String[]{ getTWSSettingsDirectory() });
```

If `ShowAllTrades=yes`, `Utils.showTradesLogWindow()` is called before this to pre-configure
the trades log. The call to `jclient.LoginFrame.main()` **blocks the main thread** for the
entire lifetime of the TWS session. IBC's remaining work is driven by the AWT event listener
from this point forward.

### Step 6 — Post-launch Configuration Tasks (asynchronous)

After calling `startTws()` (but immediately below it in the source — they execute
when `startTws()` returns, i.e., after session end), the following configuration
tasks are submitted. In practice they are submitted as part of `startTwsOrGateway()`,
and they run asynchronously via `ConfigurationTask.executeAsync()`. Each blocks on
`SessionManager.awaitReady()` internally (via `ConfigDialogManager.getConfigDialog()`)
before accessing the Global Configuration dialog.

```
configureResetOrderIdsAtStart()        // ResetOrderIdsAtStart setting
configureApiPort()                     // OverrideTwsApiPort setting
configureMasterClientID()              // OverrideTwsMasterClientID setting
configureReadOnlyApi()                 // ReadOnlyApi setting
configureSendMarketDataInLotsForUSstocks()  // SendMarketDataInLotsForUSstocks setting
configureAutoLogoffOrRestart()         // AutoLogoffTime / AutoRestartTime setting
configureApiPrecautions()              // BypassOrderPrecautions and related settings
```

All of these tasks are skipped (with a log message) if `SessionManager.isFIX()` is true.

### Step 7 — TWS Main Window Detection

**Class:** `MainWindowFrameHandler`  
**Trigger:** `WINDOW_OPENED` event on a `JFrame` that contains `File > Lock Application`
in its menu bar.

```java
// MainWindowFrameHandler.recogniseWindow()
SwingUtils.findMenuItemInAnyMenuBar(window, new String[]{"File", "Lock Application"}) != null
```

When recognized:

```
MainWindowFrameHandler.handleWindow()
  └── SessionManager.setMainWindow(window)
        ├── _InitialisationCompleted = true
        ├── initialised.signal()            // unblocks any thread in awaitReady()
        └── MainWindowManager.mainWindowManager().setMainWindow(window)
```

This is the point at which the TWS session is considered "fully operational": the
`_InitialisationCompleted` flag is set and any `ConfigurationTask` threads blocked
in `awaitReady()` are released to proceed.

---

## 2. Gateway Startup Sequence

### Entry Point: `IbcGateway.main()`

**Class:** `IbcGateway`  
**Method:** `public static void main(String[] args)`

```
IbcGateway.main(args)
  ├── Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler)
  ├── IbcTws.checkArguments(args)
  ├── IbcTws.setupDefaultEnvironment(args, isGateway=true)   // <-- isGateway=TRUE
  └── IbcTws.load()                                           // same load() as TWS
```

`IbcGateway` is a thin wrapper. It delegates immediately to `IbcTws` with the single
difference of passing `isGateway=true`.

### Step 1 — Environment Initialization

Identical to TWS except `SessionManager.initialise(true)` records `_isGateway = true`.

### Step 2 — load() Pre-launch Tasks

Identical to TWS (same `IbcTws.load()` method).

### Step 3 — Session Start and jts.ini Preparation

`startTwsOrGateway()` detects `SessionManager.isGateway() == true` and calls `startGateway()`
instead of `startTws()`.

```
startTwsOrGateway()
  ├── SessionManager.startSession()
  ├── JtsIniManager.initialise(path)
  └── startGateway()    // calls ibgateway.GWClient.main(twsArgs)  [BLOCKING]
```

### Step 4 — Gateway Process Launch

**Method:** `IbcTws.startGateway()`

```java
ibgateway.GWClient.main(new String[]{ getTWSSettingsDirectory() });
```

Unlike TWS, Gateway loads its main window **immediately at startup**, long before its menu
items become responsive. The main window appears before authentication completes.

### Step 5 — Gateway Main Window Detection

**Class:** `GatewayMainWindowFrameHandler`  
**Trigger:** `WINDOW_OPENED` event on a `JFrame` containing `Help > About Gateway` or
`Help > About IB Gateway` in its menu bar.

```java
// GatewayMainWindowFrameHandler.recogniseWindow()
SwingUtils.findMenuItemInAnyMenuBar(window, new String[]{"Help", "About Gateway"}) != null ||
SwingUtils.findMenuItemInAnyMenuBar(window, new String[]{"Help", "About IB Gateway"}) != null
```

When recognized:

```
GatewayMainWindowFrameHandler.handleWindow()
  └── SessionManager.setMainWindow(window)
        ├── _InitialisationCompleted = true
        ├── initialised.signal()
        └── MainWindowManager.mainWindowManager().setMainWindow(window)
```

**Important:** For Gateway, `setMainWindow()` is called early — before the menu items
are actually responsive. This is why Gateway has an additional readiness gate.

### Step 6 — Splash Screen as the True Readiness Gate

The Gateway displays a `JFrame` titled `"Starting application..."` (the splash frame)
in front of its main window during the initialization phase. Its window title is
repeatedly changed during initialization; only the final value `"Starting application..."`
is used for recognition.

**Class:** `SplashFrameHandler`  
**Trigger:** `WINDOW_CLOSED` event on a `JFrame` whose title contains `"Starting application..."`.

```
SplashFrameHandler.handleWindow()
  ├── LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN)
  └── SessionManager.setSplashScreenClosed()
        ├── splashScreenClosed = true
        └── if (nonBrokerageAccountDialogClosed) {
              _InitialisationCompleted = true   // may already be true from setMainWindow()
              initialised.signal()
            }
```

The Gateway is **not considered fully operational** until both conditions are true:

- `splashScreenClosed == true`
- `nonBrokerageAccountDialogClosed == true`

Only then does `SessionManager.awaitReady()` unblock.

### Step 7 — NonBrokerageAccountDialog Gate

If a `NonBrokerageAccountDialog` is displayed and dismissed, `SessionManager.setNonBrokerageAccountDialogClosed()`
is called, mirroring the same logic as `setSplashScreenClosed()`:

```
SessionManager.setNonBrokerageAccountDialogClosed()
  ├── nonBrokerageAccountDialogClosed = true
  └── if (splashScreenClosed) {
        _InitialisationCompleted = true
        initialised.signal()
      }
```

At startup, `nonBrokerageAccountDialogClosed` is `false`. If no such dialog appears,
it remains `false` and `setSplashScreenClosed()` will not signal. The dialog handler
for this window must call `setNonBrokerageAccountDialogClosed()` to release the lock.
This ensures IBC never proceeds with Gateway configuration before it is actually ready.

---

## 3. TWS vs Gateway — Key Differences

| Aspect                            | TWS                                             | Gateway                                                                       |
| --------------------------------- | ----------------------------------------------- | ----------------------------------------------------------------------------- |
| Entry class                       | `IbcTws`                                        | `IbcGateway`                                                                  |
| TWS/Gateway binary                | `jclient.LoginFrame.main()`                     | `ibgateway.GWClient.main()`                                                   |
| `isGateway` flag                  | `false`                                         | `true`                                                                        |
| Main window recognition           | `File > Lock Application` menu item             | `Help > About Gateway` or `Help > About IB Gateway`                           |
| Readiness gate                    | `MainWindowFrameHandler` WINDOW_OPENED          | `SplashFrameHandler` WINDOW_CLOSED + `NonBrokerageAccountDialogClosed`        |
| Main window timing                | Appears after login                             | Appears immediately at launch                                                 |
| `_InitialisationCompleted` set by | `setMainWindow()` only                          | `setSplashScreenClosed()` AND `setNonBrokerageAccountDialogClosed()` together |
| `LoginState.LOGGED_IN` set by     | `LoginFrameHandler` after credentials submitted | `SplashFrameHandler` on splash close                                          |
| Read-only login                   | Supported                                       | Not supported (logged as error)                                               |
| `s3store` in jts.ini              | Required for StoreSettingsOnServer              | Not required                                                                  |
| `[IBGateway] ApiOnly` in jts.ini  | Added as `true`                                 | Added as `true`; for FIX mode added as `false`                                |

---

## 4. Synchronization Points

IBC uses three mechanisms to synchronize with TWS/Gateway state:

### 4.1 Login Dialog Display Timeout

**Location:** `SessionManager.startSession()`  
**Mechanism:** `MyScheduledExecutorService.schedule()` on a daemon thread  
**Condition:** `LoginManager.loginManager().getLoginState() == LoginState.LOGGED_OUT`  
**Default timeout:** 60 seconds (configurable via `LoginDialogDisplayTimeout`)

```
SessionManager.startSession()
  └── schedules: after LoginDialogDisplayTimeout seconds,
        if loginState == LOGGED_OUT → exitWithError(LOGIN_DIALOG_DISPLAY_TIMED_OUT)
```

Not armed during restarts (when `-Drestart` JVM property is set).

### 4.2 `SessionManager.awaitReady()` — Configuration Tasks Gate

**Location:** `SessionManager.awaitReady()`  
**Mechanism:** `ReentrantLock` + `Condition.await()` (indefinite wait)  
**Unblocked by:**

- TWS: `SessionManager.setMainWindow()` when `MainWindowFrameHandler` fires
- Gateway: `SessionManager.setSplashScreenClosed()` when both `splashScreenClosed`
  and `nonBrokerageAccountDialogClosed` are `true`

Any `ConfigurationTask` (API port, master client ID, read-only API, etc.) calls
`ConfigDialogManager.getConfigDialog()` which internally calls `SessionManager.awaitReady()`.
This ensures that no Global Configuration dialog interaction is attempted before the
application is ready to handle menu events.

### 4.3 Second Factor Authentication Gate

**Location:** `LoginManager.secondFactorAuthenticationDialogClosed()`  
**Mechanism:** Elapsed-time check against `LoginStartTime` (set at 2FA initiation)  
**Timeout:** `SecondFactorAuthenticationTimeout` (default 180 seconds)  
**Exit interval:** `SecondFactorAuthenticationExitInterval` (default 60 seconds)

When the 2FA dialog closes:

- If elapsed time < `SecondFactorAuthenticationTimeout`: authentication was completed by
  the user; if `ReloginAfterSecondFactorAuthenticationTimeout=yes`, a `restartAfterTime()`
  timer is set.
- If elapsed time >= `SecondFactorAuthenticationTimeout`: the user did not respond;
  if re-login is permitted, IBC re-initiates login after 5 seconds.

---

## 5. JtsIniManager — Role and Timing

### When jts.ini is Read and Written

`JtsIniManager.initialise()` is called **once**, inside `startTwsOrGateway()`, **before**
calling `jclient.LoginFrame.main()` or `ibgateway.GWClient.main()`. This is the only
mandatory write point during startup.

```
startTwsOrGateway()
  ├── SessionManager.startSession()
  ├── JtsIniManager.initialise(jtsIniFilePath)   <-- read/write here
  └── startTws() or startGateway()               <-- TWS/Gateway launched AFTER
```

### What initialise() Does

```
JtsIniManager.initialise(path)
  ├── loadIniFile()          // reads all lines from jts.ini if it exists
  ├── if file exists:
  │     updateExistingFile()
  │       ├── getMissingSettings()  // checks for required key=value pairs
  │       ├── if any setting wrong or missing:
  │       │     jtsIniFile.delete()
  │       │     rewriteExistingFile(missingSettings)  // rewrites file with corrections
  │       └── else: logs "contains required minimal lines"
  └── if file does not exist:
        createMinimalFile()   // writes a fresh minimal jts.ini
```

### Required Minimal Content (non-FIX mode)

```ini
[Logon]
s3store=true
Locale=en
displayedproxymsg=1
UseSSL=true

[IBGateway]
ApiOnly=true
```

For FIX Gateway mode, `[IBGateway]` gets `ApiOnly=false` instead, plus `TrustedIPs`
and optionally `LocalServerPort`.

### Why Each Setting Is Required

| Setting               | Section       | Reason                                                                                      |
| --------------------- | ------------- | ------------------------------------------------------------------------------------------- |
| `Locale=en`           | `[Logon]`     | Forces TWS/Gateway to run in English regardless of user locale                              |
| `displayedproxymsg=1` | `[Logon]`     | Suppresses the "cannot access internet" dialog (introduced May 2019)                        |
| `UseSSL=true`         | `[Logon]`     | Prevents TWS from displaying a dialog asking to restart with SSL (enforced since ~Aug 2021) |
| `s3store=true`        | `[Logon]`     | Ensures `Store settings on server` checkbox appears in TWS login dialog                     |
| `ApiOnly=true`        | `[IBGateway]` | Ensures Gateway 960+ shows the expected login form with trading mode selector               |

### `JtsIniManager.reload()`

A `reload()` method exists and re-reads the file from disk. It is not called during the
normal startup sequence; it is available for use after TWS/Gateway has modified the file.

---

## 6. TwsSettingsSaver — Automatic Save Scheduling

### When Settings Are Saved

`TwsSettingsSaver` schedules automatic invocations of `File > Save Settings` from the TWS
main window menu. It is initialized in `IbcTws.load()` **before** TWS is launched, but
the scheduled saves only execute once the main window is available (because
`MainWindowManager.mainWindowManager().getMainWindow()` is called at save time).

```
IbcTws.load()
  └── startSavingTwsSettingsAutomatically()
        └── TwsSettingsSaver.getInstance().initialise()
              └── reads SaveTwsSettingsAt setting
```

### Configuration Format

The `SaveTwsSettingsAt` setting accepts two formats:

**Explicit times:**

```
SaveTwsSettingsAt=08:00 12:00 17:00
```

Each time is parsed and scheduled for the next occurrence (adjusted to the following day
if the time has already passed today).

**Interval-based:**

```
SaveTwsSettingsAt=Every 30 mins [startTime [endTime]]
SaveTwsSettingsAt=Every 2 hours 08:00 17:00
```

Valid interval range: 1 to 1439 minutes. If no start/end times are given, coverage is
00:00 to 24:00. The end time itself is also scheduled as a save point.

### How Each Save Is Scheduled

Each computed save time is passed to `scheduleSave()`:

```java
MyScheduledExecutorService.getInstance().scheduleAtFixedRate(() -> {
    Utils.invokeMenuItem(
        MainWindowManager.mainWindowManager().getMainWindow(),
        new String[]{"File", "Save Settings"}
    );
}, initialDelay, 86400000 /* 24h in ms */, TimeUnit.MILLISECONDS);
```

The `scheduleAtFixedRate` period is exactly **86400000 ms (24 hours)**, meaning each
scheduled save repeats daily at the same wall-clock time.

If `SaveTwsSettingsAt` is empty or not set, `initialise()` returns immediately and no
saves are scheduled.

---

## 7. Timeout Reference

| Timeout              | Setting                                  | Default | Location                                                | Action on Expiry                                                           |
| -------------------- | ---------------------------------------- | ------- | ------------------------------------------------------- | -------------------------------------------------------------------------- |
| Login dialog display | `LoginDialogDisplayTimeout`              | 60 s    | `SessionManager.startSession()`                         | `exitWithError(LOGIN_DIALOG_DISPLAY_TIMED_OUT)`                            |
| 2FA total duration   | `SecondFactorAuthenticationTimeout`      | 180 s   | `LoginManager.secondFactorAuthenticationDialogClosed()` | If elapsed >= timeout: triggers re-login or exit                           |
| 2FA exit interval    | `SecondFactorAuthenticationExitInterval` | 60 s    | `LoginManager.restartAfterTime()`                       | `exitWithError(SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT)` if not yet `LOGGED_IN` |
| Scheduled shutdown   | `ClosedownAt`                            | none    | `IbcTws.startShutdownTimerIfRequired()`                 | `StopTask` executed (clean shutdown)                                       |
| Cold restart         | `ColdRestartTime`                        | none    | `IbcTws.startShutdownTimerIfRequired()`                 | `StopTask` executed with `isColdRestart=true`                              |
| `awaitReady()`       | none (indefinite)                        | —       | `SessionManager.awaitReady()`                           | Blocks forever until signaled                                              |
| Settings save        | `SaveTwsSettingsAt`                      | none    | `TwsSettingsSaver.scheduleSave()`                       | `File > Save Settings` menu invoked every 24 h                             |

### Notes on Timeout Behavior

- The `LoginDialogDisplayTimeout` timer is not set when `-Drestart` is detected as a JVM
  property, because on restart TWS re-establishes the session without showing a login dialog.
- If `ClosedownAt` and `ColdRestartTime` are both set, the earlier of the two wins;
  the winner determines whether the shutdown is a cold restart or a clean stop.
- `awaitReady()` has no timeout — if the main window or splash frame is never detected,
  all `ConfigurationTask` threads will block indefinitely (until JVM termination).

---

## 8. Complete Sequence Diagram

### TWS Startup

```
IbcTws.main()
│
├─[main thread]─────────────────────────────────────────────────────────────────────
│
│  checkArguments(args)
│  setupDefaultEnvironment(args, isGateway=false)
│    SessionManager.initialise(false)
│    Settings.initialise(DefaultSettings)
│    LoginManager.initialise(DefaultLoginManager)
│    MainWindowManager.initialise(DefaultMainWindowManager)
│    TradingModeManager.initialise(DefaultTradingModeManager)
│
│  load()
│    printVersionInfo()
│    printProperties()
│    [managers].logDiagnosticMessage()
│    startCommandServer()  ─────────────────────────────► [CommandServer thread]
│    startShutdownTimerIfRequired()
│      └─ if ClosedownAt/ColdRestartTime set:
│           schedule StopTask ──────────────────────────► [scheduled at configured time]
│    createToolkitListener()
│      Toolkit.addAWTEventListener(TwsListener, WINDOW_EVENT_MASK)
│    startSavingTwsSettingsAutomatically()
│      TwsSettingsSaver.initialise()
│        └─ schedule File>Save Settings ────────────────► [scheduled daily at configured times]
│    startTwsOrGateway()
│      SessionManager.startSession()
│        _isSessionStarted = true
│        schedule LoginDialogDisplayTimeout ────────────► [timer: default 60s]
│                                                            if loginState==LOGGED_OUT → exit
│      JtsIniManager.initialise(jtsIniFilePath)
│        loadIniFile()                  // read jts.ini
│        updateExistingFile()           // verify/patch required settings
│          or createMinimalFile()       // create from scratch if missing
│      configureResetOrderIdsAtStart()  ────────────────► [ConfigurationTask thread]
│      configureApiPort()               ────────────────► [ConfigurationTask thread]
│      configureMasterClientID()        ────────────────► [ConfigurationTask thread]
│      configureReadOnlyApi()           ────────────────► [ConfigurationTask thread]
│      configureSendMarketDataInLots()  ────────────────► [ConfigurationTask thread]
│      configureAutoLogoffOrRestart()   ────────────────► [ConfigurationTask thread]
│      configureApiPrecautions()        ────────────────► [ConfigurationTask thread]
│      startTws()
│        jclient.LoginFrame.main([settingsDir])  ◄── BLOCKS until TWS exits
│
│  [AWT event thread drives the rest via TwsListener]
│
├─[AWT event thread]─────────────────────────────────────────────────────────────────
│
│  TWS shows SplashScreen
│  TWS shows LoginFrame
│    LoginFrameHandler.recogniseWindow() → true
│    LoginFrameHandler.handleWindow()
│      loginState = LOGGING_IN
│      LoginHandler fills username, password, clicks Login
│        [if 2FA required]:
│          SecondFactorAuthenticationDialogHandler handles 2FA dialog
│          loginState = TWO_FA_IN_PROGRESS
│          [user completes 2FA]
│          secondFactorAuthenticationDialogClosed()
│            [if within SecondFactorAuthenticationTimeout]: continue
│            [if timeout]: re-initiate login or exit
│
│  TWS loads main window
│    MainWindowFrameHandler.recogniseWindow()
│      SwingUtils.findMenuItemInAnyMenuBar("File", "Lock Application") → found
│    MainWindowFrameHandler.handleWindow()
│      SessionManager.setMainWindow(window)
│        _InitialisationCompleted = true
│        initialised.signal()          ◄── UNBLOCKS all ConfigurationTask threads
│        MainWindowManager.setMainWindow(window)
│
│  [TWS is now FULLY OPERATIONAL]
│
├─[ConfigurationTask threads]────────────────────────────────────────────────────────
│
│  Each ConfigurationTask thread:
│    ConfigDialogManager.getConfigDialog()
│      → SessionManager.awaitReady()   ◄── BLOCKED until initialised.signal()
│      → opens Global Configuration dialog
│    configAction.run()
│      → modifies API settings in dialog
│    ConfigDialogManager.releaseConfigDialog()
│      → closes Global Configuration dialog
│
└────────────────────────────────────────────────────────────────────────────────────
```

### Gateway Startup

```
IbcGateway.main()
│
├─[main thread]─────────────────────────────────────────────────────────────────────
│
│  checkArguments(args)
│  IbcTws.setupDefaultEnvironment(args, isGateway=true)   ← KEY DIFFERENCE
│    SessionManager.initialise(true)                       ← isGateway=true
│    [same as TWS for remaining managers]
│
│  IbcTws.load()
│    [same pre-launch sequence as TWS]
│    startTwsOrGateway()
│      SessionManager.startSession()
│        [same as TWS: LoginDialogDisplayTimeout timer]
│      JtsIniManager.initialise(jtsIniFilePath)
│        [Gateway mode: checks [IBGateway]/ApiOnly, TrustedIPs, LocalServerPort]
│      [ConfigurationTask threads submitted]
│      startGateway()
│        ibgateway.GWClient.main([settingsDir])  ◄── BLOCKS until Gateway exits
│
├─[AWT event thread]─────────────────────────────────────────────────────────────────
│
│  Gateway immediately shows main window (before login)
│    GatewayMainWindowFrameHandler.recogniseWindow()
│      SwingUtils.findMenuItemInAnyMenuBar("Help", "About Gateway") → found
│    GatewayMainWindowFrameHandler.handleWindow()
│      SessionManager.setMainWindow(window)
│        _InitialisationCompleted = true   ← set early, but menus not yet responsive
│        initialised.signal()              ← premature signal; awaitReady() still needed
│        MainWindowManager.setMainWindow(window)
│
│  Gateway shows splash frame ("Starting application...")
│    [splash title changes multiple times during init]
│    [IBC ignores intermediate titles]
│
│  Gateway shows LoginFrame (GatewayLoginFrameHandler handles it)
│    loginState = LOGGING_IN
│    GatewayLoginHandler fills credentials, clicks Login
│
│  [if NonBrokerageAccountDialog appears]:
│    NonBrokerageAccountDialogHandler handles it
│      SessionManager.setNonBrokerageAccountDialogClosed()
│        nonBrokerageAccountDialogClosed = true
│        [waits for splashScreenClosed before signaling]
│
│  Splash frame closes (WINDOW_CLOSED on "Starting application...")
│    SplashFrameHandler.recogniseWindow() → true
│    SplashFrameHandler.handleWindow()
│      LoginManager.setLoginState(LOGGED_IN)
│      SessionManager.setSplashScreenClosed()
│        splashScreenClosed = true
│        if (nonBrokerageAccountDialogClosed):
│          _InitialisationCompleted = true
│          initialised.signal()   ◄── TRUE READINESS SIGNAL for Gateway
│
│  [Gateway is now FULLY OPERATIONAL — menus are responsive]
│
├─[ConfigurationTask threads]────────────────────────────────────────────────────────
│
│  Same as TWS: blocked in awaitReady() until initialised.signal()
│  Released only after splash closes AND NonBrokerageAccountDialog closed
│
└────────────────────────────────────────────────────────────────────────────────────
```

### Readiness Comparison

```
TWS readiness:

  jclient.LoginFrame.main() called
          │
          │  [login, 2FA if needed]
          │
  WINDOW_OPENED on JFrame with "File > Lock Application"
          │
          └──► SessionManager.setMainWindow()
                    _InitialisationCompleted = true
                    initialised.signal()
                         │
                         └──► TWS FULLY OPERATIONAL


Gateway readiness:

  ibgateway.GWClient.main() called
          │
  WINDOW_OPENED on JFrame with "Help > About Gateway"    ← early (menus not ready)
          │
          │  [login, 2FA if needed]
          │
  WINDOW_CLOSED on JFrame titled "Starting application..."
          │                                               ← splash closes = menus ready
          ├── SplashFrameHandler.handleWindow()
          │       LoginState = LOGGED_IN
          │       setSplashScreenClosed()
          │           splashScreenClosed = true
          │           if nonBrokerageAccountDialogClosed:
          │               initialised.signal()
          │                    │
  [NonBrokerageAccountDialog closes if shown]
          │
          └──► GATEWAY FULLY OPERATIONAL
```

---

_Document generated from source code analysis. All behaviors described are derived
exclusively from the files listed in the header. Applicable to IBC as found on branch
`develop` at commit `cbe436f`._
