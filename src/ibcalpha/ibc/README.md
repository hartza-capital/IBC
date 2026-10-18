# ibcalpha.ibc — IBC Swing Automation Engine

IBC is a Java Swing automation agent for Interactive Brokers' TWS (Trader
Workstation) and IB Gateway. The `ibcalpha.ibc` package is the complete
automation engine: it hooks into the Swing event system, intercepts window
events, drives dialogs programmatically, performs login and 2FA, applies
configuration tasks, and exposes a TCP command server. It contains 83 top-level
classes.

IBC (and therefore this package) runs inside the same JVM as TWS/Gateway. It
shares the Swing `Toolkit`, the Event Dispatch Thread (EDT) and direct access to
the component trees, so instead of scripting a child process it hooks the AWT
event stream and reacts to windows as they appear.

```java
import ibcalpha.ibc.IbcTws;      // TWS entry point
import ibcalpha.ibc.IbcGateway; // IB Gateway entry point
```

## Overview

The package is a single unit; classes below are grouped by role. Visibility
follows the source: handlers and helpers are frequently package-private by
design (only what other packages need is `public`).

### Entry points and event dispatch

| Class               | Description                                                                  |
| ------------------- | ---------------------------------------------------------------------------- |
| `IbcTws`            | Main class for TWS; loads config, registers the listener, starts everything. |
| `IbcGateway`        | Main class for IB Gateway; same pattern, Gateway entry point.                |
| `TwsListener`       | `AWTEventListener` that dispatches window events to `WindowHandler`s.        |
| `WindowHandler`     | Interface implemented by every dialog/frame handler.                         |
| `ComponentIterator` | Depth-first iterator over a Swing component tree.                            |
| `SwingUtils`        | Static helpers to locate and manipulate Swing components by text.            |

### Management singletons (`Default*` implementations plus `Stub*` test doubles)

| Class                 | Description                                                         |
| --------------------- | ------------------------------------------------------------------- |
| `Settings`            | Abstract settings source; `DefaultSettings` is the `.ini` parser.   |
| `LoginManager`        | Abstract login state machine; `DefaultLoginManager` supplies creds. |
| `SessionManager`      | Session lifecycle and Gateway initialisation barrier.               |
| `MainWindowManager`   | Abstract accessor for the main TWS/Gateway window.                  |
| `TradingModeManager`  | Abstract accessor for the live/paper trading mode.                  |
| `ConfigDialogManager` | Abstract accessor for the Global Configuration dialog.              |
| `CommandServer`       | TCP server for remote commands.                                     |

### Window and dialog handlers (33 classes)

All implement `WindowHandler` and are registered, in priority order, by
`IbcTws.createWindowHandlers()`. `AbstractLoginHandler` is their abstract login
base.

### Support, configuration and infrastructure

`ConfigurationTask`, `ConfigurationAction`, the eight `Configure*Task` /
`EnableApiTask` actions, `GetConfigDialogTask`, `JtsIniManager`,
`JtsIniSectionSetting`, `GetMainWindowTask`, the three `Gui*Executor` wrappers,
`MyCachedThreadPool`, `MyScheduledExecutorService`, `RestartTask`, `StopTask`,
`TwsSettingsSaver`, `Totp`, `Base32`, `Utils`, `ErrorCodes`, `IbcException`,
`UncaughtExceptionHandler`, `SwitchLock`.

---

## IbcTws

TWS entry point. Private constructor (utility class).

### Methods

| Method                                      | Parameters                                 | Returns | Description                                                                                                              |
| ------------------------------------------- | ------------------------------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------ |
| `main`                                      | `(final String[] args)`                    | `void`  | Installs the default uncaught-exception handler, validates arguments, sets up the environment, calls `load()`.           |
| `load`                                      | `()`                                       | `void`  | Prints diagnostics, starts the command server, registers the Toolkit listener, schedules shutdown, starts settings save. |
| `setupDefaultEnvironment` (package-private) | `(final String[] args, boolean isGateway)` | `void`  | Initialises `SessionManager`, `Settings`, `LoginManager`, `MainWindowManager`, `TradingModeManager`.                     |
| `checkArguments` (package-private)          | `(String[] args)`                          | `void`  | Exits with `INCORRECT_NUMBER_OF_ARGS` above six arguments; never echoes their values.                                    |
| `validateTwoFactorAuthenticationSetting`    | `()`                                       | `void`  | Exits with `INVALID_SETTING_VALUE` when `Ib2FA` is set but is not valid Base32.                                          |
| `printVersionInfo`                          | `()`                                       | `void`  | Logs `IbcVersionInfo.IBC_VERSION` to the console.                                                                        |

Accepted argument forms: none; `<iniFile> [<tradingMode>]`;
`<iniFile> <apiUserName> <apiPassword> [<tradingMode>]`;
`<iniFile> <fixUserName> <fixPassword> <apiUserName> <apiPassword> [<tradingMode>]`.

### Example

```java
public static void main(String[] args) throws Exception {
    IbcTws.main(args); // starts TWS and automates it in-process
}
```

---

## IbcGateway

IB Gateway entry point. Delegates to `IbcTws.checkArguments` and
`IbcTws.setupDefaultEnvironment(args, true)`, then `IbcTws.load()`.

| Method             | Parameters        | Returns | Description                                            |
| ------------------ | ----------------- | ------- | ------------------------------------------------------ |
| `main`             | `(String[] args)` | `void`  | Installs the exception handler, validates args, loads. |
| `printVersionInfo` | `()`              | `void`  | Logs the IBC version.                                  |

---

## TwsListener

Package-private `AWTEventListener` registered on the Toolkit with
`AWTEvent.WINDOW_EVENT_MASK`. Every event is dispatched on the EDT via
`GuiDeferredExecutor`: the first handler whose `recogniseWindow` matches the
window is asked to `filterEvent`, and is then allowed to `handleWindow`. A
miss is logged (structure logging subject to `LogStructureScope` /
`LogStructureWhen`).

| Method                        | Parameters                                          | Returns | Description                                                               |
| ----------------------------- | --------------------------------------------------- | ------- | ------------------------------------------------------------------------- |
| `TwsListener`                 | `(List<WindowHandler> windowHandlers)`              | —       | Creates the listener and loads the structure-logging parameters.          |
| `eventDispatched`             | `(AWTEvent event)`                                  | `void`  | Filters window events, iterates the handlers, defers handling to the EDT. |
| `logWindow` (static)          | `(Window window, int eventID)`                      | `void`  | Logs type, title and event name of a detected window.                     |
| `logWindowStructure` (static) | `(Window window, int eventID, boolean windowKnown)` | `void`  | Dumps the component tree when scope and event match.                      |

---

## WindowHandler

Package-private interface implemented by all dialog and frame handlers.

```java
interface WindowHandler {
    boolean filterEvent(Window window, int eventId);   // act on this event?
    void handleWindow(Window window, int eventID);     // do the automation
    boolean recogniseWindow(Window window);            // is this my window?
}
```

---

## SwingUtils

Package-private static helpers that locate Swing components by their text. Used
by every handler and by `Utils`. `NO_TITLE` (`"** no title **"`) is returned by
`getWindowTitle` when a window has no usable title.

### Methods

| Method                     | Parameters                                             | Returns         | Description                                                 |
| -------------------------- | ------------------------------------------------------ | --------------- | ----------------------------------------------------------- |
| `clickButton`              | `(final Window window, final String buttonText)`       | `boolean`       | Finds a button by text, enables it if needed and clicks it. |
| `clickButton`              | `(final JButton button)`                               | `void`          | Clicks the given button (enabling it first if disabled).    |
| `findButton`               | `(Container container, String text)`                   | `JButton`       | Finds a button by text.                                     |
| `findCheckBox`             | `(Container container, String text)`                   | `JCheckBox`     | Finds a check box by text.                                  |
| `findRadioButton`          | `(Container container, String text)`                   | `JRadioButton`  | Finds a radio button by text.                               |
| `findToggleButton`         | `(Container container, String text)`                   | `JToggleButton` | Finds a toggle button by text.                              |
| `findLabel`                | `(Container container, String text)`                   | `JLabel`        | Finds a label by text.                                      |
| `findTextArea`             | `(Container container, String text)`                   | `JTextArea`     | Finds a text area by text.                                  |
| `findTextField`            | `(Container container, int ith)`                       | `JTextField`    | Finds the i-th text field.                                  |
| `findTextPane`             | `(Container container, String text)`                   | `JTextPane`     | Finds a text pane by text.                                  |
| `findComponent`            | `(Container container, String text)`                   | `Component`     | Finds a component by its rendered text.                     |
| `findMenuBar`              | `(Container container)`                                | `JMenuBar`      | Finds the first menu bar.                                   |
| `findMenuItem`             | `(MenuElement container, String text)`                 | `JMenuItem`     | Finds a menu item by text.                                  |
| `findMenuItem`             | `(Container container, String[] path)`                 | `JMenuItem`     | Finds a menu item by hierarchical path.                     |
| `findMenuItemInAnyMenuBar` | `(Container container, String[] path)`                 | `JMenuItem`     | Finds a menu item across every menu bar in the container.   |
| `findOptionPane`           | `(Container container)`                                | `JOptionPane`   | Finds the first option pane.                                |
| `findTree`                 | `(Container container)`                                | `JTree`         | Finds the first tree.                                       |
| `findChildNode`            | `(TreeModel model, Object node, String text)`          | `Object`        | Finds a child tree node by text.                            |
| `isButtonEnabled`          | `(final Window window, final String buttonText)`       | `boolean`       | Whether a named button exists and is enabled.               |
| `isCheckBoxSelected`       | `(Window window, String buttonText)`                   | `boolean`       | Whether a named check box is selected.                      |
| `isRadioButtonSelected`    | `(Window window, String buttonText)`                   | `boolean`       | Whether a named radio button is selected.                   |
| `setCheckBoxSelected`      | `(Window window, String buttonText, boolean value)`    | `boolean`       | Sets a named check box.                                     |
| `setRadioButtonSelected`   | `(Window window, String buttonText)`                   | `boolean`       | Sets a named radio button.                                  |
| `setTextField`             | `(Window window, int fieldNumber, final String value)` | `boolean`       | Sets the value of the i-th text field.                      |
| `getLabelTexts`            | `(Container container)`                                | `String`        | Concatenates all label texts.                               |
| `getTexts`                 | `(Container container)`                                | `String`        | Concatenates all component texts.                           |
| `getWindowStructure`       | `(Window window)`                                      | `String`        | Dumps the component tree for debugging/new handlers.        |
| `getWindowTitle`           | `(Window window)`                                      | `String`        | Window title, or `NO_TITLE`.                                |
| `titleContains`            | `(Window window, String text)`                         | `boolean`       | Whether the title contains the text.                        |
| `titleEquals`              | `(Window window, String text)`                         | `boolean`       | Whether the title equals the text.                          |
| `windowEventToString`      | `(int eventID)`                                        | `String`        | Human-readable name of a window event ID.                   |

---

## ComponentIterator

Package-private `Iterator<Component>` over a Swing container's subtree in
depth-first order. `remove` is unsupported.

| Method              | Parameters              | Returns     | Description                                      |
| ------------------- | ----------------------- | ----------- | ------------------------------------------------ |
| `ComponentIterator` | `(Container container)` | —           | Creates an iterator rooted at the container.     |
| `hasNext`           | `()`                    | `boolean`   | Whether another component is available.          |
| `next`              | `()`                    | `Component` | Next component; throws `NoSuchElementException`. |
| `remove`            | `()`                    | `void`      | Always throws `UnsupportedOperationException`.   |

---

## Window and Dialog Handlers

All 33 handlers implement `WindowHandler`. Registration order matters: the first
recognising handler wins. `SecondFactorAuthenticationDialogHandler` is
deliberately registered before `SecurityCodeDialogHandler` because both dialogs
contain an "Enter Read Only" button.

| Handler                                           | Recognises (title / label)                                                           | Action                                                                                  |
| ------------------------------------------------- | ------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| `AcceptIncomingConnectionDialogHandler`           | JDialog, label "Accept incoming connection"                                          | Per `AcceptIncomingConnectionAction` (`accept`/`reject`/`manual`): OK/Yes or No.        |
| `ApiChangeConfirmationDialogHandler`              | JDialog, label "apply the new socket port setting"                                   | Clicks Yes when an API config change confirmation is expected.                          |
| `BidAskLastSizeDisplayUpdateDialogHandler`        | JDialog "Bid, Ask and Last Size Display Update"                                      | Per `AcceptBidAskLastSizeDisplayUpdateNotification` (`accept`/`defer`/`ignore`).        |
| `BlindTradingWarningDialogHandler`                | Order Preview, labels "blind trading" + confirm                                      | Per `AllowBlindTrading`: Override and Transmit / Yes.                                   |
| `CryptoOrderConfirmationDialogHandler`            | JDialog "Cryptocurrency order confirmation"                                          | Per `ConfirmCryptoCurrencyOrders`: Transmit or Cancel.                                  |
| `ExistingSessionDetectedDialogHandler`            | JDialog "Existing session detected"                                                  | Per `ExistingSessionDetectedAction` (`primary`/`secondary`/`primaryoverride`/`manual`). |
| `ExitConfirmationDialogHandler`                   | JDialog, label "Are you sure you want to exit?"                                      | Confirms the exit (Yes).                                                                |
| `GatewayDialogHandler`                            | JDialog titled "Gateway"                                                             | OK; triggers a cold restart when the server connection has failed.                      |
| `GatewayLoginFrameHandler`                        | JFrame "IBKR Gateway"/"IB Gateway"/"Interactive Brokers Gateway" with a login button | Gateway login flow (see Login).                                                         |
| `GatewayMainWindowFrameHandler`                   | JFrame with Help > About Gateway menu item                                           | Registers the Gateway main window via `SessionManager.setMainWindow`.                   |
| `GlobalConfigurationDialogHandler`                | JDialog title contains "Configuration"                                               | On open, registers the dialog with `ConfigDialogManager`; on close, clears it.          |
| `LoginErrorDialogHandler`                         | JDialog "Login Error"                                                                | Logs the message, OK, then cold restart.                                                |
| `LoginFailedDialogHandler`                        | JDialog "Login failed"                                                               | Logs the failure and initiates a cold restart.                                          |
| `LoginFrameHandler`                               | JFrame "New Login"/"Login" with a login button                                       | TWS login flow (see Login).                                                             |
| `MainWindowFrameHandler`                          | JFrame (TWS main window)                                                             | Registers the main window via `SessionManager.setMainWindow`.                           |
| `NewerVersionDialogHandler`                       | JDialog with label "Newer Version"                                                   | OK, or No (Linux: decline the upgrade).                                                 |
| `NewerVersionFrameHandler`                        | JFrame with label "Newer Version"                                                    | Dismisses the newer-version notice.                                                     |
| `NonBrokerageAccountDialogHandler`                | JDialog label "This is not a brokerage account"                                      | Per `AcceptNonBrokerageAccountWarning`; signals dialog closed to `SessionManager`.      |
| `NotCurrentlyAvailableDialogHandler`              | Login dialog with label "not currently available"                                    | OK.                                                                                     |
| `NSEComplianceFrameHandler`                       | JFrame "NSE Compliance"                                                              | Per `DismissNSEComplianceNotice`.                                                       |
| `PasswordExpiryWarningFrameHandler`               | JFrame "Password Notice"                                                             | Per `DismissPasswordExpiryWarning`: OK.                                                 |
| `ReconnectDataOrAccountConfirmationDialogHandler` | Reconnect confirmation dialog                                                        | OK.                                                                                     |
| `ReloginDialogHandler`                            | JDialog "Re-login is required"                                                       | Clicks Re-login and sets state `LOGGING_IN`.                                            |
| `ResetOrderIdConfirmationDialogHandler`           | JDialog label "reset API order ID sequence"                                          | Per `ConfirmOrderIdReset`: Yes or No.                                                   |
| `RestartConfirmationDialogHandler`                | JDialog label "restart the application"                                              | Confirms the restart (Yes).                                                             |
| `SecondFactorAuthenticationDialogHandler`         | JFrame/JDialog "Second Factor Authentication" or "Security Code Card Authentication" | Read-only login, device selection, or automatic TOTP submission (see Login).            |
| `SecurityCodeDialogHandler`                       | JDialog with an "Enter Read Only" button                                             | Starts read-only login when `ReadOnlyLogin` is set.                                     |
| `ShutdownProgressDialogHandler`                   | JDialog "Shutdown progress"                                                          | Shuts down the thread pools and the command server.                                     |
| `SplashFrameHandler`                              | JFrame "Starting application..."                                                     | Uses the splash close to mark Gateway startup finished.                                 |
| `TipOfTheDayDialogHandler`                        | JDialog "Tip of the Day"                                                             | Closes the dialog.                                                                      |
| `TooManyFailedLoginAttemptsDialogHandler`         | JDialog text area "Too many failed login attempts"                                   | Per `ReloginAfterSecondFactorAuthenticationTimeout`: schedules a re-login, else OK.     |
| `TradesFrameHandler`                              | JFrame "Trades"                                                                      | Per `ShowAllTrades`: configures the Trades window and re-opens it if closed.            |
| `TradingLoginHandoffDialogHandler`                | JDialog "Trading Login Handoff"                                                      | Per `ExistingSessionDetectedAction`: take over this session or hand off.                |

---

## Login and Authentication

### AbstractLoginHandler

Public abstract base for login handlers; implements `WindowHandler`.
`filterEvent` accepts `WINDOW_OPENED` unless the login state is `LOGGED_IN`,
`LOGGING_IN` or `TWO_FA_IN_PROGRESS`. `handleWindow` records the handler and
login frame, then calls `initiateLogin` when state is `LOGGED_OUT` and the
session is not a `-Drestart`.

| Method (protected unless noted)         | Parameters                             | Returns   | Description                                                                 |
| --------------------------------------- | -------------------------------------- | --------- | --------------------------------------------------------------------------- |
| `filterEvent` (public)                  | `(Window window, int eventId)`         | `boolean` | Accepts `WINDOW_OPENED` per current login state.                            |
| `handleWindow` (public, final)          | `(Window window, int eventID)`         | `void`    | Registers handler/frame and initiates login.                                |
| `recogniseWindow` (public, abstract)    | `(Window window)`                      | `boolean` | Subclass identifies its login frame.                                        |
| `initiateLogin`                         | `(Window window)`                      | `void`    | Sets `AWAITING_CREDENTIALS`, then initialise, setFields, preLogin, doLogin. |
| `initialise` (abstract)                 | `(Window window, int eventID)`         | `boolean` | Frame-specific setup.                                                       |
| `preLogin` (abstract)                   | `(Window window, int eventID)`         | `boolean` | Credential completeness check.                                              |
| `setFields` (abstract)                  | `(Window window, int eventID)`         | `boolean` | Writes credentials into the frame.                                          |
| `isUserIdDisabledOrAbsent` (abstract)   | `(Window window)`                      | `boolean` | Whether the user id field is unusable.                                      |
| `isPasswordDisabledOrAbsent` (abstract) | `(Window window)`                      | `boolean` | Whether the password field is unusable.                                     |
| `currentLoginAttemptNumber`             | `()`                                   | `int`     | Number of login attempts made so far.                                       |
| `setCredential` (final)                 | `(Window, String, int, String)`        | `void`    | Sets a field or throws `IbcException`.                                      |
| `setMissingCredential` (final)          | `(Window window, int credentialIndex)` | `void`    | Focuses a missing/empty credential field.                                   |
| `setTradingMode` (final)                | `(Window window)`                      | `boolean` | Selects live/paper via toggle buttons; false triggers a cold restart.       |

`doLogin` clicks the login button; for live trading on TWS 1016+ it polls for
the login frame's title label to change from `LOGIN` to
`SECOND FACTOR AUTHENTICATION` and, when it does, passes the window to
`SecondFactorAuthenticationDialogHandler`.

### LoginFrameHandler

Package-private final subclass for TWS. `recogniseWindow` matches
`JFrame` titled `New Login`/`Login` with a `Log In`/`Paper Log In` button, or any
login frame during an auto-restart. It sets the trading mode, reloads
`jts.ini`, attaches a `DocumentListener` that keeps the "Use/store settings on
server" check box in sync with `StoreSettingsOnServer`, and fills fields 0/1
with `IBAPIUserName()`/`IBAPIPassword()`.

### GatewayLoginFrameHandler

Package-private final subclass for IB Gateway. Selects the gateway mode
(`FIX CTCI` when `FIX` is set, else `IB API`/`TWS/API`) and, when a "Trading
Mode" label is present, the trading mode. In FIX mode fields 0..3 are FIX
user/password then API user/password; otherwise fields 0/1 are API
user/password.

### SecondFactorAuthenticationDialogHandler

Public singleton (`getInstance()`), registered before `SecurityCodeDialogHandler`.
Handles `WINDOW_OPENED` and `WINDOW_CLOSED`. On open it takes, in order: a
read-only login (clicks "Enter Read Only" when `LoginManager.readonlyLoginRequired()`),
a second-factor device selection (when the dialog contains "Select second
factor device"), or an automatic TOTP submission.

- **Device selection** reads `SecondFactorDevice` and selects the matching entry
  in the dialog's list, then clicks OK. An empty `SecondFactorDevice` logs an
  error and leaves the dialog.
- **TOTP** reads `Ib2FA`, sets state `TWO_FA_IN_PROGRESS`, then calls
  `fillTotpCode`, which generates the code with `Totp`/`Base32` and writes it into
  the passcode field. The dialog is submitted with OK **only** when a code was
  written; otherwise it is logged and left open, because submitting an empty or
  wrong code spends a failed login attempt and IBKR locks the account after a few.
  An **empty** `Ib2FA` logs "You should specify the required 2FA secret using the
  Ib2FA setting in config.ini" and leaves the dialog open for a push notification
  or a manually entered code.
- A **non-empty** `Ib2FA` that is not valid Base32 stops IBC at startup
  (`IbcTws.validateTwoFactorAuthenticationSetting`), so a malformed secret never
  reaches the dialog. There is **no** special-casing of `none`, `IBKR Mobile` or
  any other literal string.

On close it calls `LoginManager.secondFactorAuthenticationDialogClosed()` (or
sets `LOGGED_IN` for read-only login).

| Method                                  | Parameters                     | Returns                                   | Description                                      |
| --------------------------------------- | ------------------------------ | ----------------------------------------- | ------------------------------------------------ |
| `getInstance` (static, package-private) | `()`                           | `SecondFactorAuthenticationDialogHandler` | Singleton accessor.                              |
| `filterEvent`                           | `(Window window, int eventId)` | `boolean`                                 | `WINDOW_OPENED` or `WINDOW_CLOSED`.              |
| `handleWindow`                          | `(Window window, int eventID)` | `void`                                    | Read-only / device selection / TOTP submission.  |
| `fillTotpCode` (package-private)        | `(Container container)`        | `boolean`                                 | Writes a fresh TOTP code; false if none written. |
| `recogniseWindow`                       | `(Window window)`              | `boolean`                                 | Matches the two dialog titles.                   |

### Totp

Package-private static utility generating 6-digit RFC 6238 TOTP codes over
HMAC-SHA1 (RFC 4226 dynamic truncation, RFC 4648 Base32), on a 30-second step.
No external library.

| Method (static, package-private) | Parameters | Returns | Description |

| `validateSecret`  | `(String base32Secret)`                                          | `void`   | Throws `IllegalArgumentException` when the secret is not valid Base32.                                        |
| ----------------- | ---------------------------------------------------------------- | -------- | ------------------------------------------------------------------------------------------------------------- |
| `timeStepCounter` | `(final long epochMillis)`                                       | `long`   | Whole 30-second steps since the epoch.                                                                        |
| `generate`        | `(final String base32Secret, final long timeStepCounter)`        | `String` | 6-digit passcode; throws `IllegalArgumentException` on bad Base32, `GeneralSecurityException` on JCE failure. |
| `generateHotp`    | `(final byte[] secretKey, final long counter, final int digits)` | `String` | HOTP passcode; `digits` must be 0..7.                                                                         |

### Base32

Package-private static decoder for RFC 4648 Base32.

| Method (static, package-private) | Parameters               | Returns  | Description                                                                                                               |
| -------------------------------- | ------------------------ | -------- | ------------------------------------------------------------------------------------------------------------------------- |
| `decode`                         | `(final String encoded)` | `byte[]` | Decodes, accepting whitespace, lowercase and trailing `=`; throws `IllegalArgumentException` on null/blank/invalid input. |

### LoginManager

Public abstract login state machine. Singleton via static factory, with a static
`DefaultLoginManager` initialiser.

`LoginState` enum: `LOGGED_OUT`, `LOGGED_IN`, `LOGGING_IN`,
`TWO_FA_IN_PROGRESS`, `LOGIN_FAILED`, `AWAITING_CREDENTIALS`.

| Method (abstract unless noted)        | Parameters                      | Returns                         | Description                                                          |
| ------------------------------------- | ------------------------------- | ------------------------------- | -------------------------------------------------------------------- |
| `initialise` (static)                 | `(LoginManager loginManager)`   | `void`                          | Sets the global instance; throws `IllegalArgumentException` if null. |
| `setDefault` (static)                 | `()`                            | `void`                          | Resets to a new `DefaultLoginManager`.                               |
| `loginManager` (static)               | `()`                            | `LoginManager`                  | Current global instance.                                             |
| `getLoginState`                       | `()`                            | `LoginState`                    | Current state.                                                       |
| `setLoginState`                       | `(LoginState state)`            | `void`                          | Sets state and logs transitions / manages the 2FA exit timer.        |
| `logDiagnosticMessage`                | `()`                            | `void`                          | Diagnostic output.                                                   |
| `FIXPassword` / `FIXUserName`         | `()`                            | `String`                        | FIX credentials.                                                     |
| `IBAPIPassword` / `IBAPIUserName`     | `()`                            | `String`                        | TWS credentials.                                                     |
| `getLoginHandler` / `setLoginHandler` | `()` / `(AbstractLoginHandler)` | `AbstractLoginHandler` / `void` | Active login handler.                                                |

Package-private helpers: `readonlyLoginRequired()`, `getLoginFrame()`,
`setLoginFrame(JFrame)`, `secondFactorAuthenticationDialogClosed()`,
`restartAfterTime(int, String)`. The 2FA timeout uses
`SecondFactorAuthenticationTimeout` (default 180s),
`SecondFactorAuthenticationExitInterval` (default 60s),
`ReloginAfterSecondFactorAuthenticationTimeout` and
`ExitAfterSecondFactorAuthenticationTimeout`.

### DefaultLoginManager

Public `LoginManager` implementation. Four constructors: no-arg (credentials from
settings), `(String[] args)`, `(String username, String password)` and
`(String FIXUsername, String FIXPassword, String IBAPIUsername, String
IBAPIPassword)`. Implements `FIXPassword`/`FIXUserName`/`IBAPIPassword`/
`IBAPIUserName`, `getLoginHandler`/`setLoginHandler`, `getLoginFrame`/
`setLoginFrame`, `logDiagnosticMessage`. Settings keys: `IbLoginId`/`IbPassword`
and `FIXLoginId`/`FIXPassword`.

---

## Session and State Management

### SessionManager

Public static-utility class (not instantiable) tracking session state and
carrying the Gateway initialisation barrier.

| Method (static)                                        | Parameters            | Returns   | Description                                                                   |
| ------------------------------------------------------ | --------------------- | --------- | ----------------------------------------------------------------------------- |
| `initialise`                                           | `(boolean isGateway)` | `void`    | Records whether this session is Gateway or TWS.                               |
| `isGateway`                                            | `()`                  | `boolean` | True for IB Gateway.                                                          |
| `isFIX`                                                | `()`                  | `boolean` | True for FIX mode; exits with `INVALID_STATE` if called before start.         |
| `isRestart` (package-private)                          | `()`                  | `boolean` | True when the `restart` system property is set.                               |
| `startSession` (package-private)                       | `()`                  | `void`    | Records FIX/restart flags and schedules an exit if the login dialog is late.  |
| `awaitReady` (package-private)                         | `()`                  | `void`    | Blocks until Gateway init (splash + non-brokerage dialog) or TWS main window. |
| `setSplashScreenClosed` (package-private)              | `()`                  | `void`    | Gateway-only; contributes to the barrier.                                     |
| `setNonBrokerageAccountDialogClosed` (package-private) | `()`                  | `void`    | Gateway-only; contributes to the barrier.                                     |
| `setMainWindow` (package-private)                      | `(JFrame window)`     | `void`    | Records the main window and signals the barrier.                              |

### MainWindowManager and DefaultMainWindowManager

Public abstract accessor plus its default implementation, which caches the main
window and blocks callers until it is available. `setMainWindow` marks login
complete for TWS and minimises the window when `MinimizeMainWindow` is set.

| Method (abstract unless noted) | Parameters                              | Returns             | Description                                                   |
| ------------------------------ | --------------------------------------- | ------------------- | ------------------------------------------------------------- |
| `initialise` (static)          | `(MainWindowManager mainWindowManager)` | `void`              | Sets the global instance (non-null).                          |
| `setDefault` (static)          | `()`                                    | `void`              | Resets to a new `DefaultMainWindowManager`.                   |
| `mainWindowManager` (static)   | `()`                                    | `MainWindowManager` | Current instance.                                             |
| `getMainWindow`                | `(long timeout, TimeUnit unit)`         | `JFrame`            | Blocks for the timeout (negative = forever); null on timeout. |
| `getMainWindow`                | `()`                                    | `JFrame`            | Blocks until available.                                       |
| `setMainWindow`                | `(JFrame window)`                       | `void`              | Records the window; completes init.                           |
| `iconizeIfRequired`            | `()`                                    | `void`              | Minimises when `MinimizeMainWindow` is set.                   |
| `logDiagnosticMessage`         | `()`                                    | `void`              | Diagnostic output.                                            |

### GetMainWindowTask

Package-private `Callable<JFrame>` that awaits a `ReentrantLock`/`Condition`
pair until `setMainWindow` supplies the frame.

| Method          | Parameters        | Returns  | Description                      |
| --------------- | ----------------- | -------- | -------------------------------- |
| `call`          | `()`              | `JFrame` | Blocks, then returns the window. |
| `setMainWindow` | `(JFrame window)` | `void`   | Records the window and signals.  |

### TradingModeManager and DefaultTradingModeManager

Public abstract accessor with constants `TRADING_MODE_LIVE` (`"live"`) and
`TRADING_MODE_PAPER` (`"paper"`).

| Method (abstract unless noted) | Parameters                                | Returns              | Description                                  |
| ------------------------------ | ----------------------------------------- | -------------------- | -------------------------------------------- |
| `initialise` (static)          | `(TradingModeManager tradingModeManager)` | `void`               | Sets the global instance.                    |
| `setDefault` (static)          | `()`                                      | `void`               | Resets to a new `DefaultTradingModeManager`. |
| `tradingModeManager` (static)  | `()`                                      | `TradingModeManager` | Current instance.                            |
| `getTradingMode`               | `()`                                      | `String`             | `live` or `paper`.                           |
| `logDiagnosticMessage`         | `()`                                      | `void`               | Diagnostic output.                           |

`DefaultTradingModeManager` resolves the mode from the no-arg constructor
(assumes `live`), an explicit `(String)`, or `(String[] args)` reading
`args[1]`, `args[3]` or `args[5]` for 2/4/6 arguments, falling back to the
`TradingMode` setting. Any other value exits with `INVALID_TRADING_MODE`.

---

## Configuration Tasks

Configuration runs after login by opening the Global Configuration dialog and
setting values.

### ConfigurationAction

Public interface extending `Runnable`.

| Method                  | Parameters               | Returns | Description                    |
| ----------------------- | ------------------------ | ------- | ------------------------------ |
| `initialise`            | `(JDialog configDialog)` | `void`  | Stores the config dialog.      |
| `run` (from `Runnable`) | `()`                     | `void`  | Applies the change on the EDT. |

### ConfigurationTask

Public wrapper binding an action to the dialog.

| Method              | Parameters                           | Returns | Description                               |
| ------------------- | ------------------------------------ | ------- | ----------------------------------------- |
| `ConfigurationTask` | `(ConfigurationAction configAction)` | —       | Wraps the action.                         |
| `executeAsync`      | `()`                                 | `void`  | Runs on the cached thread pool.           |
| `execute`           | `()`                                 | `void`  | Runs synchronously on the calling thread. |

### ConfigDialogManager and DefaultConfigDialogManager

Public abstract accessor for the Global Configuration dialog plus its default
implementation, which opens the dialog via the menu and tracks usage so it can
close it when the last task releases it.

| Method (abstract unless noted)           | Parameters                      | Returns               | Description                                                         |
| ---------------------------------------- | ------------------------------- | --------------------- | ------------------------------------------------------------------- |
| `initialise` (static)                    | `(ConfigDialogManager manager)` | `void`                | Sets the global instance.                                           |
| `setDefault` (static)                    | `()`                            | `void`                | Resets to a new `DefaultConfigDialogManager`.                       |
| `configDialogManager` (static)           | `()`                            | `ConfigDialogManager` | Current instance.                                                   |
| `getConfigDialog`                        | `(long timeout, TimeUnit unit)` | `JDialog`             | Blocks for the timeout; null on timeout or missing menu.            |
| `getConfigDialog`                        | `()`                            | `JDialog`             | Blocks until available.                                             |
| `releaseConfigDialog`                    | `()`                            | `void`                | Decrements usage; closes and re-minimises when the count hits zero. |
| `setConfigDialog`                        | `(JDialog window)`              | `void`                | Registers the dialog and releases a waiting task.                   |
| `clearConfigDialog`                      | `()`                            | `void`                | Clears the cached dialog.                                           |
| `getApiConfigChangeConfirmationExpected` | `()`                            | `boolean`             | Whether an API change confirmation is expected.                     |
| `setApiConfigChangeConfirmationExpected` | `()`                            | `void`                | Marks one as expected.                                              |
| `setApiConfigChangeConfirmationHandled`  | `()`                            | `void`                | Marks it handled.                                                   |
| `logDiagnosticMessage`                   | `()`                            | `void`                | Diagnostic output.                                                  |

Calling `getConfigDialog` from the EDT throws `IllegalStateException`.

### GetConfigDialogTask

Package-private `Callable<JDialog>` that opens the dialog through the menu:
`Configure > Settings` on Gateway; `Edit > Global Configuration...` (TWS
Classic) or `File > Global Configuration...` (TWS Mosaic). It waits for
`SessionManager.awaitReady()`.

| Method            | Parameters               | Returns   | Description                                                                           |
| ----------------- | ------------------------ | --------- | ------------------------------------------------------------------------------------- |
| `call`            | `()`                     | `JDialog` | Opens the dialog and blocks until registered; throws `IbcException` if no menu works. |
| `setConfigDialog` | `(JDialog configDialog)` | `void`    | Records the dialog and releases the waiter.                                           |

### Configuration action implementations

| Action                                         | Constructor                                                               | Applies                                                     |
| ---------------------------------------------- | ------------------------------------------------------------------------- | ----------------------------------------------------------- |
| `ConfigureApiPrecautionsTask`                  | nine `String` bypass flags (empty = unchanged)                            | API precaution bypass check boxes.                          |
| `ConfigureAutoLogoffOrRestartTimeTask`         | `(String autoActionName, String autoActionTime)` or `(String, LocalTime)` | Auto logoff / auto restart time.                            |
| `ConfigureReadOnlyApiTask`                     | `(boolean readOnlyApi)`                                                   | Read-Only API check box.                                    |
| `ConfigureResetOrderIdsTask`                   | `(boolean resetOrderIds)`                                                 | Resets the API order ID sequence.                           |
| `ConfigureSendMarketDataInLotsForUSstocksTask` | `(boolean sendMarketDataInLots)`                                          | Send Market Data In Lots for US stocks.                     |
| `ConfigureTwsApiPortTask`                      | `(int portNumber)`                                                        | API socket port.                                            |
| `ConfigureTwsMasterClientIDTask`               | `(String masterClientID)`                                                 | Master API client ID.                                       |
| `EnableApiTask`                                | `(CommandChannel channel)`                                                | Enables ActiveX and socket clients; replies on the channel. |

Each implements `ConfigurationAction` (`initialise(JDialog)` and `run()`).
`ConfigureApiPrecautionsTask`, `ConfigureReadOnlyApiTask`,
`ConfigureResetOrderIdsTask` and `ConfigureSendMarketDataInLotsForUSstocksTask`
are `public`; `ConfigureAutoLogoffOrRestartTimeTask`, `ConfigureTwsApiPortTask`,
`ConfigureTwsMasterClientIDTask` and `EnableApiTask` are package-private.

---

## Settings and JTS INI

### Settings

Public abstract configuration source.

| Method (abstract unless noted) | Parameters                           | Returns    | Description                                              |
| ------------------------------ | ------------------------------------ | ---------- | -------------------------------------------------------- |
| `initialise` (static)          | `(Settings settings)`                | `void`     | Sets the global instance (non-null).                     |
| `setDefault` (static)          | `()`                                 | `void`     | Resets to a new `DefaultSettings`.                       |
| `settings` (static)            | `()`                                 | `Settings` | Current instance.                                        |
| `getString`                    | `(String key, String defaultValue)`  | `String`   | String value or default.                                 |
| `getInt`                       | `(String key, int defaultValue)`     | `int`      | Int value or default.                                    |
| `getDouble`                    | `(String key, double defaultValue)`  | `double`   | Double value or default.                                 |
| `getChar`                      | `(String key, String defaultValue)`  | `char`     | First character of the value, else first of the default. |
| `getBoolean`                   | `(String key, boolean defaultValue)` | `boolean`  | Boolean value or default.                                |
| `logDiagnosticMessage`         | `()`                                 | `void`     | Diagnostic output.                                       |

### DefaultSettings

Public `.ini` parser backed by `java.util.Properties`. Constructors:
`()`, `(String[] args)`, `(String path)`. It logs the active settings with
`IbLoginId`/`IbPassword`/`FIXLoginId`/`FIXPassword` masked as `***`, and
implements all typed accessors. Default path is
`<user.home>/ibc/config.ini`; with no arguments the path is
`<workingDir>/config.<user>.ini`. An existing-but-directory or missing ini file
exits with `INI_FILE_DOES_NOT_EXIST`.

### JtsIniManager

Package-private static utility that creates or repairs the TWS/Gateway
`jts.ini` so it holds the minimum settings IBC requires.

| Method (static, package-private) | Parameters            | Returns | Description                                               |
| -------------------------------- | --------------------- | ------- | --------------------------------------------------------- |
| `initialise`                     | `(String jtsIniPath)` | `void`  | Loads the file, then updates it or creates a minimal one. |
| `reload`                         | `()`                  | `void`  | Re-reads the file from disk.                              |

Required lines depend on mode: for FIX, `[IBGateway] ApiOnly`, `TrustedIPs`
(always `127.0.0.1`, plus `TrustedTwsApiClientIPs`) and `LocalServerPort` when
`OverrideTwsApiPort` is set; otherwise `[Logon] s3store/Locale/displayedproxymsg/UseSSL`
and `[IBGateway] ApiOnly`, plus `TrustedIPs` for Gateway.

### JtsIniSectionSetting

Package-private value holder for a pending ini change.

| Field         | Type      | Meaning                          |
| ------------- | --------- | -------------------------------- |
| `section`     | `String`  | Section header, e.g. `[Logon]`.  |
| `setting`     | `String`  | Setting line, e.g. `Locale=en`.  |
| `isProcessed` | `boolean` | Whether it has been written out. |

---

## Command Server

### CommandServer

Package-private `Runnable` TCP server. `CommandServerPort` = `0` disables it.
Accepts one connection at a time and dispatches each to a `CommandDispatcher`
on `MyCachedThreadPool`. Clients must be local, loopback, or listed in
`ControlFrom`; `BindAddress` optionally restricts the listening address.

| Method                   | Parameters | Returns         | Description                                                 |
| ------------------------ | ---------- | --------------- | ----------------------------------------------------------- |
| `CommandServer`          | `()`       | —               | Singleton; throws `IllegalArgumentException` if one exists. |
| `commandServer` (static) | `()`       | `CommandServer` | Singleton accessor.                                         |
| `run`                    | `()`       | `void`          | Listen/accept loop.                                         |
| `shutdown`               | `()`       | `void`          | Stops the loop and closes the socket.                       |

### CommandDispatcher

Package-private `Runnable` that reads commands from one `CommandChannel`.
Commands: `EXIT`, `STOP`, `ENABLEAPI`, `RECONNECTDATA`, `RECONNECTACCOUNT`,
`RESTART`, `PAUSE` (unknown commands are rejected). `ENABLEAPI` is TWS-only;
`RECONNECTDATA`, `RECONNECTACCOUNT`, `RESTART` and `PAUSE` are refused for FIX.

| Method              | Parameters                       | Returns | Description                                           |
| ------------------- | -------------------------------- | ------- | ----------------------------------------------------- |
| `CommandDispatcher` | `(final CommandChannel channel)` | —       | Binds the dispatcher to a channel.                    |
| `run`               | `()`                             | `void`  | Command loop until `EXIT` or disconnect, then closes. |

### CommandChannel

Package-private final I/O handler for one connection. `CommandPrompt` sets the
prompt (empty = none); `SuppressInfoMessages` (default true) suppresses `INFO`
lines.

| Method           | Parameters        | Returns  | Description                                                      |
| ---------------- | ----------------- | -------- | ---------------------------------------------------------------- |
| `CommandChannel` | `(Socket socket)` | —        | Sets up streams and writes the `INFO IBC Command Server` banner. |
| `getCommand`     | `()`              | `String` | Next non-blank command line, or null on disconnect.              |
| `writeAck`       | `(String info)`   | `void`   | Sends `OK <info>`.                                               |
| `writeInfo`      | `(String info)`   | `void`   | Sends `INFO <info>` unless suppressed.                           |
| `writeNack`      | `(String info)`   | `void`   | Sends `ERROR <info>`.                                            |
| `writePrompt`    | `()`              | `void`   | Sends the configured prompt.                                     |
| `close`          | `()`              | `void`   | Closes streams and socket.                                       |

---

## GUI Execution and Thread Pools

| Class                        | Methods                           | Description                                               |
| ---------------------------- | --------------------------------- | --------------------------------------------------------- |
| `GuiExecutor`                | `instance()`, `execute(Runnable)` | Runs on the EDT, executing inline when already on it.     |
| `GuiDeferredExecutor`        | `instance()`, `execute(Runnable)` | Schedules on the EDT via `invokeLater` without blocking.  |
| `GuiSynchronousExecutor`     | `instance()`, `execute(Runnable)` | Runs on the EDT via `invokeAndWait`, blocking the caller. |
| `MyCachedThreadPool`         | `getInstance()`                   | Shared cached thread pool.                                |
| `MyScheduledExecutorService` | `getInstance()`                   | Shared single-thread scheduled executor.                  |

All are package-private except `MyCachedThreadPool` and
`MyScheduledExecutorService` (public, though `getInstance` is package-private).

---

## Lifecycle Tasks

### RestartTask

Package-private `Runnable` that restarts or pauses TWS/Gateway. The constructor
records the reporting channel and whether this is a pause. `run` guards against
concurrent restarts with a `SwitchLock`, writes a `PAUSE<sessionid>` flag file
when pausing, then tries `File > Restart...`; if that fails it schedules an
auto-restart one minute ahead via `ConfigureAutoLogoffOrRestartTimeTask` and
draws a translucent countdown over the main window.

### StopTask

Package-private `Runnable` that shuts down TWS/Gateway. `run` guards with a
`SwitchLock`, writes a `COLDRESTART<sessionid>` flag file when forcing a cold
restart, then: if login is incomplete, halts immediately; otherwise invokes
`File > Close` (Gateway) or `File > Exit` (TWS).

| Method (static)      | Parameters | Returns   | Description                      |
| -------------------- | ---------- | --------- | -------------------------------- |
| `shutdownInProgress` | `()`       | `boolean` | Whether a shutdown is under way. |

### TwsSettingsSaver

Package-private singleton that schedules `File > Save Settings` at the times in
`SaveTwsSettingsAt` (`hh:mm [hh:mm]...` or
`Every n [{mins \| hours}] [hh:mm [hh:mm]]`).

| Method                 | Parameters | Returns            | Description                                 |
| ---------------------- | ---------- | ------------------ | ------------------------------------------- |
| `getInstance` (static) | `()`       | `TwsSettingsSaver` | Singleton accessor.                         |
| `initialise`           | `()`       | `void`             | Parses the setting and schedules the saves. |

---

## Utilities

### Utils

Package-private static helpers for Swing menu interaction, logging and process
termination. `invokeMenuItem` and `selectConfigSection` must not be called on
the EDT.

| Method (static)       | Parameters                                          | Returns       | Description                                                                        |
| --------------------- | --------------------------------------------------- | ------------- | ---------------------------------------------------------------------------------- |
| `invokeMenuItem`      | `(final Container container, final String[] path)`  | `boolean`     | Clicks a menu item by path, waiting for it to enable; false if not found.          |
| `selectConfigSection` | `(final JDialog configDialog, final String[] path)` | `boolean`     | Selects a section in the config tree.                                              |
| `selectApiSettings`   | `(final JDialog configDialog)`                      | `void`        | Selects `API > Settings`, falling back to `API`.                                   |
| `showTradesLogWindow` | `()`                                                | `void`        | Opens Account > Trade Log on a background thread.                                  |
| `exitWithError`       | `(final int errorCode)`                             | `void`        | Logs and halts the JVM with the code.                                              |
| `exitWithError`       | `(final int errorCode, final String message)`       | `void`        | Logs the message then halts.                                                       |
| `exitWithException`   | `(final int errorCode, final Throwable t)`          | `void`        | Logs the exception then halts.                                                     |
| `exitWithoutError`    | `()`                                                | `void`        | `System.exit(0)`.                                                                  |
| `logError`            | `(final String message)`                            | `void`        | Logs a banner-wrapped error.                                                       |
| `logException`        | `(final Throwable t)`                               | `void`        | Logs a banner-wrapped exception (stack trace if `IncludeStackTraceForExceptions`). |
| `logRawToConsole`     | `(String msg)`                                      | `void`        | Writes a plain line.                                                               |
| `logToConsole`        | `(String msg)`                                      | `void`        | Writes a line with timestamp and `IBC:` prefix.                                    |
| `getErrStream`        | `()`                                                | `PrintStream` | Error stream.                                                                      |
| `getOutStream`        | `()`                                                | `PrintStream` | Console stream.                                                                    |
| `formatDate`          | `(LocalDateTime date)`                              | `String`      | Formats with IBC's timestamp pattern.                                              |
| `pause`               | `(int millis)`                                      | `void`        | Sleeps, swallowing interruption.                                                   |
| `stringToHex`         | `(String value)`                                    | `String`      | Hex of each character, concatenated.                                               |

### ErrorCodes

Public final class of exit codes:

| Constant                             | Value |
| ------------------------------------ | ----- |
| `UNHANDLED_EXCEPTION`                | 1100  |
| `CANT_FIND_CONTROL`                  | 1101  |
| `INCORRECT_NUMBER_OF_ARGS`           | 1102  |
| `INI_FILE_DOES_NOT_EXIST`            | 1103  |
| `INVALID_TRADING_MODE`               | 1104  |
| `INVALID_SETTING_VALUE`              | 1105  |
| `CANT_CREATE_TWS_SETTINGS_DIR`       | 1106  |
| `CANT_FIND_ENTRYPOINT`               | 1107  |
| `INVALID_JTSINI_PATH`                | 1108  |
| `IO_EXCEPTION_ON_JTSINI`             | 1109  |
| `INVALID_STATE`                      | 1110  |
| `SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT` | 1111  |
| `LOGIN_DIALOG_DISPLAY_TIMED_OUT`     | 1112  |

### IbcException

Public `Exception` subclass carrying a human-readable message. The constructor
is package-private; `toString()` returns the message.

### UncaughtExceptionHandler

Public final `Thread.UncaughtExceptionHandler` that logs the throwable and exits
with `UNHANDLED_EXCEPTION`.

### SwitchLock

Package-private one-shot latch used to detect that an operation is already in
progress.

| Method  | Parameters | Returns   | Description                                 |
| ------- | ---------- | --------- | ------------------------------------------- |
| `set`   | `()`       | `boolean` | Sets the flag; false if it was already set. |
| `clear` | `()`       | `void`    | Clears the flag.                            |
| `query` | `()`       | `boolean` | Current flag state.                         |

---

## Adding a New Dialog Handler

1. Create a class implementing `WindowHandler`
2. Implement `recogniseWindow()` using `SwingUtils` to match the title and/or a
   unique component
3. Implement `filterEvent()` - typically accept only `WINDOW_OPENED`, possibly
   with state checks
4. Implement `handleWindow()` using `SwingUtils.clickButton()`, `setTextField()`,
   etc.
5. Register the instance in `IbcTws.createWindowHandlers()`

```java
class MyDialogHandler implements WindowHandler {
  @Override
  public boolean recogniseWindow(Window window) {
    if (!(window instanceof JDialog)) return false;
    return SwingUtils.titleContains(window, "My Dialog")
        && SwingUtils.findButton(window, "OK") != null;
  }

  @Override
  public boolean filterEvent(Window window, int eventId) {
    return eventId == WindowEvent.WINDOW_OPENED;
  }

  @Override
  public void handleWindow(Window window, int eventID) {
    SwingUtils.clickButton(window, "OK");
  }
}
```

To capture a dialog's component tree while writing a handler, use
`SwingUtils.getWindowStructure()` and the `LogStructureScope` /
`LogStructureWhen` settings.

---

## Threading Model

- **EDT** - Swing's thread; all GUI work runs here.
- **TwsListener** - invoked by the Toolkit on the EDT; defers handling through
  `GuiDeferredExecutor`.
- **GuiExecutor / GuiDeferredExecutor / GuiSynchronousExecutor** - move work
  onto the EDT (inline, deferred, or blocking).
- **CommandServer** - a dedicated thread (from the cached pool) running the
  accept loop.
- **CommandDispatcher** - one task per client on the cached pool.
- **Configuration tasks** - run off the EDT, acquire the Global Configuration
  dialog, execute on the EDT, then release it.

State is mostly immutable or `volatile`; `SessionManager` and `LoginManager` use
`Lock`/`Condition` for initialisation barriers, and `StopTask`/`RestartTask`
guard against concurrent runs with `SwitchLock`.

---

## Related Documentation

- [`AGENTS.md`](../../../AGENTS.md) - agent guidance, build and conventions
- [`CONTRIBUTING.md`](../../../CONTRIBUTING.md) - build, handler pattern, PR checklist
- [`README.md`](../../../README.md) - end-user installation, configuration and
  startup scripts
