# ibcalpha.ibc — IBC Core Automation Engine

IBC is a Java Swing automation agent for Interactive Brokers' TWS (Trader Workstation) and IB Gateway. The `ibcalpha.ibc` package contains the complete core automation engine: GUI event interception, window recognition, dialog automation, login flow, configuration management, and remote command dispatch.

## Core Concept

IBC solves the automation problem by running TWS/Gateway in the same JVM process. This means IBC shares:

- The same Swing `Toolkit` (AWT event system)
- The same Event Dispatch Thread (EDT)
- Direct access to Swing component trees

Rather than scripting a child process, IBC hooks into Swing's central event system, recognizes dialogs and frames as they appear, and interacts with them programmatically.

## Architecture

### Entry Points

| Class        | Role                                                                                                                                         |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| `IbcTws`     | Main class for TWS automation. Sets up the event listener, initializes singletons, starts the command server, and loads TWS in the same JVM. |
| `IbcGateway` | Main class for IB Gateway automation. Same pattern as `IbcTws` but for the Gateway application.                                              |

### Core Event Loop

```
AWTEvent Stream (Toolkit)
    ↓
TwsListener.eventDispatched()
    ↓ (dispatches to EDT)
    ↓
for (WindowHandler wh : handlers)
    if (wh.recogniseWindow(window)) {
        if (wh.filterEvent(window, eventId)) {
            wh.handleWindow(window, eventId);
        }
    }
```

**TwsListener** is registered as a global `AWTEventListener` on the Toolkit. Every window event (`WINDOW_OPENED`, `WINDOW_ACTIVATED`, etc.) flows through it. For each event, it iterates through a list of `WindowHandler` instances to find and execute the first handler that recognizes the window.

**WindowHandler** is the fundamental abstraction — all dialog and frame handlers implement it:

```java
interface WindowHandler {
    boolean recogniseWindow(Window window);      // Is this the dialog/frame I'm looking for?
    boolean filterEvent(Window window, int id);  // Should I act on this event?
    void handleWindow(Window window, int id);    // What to do
}
```

### Key Singletons (All use static factory pattern)

| Class               | Role                                                                                                                                |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `Settings`          | Configuration abstraction backed by `config.ini`. Query settings like `IbLoginId`, `TradingMode`, `CommandServerPort`, etc.         |
| `LoginManager`      | Login state machine (`LOGGED_OUT` → `LOGGING_IN` → `TWO_FA_IN_PROGRESS` → `LOGGED_IN`). Tracks login progress and handles timeouts. |
| `SessionManager`    | Session lifecycle; distinguishes TWS vs Gateway, FIX vs IB API mode.                                                                |
| `MainWindowManager` | Tracks the main TWS/Gateway frame and provides hooks for main-window-ready detection.                                               |
| `CommandServer`     | TCP server (configurable port) for remote commands: `STOP`, `RESTART`, `RECONNECTDATA`, etc.                                        |

All singletons are initialized at startup in `IbcTws.setupDefaultEnvironment()`.

---

## Subsystems

### Entry Points & Initialization

**IbcTws.java** (43 KB)

- `main(String[])` — entry point; sets default uncaught exception handler and calls `load()`
- `setupDefaultEnvironment()` — initializes `Settings`, `LoginManager`, `SessionManager`, `MainWindowManager`, `TradingModeManager`
- `load()` — prints diagnostics, starts the command server, registers the Toolkit listener, schedules shutdown if configured, and starts auto-save of TWS settings
- `checkArguments()` — validates command-line parameters (config file, credentials, trading mode)

**IbcGateway.java** (2 KB)

- Same entry point pattern as `IbcTws` but calls Gateway's main entry point instead of TWS

### Core Event Loop & Window Recognition

**TwsListener.java** (8 KB)

- Implements `AWTEventListener`; registered on the Toolkit to receive all window events
- `eventDispatched()` — filters for window events and dispatches to handlers via `GuiDeferredExecutor`
- Logs window structure and component tree for debugging (controlled by `LogStructureScope` and `LogStructureWhen` settings)

**WindowHandler.java** (1 KB)

- Interface implemented by all dialog and frame handlers
- `recogniseWindow()` — identify the window by title and component presence
- `filterEvent()` — filter which events to act on (e.g., only `WINDOW_OPENED`)
- `handleWindow()` — execute the automation

**SwingUtils.java** (34 KB)

- Core utilities for interacting with Swing components
- `clickButton()` — find a button by text and simulate a click (enables if needed)
- `findButton()`, `setText()`, `getText()`, `setSelectedIndex()` — search and interact with components
- `getWindowStructure()` — dump the component tree for debugging
- `getWindowTitle()` — extract the title, with fallback for edge cases
- `windowEventToString()` — convert event IDs to readable names

**ComponentIterator.java** (3 KB)

- Depth-first traversal of Swing component hierarchies
- Used by `SwingUtils` to find buttons, text fields, etc.

### Window & Dialog Handlers (~50 classes, ~100 KB total)

All dialog handlers follow the `WindowHandler` pattern. Notable examples:

| Handler                                                 | Dialog                               | Action                                                                   |
| ------------------------------------------------------- | ------------------------------------ | ------------------------------------------------------------------------ |
| `LoginFrameHandler`                                     | TWS login dialog                     | Fill username, password, click Login; handle read-only mode              |
| `GatewayLoginFrameHandler`                              | IB Gateway login dialog              | Same flow but for Gateway UI                                             |
| `SecondFactorAuthenticationDialogHandler`               | 2FA prompt                           | Generate TOTP code from secret; handle IBKR Mobile push notifications    |
| `ExistingSessionDetectedDialogHandler`                  | "Account already logged in"          | Handle per `ExistingSessionDetectedAction` setting                       |
| `AcceptIncomingConnectionDialogHandler`                 | "Accept incoming API connection?"    | Handle per `AcceptIncomingConnectionAction` setting                      |
| `GlobalConfigurationDialogHandler`                      | Edit > Global Configuration          | Dispatch to `Configure*Task` classes to set API port, trading mode, etc. |
| `TradingModeConfirmationDialog...`                      | Paper/live mode switch confirmations | Confirm and log the mode change                                          |
| `NewerVersionDialogHandler`, `NewerVersionFrameHandler` | "A newer version is available"       | Auto-dismiss (click No for Linux)                                        |
| `ExitConfirmationDialogHandler`                         | "Exit TWS?" confirmation             | Auto-confirm based on `ClosedownAt` scheduling                           |

Handlers for less critical dialogs (Tip of the Day, Blind Trading Warning, etc.) dismiss or auto-confirm based on settings.

### Login & Authentication

**LoginManager.java** (8 KB)

- Abstract base; singleton pattern via static factory methods
- `LoginState` enum: `LOGGED_OUT`, `LOGGING_IN`, `TWO_FA_IN_PROGRESS`, `LOGGED_IN`, `LOGIN_FAILED`
- Tracks login progress and implements 2FA timeout/retry logic
- Holds the current login handler and login frame reference

**DefaultLoginManager.java** (9 KB)

- Concrete implementation; parses credentials from config file or command-line args
- `FIXUserName()`, `FIXPassword()`, `IBAPIUserName()`, `IBAPIPassword()` — return credentials
- `getLoginHandler()` / `setLoginHandler()` — register the active login handler (TWS or Gateway)

**AbstractLoginHandler.java** (11 KB)

- Base class for all login handlers; implements `WindowHandler`
- `recogniseWindow()` — identify the login dialog by title
- `filterEvent()` — only handle `WINDOW_OPENED` during `LOGGED_OUT` or `LOGIN_FAILED` states
- `handleWindow()` — call `initiateLogin()` to fill credentials and click Login
- `initiateLogin()` — override in subclasses

**LoginFrameHandler.java** (9 KB)

- TWS-specific login handler
- Fills `IbLoginId` and `IbPassword` into login fields
- Handles read-only login mode (`ReadOnlyLogin` setting)
- Implements periodic button clicking for reliability (handles TWS timing quirks)

**GatewayLoginFrameHandler.java** (10 KB)

- IB Gateway-specific login handler
- Similar pattern to `LoginFrameHandler` but for Gateway's UI layout

**SecondFactorAuthenticationDialogHandler.java** (9 KB)

- Handles 2FA prompt when `Ib2FA` is configured
- If `Ib2FA` is a base32 string: generates TOTP code using the in-tree `Totp`/`Base32` classes (RFC 4226/4648/6238)
- If `Ib2FA` is `"IBKR Mobile"`: waits for push notification, can retry on timeout
- Manages 2FA timeout/exit intervals via settings

### Session & State Management

**SessionManager.java** (6 KB)

- Static utility class tracking session properties (not instantiable)
- `isGateway()` — return true if running IB Gateway (vs. TWS)
- `isFIX()` — return true if FIX protocol mode is active
- `isRestart()` — return true if `-Drestart` VM option was passed (re-login without prompting)
- `startSession()` — initialize session; start login dialog timeout if first-time startup
- `awaitReady()` — synchronization point; blocks until configuration is complete

**LoginManager** (see above) — lifecycle hooks, 2FA retry logic, graceful shutdown on login timeout

**DefaultMainWindowManager.java** (7 KB)

- Concrete implementation of `MainWindowManager`
- Tracks when the main TWS/Gateway window is ready
- Provides coordination point for tasks that need the main window to exist

**MainWindowManager.java** (4 KB)

- Abstract base; singleton factory pattern
- `mainWindowManager()` — get singleton
- `logDiagnosticMessage()` — log configuration state

### Configuration Tasks

**ConfigurationTask.java** (2 KB)

- Wrapper that executes a `ConfigurationAction` asynchronously
- Gets the global configuration dialog, executes the action on the EDT, and releases the dialog
- Used for multi-step configuration like setting API port, trading mode, etc.

**ConfigurationAction.java** (1 KB)

- Interface for actions that modify configuration via the Global Configuration dialog
- `initialise(JDialog)` — set the config dialog reference
- `run()` — execute on the EDT; modify settings and click OK/Cancel

**ConfigurationTask-derived classes** (7 classes, ~20 KB total):

- `ConfigureApiPrecautionsTask` — set API warnings/restrictions
- `ConfigureReadOnlyApiTask` — enable read-only API mode
- `ConfigureTwsApiPortTask` — set or override the API listening port
- `ConfigureTwsMasterClientIDTask` — configure master client ID
- `ConfigureAutoLogoffOrRestartTimeTask` — set auto-logoff or auto-restart time
- `ConfigureResetOrderIdsTask` — reset the API order ID sequence
- `ConfigureSendMarketDataInLotsForUSstocksTask` — configure market data display

**ConfigDialogManager.java** (4 KB)

- Manages singleton access to the Global Configuration dialog
- `getConfigDialog()` — blocks until the dialog is available; used by configuration tasks
- `releaseConfigDialog()` — signal that the dialog is done being used

**DefaultConfigDialogManager.java** (8 KB)

- Concrete implementation
- Opens the Edit > Global Configuration menu and waits for the dialog to appear
- Implements a queue so multiple configuration tasks don't interfere

### Settings Management

**Settings.java** (3 KB)

- Abstract base; singleton factory pattern
- `getBoolean()`, `getInt()`, `getDouble()`, `getChar()`, `getString()` — query config values with defaults
- Backed by `.ini` file (default: `config.ini`)

**DefaultSettings.java** (11 KB)

- Concrete implementation; parses `config.ini`
- Supports property resolution from command-line arguments (override file settings)
- Handles encrypted passwords (decryption logic)
- `logDiagnosticMessage()` — log active settings on startup

**JtsIniManager.java** (19 KB)

- Reads and writes the TWS/Gateway `jts.ini` file (Java System Properties file format)
- Used by configuration tasks to persist settings like API port, trading mode
- Handles multi-platform paths and file locking

**JtsIniSectionSetting.java** (2 KB)

- Represents a section in `jts.ini` and a key-value pair within it
- Used by configuration tasks to navigate and modify `jts.ini`

### Command Server (Remote Control)

**CommandServer.java** (9 KB)

- TCP server (port from `CommandServerPort` setting; `0` = disabled)
- Listens for incoming connections on the configured port
- Dispatches each connection to a `CommandDispatcher` in a thread pool
- `run()` — main loop; accepts connections
- `shutdown()` — gracefully stop the server

**CommandDispatcher.java** (7 KB)

- Processes commands from a single client connection
- Maps command strings to actions: `STOP`, `RESTART`, `RECONNECTDATA`, `RECONNECTACCOUNT`, `PAUSE`, `ENABLEAPI`, `EXIT`, `WAITINIT` (legacy)
- Sends back `OK info`, `ERROR info`, or `INFO info` responses
- Coordinates with `LoginManager` for login state queries

**CommandChannel.java** (5 KB)

- Handles I/O for a single client connection
- `readCommand()` — read a line from the socket
- `sendMessage()` — send a response

### GUI Execution Utilities

**GuiDeferredExecutor.java** (2 KB)

- Singleton wrapper around `SwingUtilities.invokeLater()`
- Posts work to the Swing EDT asynchronously; never blocks
- Used by `TwsListener` to avoid blocking on EDT during event handling

**GuiSynchronousExecutor.java** (2 KB)

- Synchronous version; blocks until work completes on the EDT
- Uses `SwingUtilities.invokeAndWait()`

**GuiExecutor.java** (2 KB)

- Abstract base; factory for choosing sync vs. async execution

**MyCachedThreadPool.java** (1 KB)

- Wrapper around `Executors.newCachedThreadPool()`
- Applies common naming and uncaught exception handling

**MyScheduledExecutorService.java** (1 KB)

- Wrapper around `Executors.newScheduledThreadPool()`
- Used for timeouts, retry delays, auto-restart scheduling

### Lifecycle Tasks

**RestartTask.java** (10 KB)

- Initiates a clean TWS/Gateway restart by clicking the Restart button in the Exit dialog
- Supports both full restart and pause (continue without re-authentication) modes
- Handles `AutoRestartTime` and daily restart scheduling

**StopTask.java** (5 KB)

- Gracefully stops TWS/Gateway by clicking Exit in the main menu
- Confirms the exit dialog automatically

**TwsSettingsSaver.java** (7 KB)

- Periodically reads and saves TWS settings (e.g., API port, trading mode) to `jts.ini`
- Runs on a scheduled interval to persist configuration changes

### Utilities & Infrastructure

**Utils.java** (9 KB)

- Common utilities: logging (to console or TWS log file), error exit codes, stack trace formatting
- `logToConsole()` — log with timestamp
- `logException()` — log exception stack traces
- `exitWithError()` — exit with a specific error code and message

**ErrorCodes.java** (2 KB)

- Exit code constants: `INCORRECT_NUMBER_OF_ARGS`, `LOGIN_DIALOG_DISPLAY_TIMED_OUT`, `UNHANDLED_EXCEPTION`, etc.

**IbcException.java** (1 KB)

- Custom exception for IBC-specific errors

**UncaughtExceptionHandler.java** (1 KB)

- Global JVM exception handler; logs uncaught exceptions before exit

**SwitchLock.java** (2 KB)

- Synchronization utility for toggling boolean state atomically

---

## Configuration (Key Settings)

These settings live in `config.ini` and are queried via `Settings.settings().get*()`:

| Setting                          | Type   | Default       | Purpose                                      |
| -------------------------------- | ------ | ------------- | -------------------------------------------- |
| `IbLoginId`                      | string | (required)    | IBKR account username                        |
| `IbPassword`                     | string | (required)    | IBKR account password (encrypted by default) |
| `TradingMode`                    | string | `paper`       | `paper` or `live`                            |
| `Ib2FA`                          | string | `none`        | TOTP base32 secret, `IBKR Mobile`, or `none` |
| `CommandServerPort`              | int    | `0`           | TCP port for remote commands; `0` = disabled |
| `AutoRestartTime`                | string | (not set)     | Time to auto-restart daily (HH:MM format)    |
| `ClosedownAt`                    | string | (not set)     | Time to auto-shutdown daily (HH:MM format)   |
| `AcceptIncomingConnectionAction` | string | `accept`      | `accept`, `reject`, or `manual`              |
| `ExistingSessionDetectedAction`  | string | `manual`      | `primary`, `secondary`, or `manual`          |
| `OverrideTwsApiPort`             | int    | (auto-detect) | Force a specific API listening port          |
| `ReadOnlyLogin`                  | bool   | `false`       | Enable read-only API mode (TWS only)         |
| `ReadOnlyApiMasterClientID`      | string | (not set)     | Master client ID for read-only connections   |
| `FIX`                            | bool   | `false`       | Enable FIX protocol mode (Gateway only)      |

See `/docs/CONFIGURATION_REFERENCE.md` for complete reference.

---

## Adding a New Dialog Handler

1. Create a class implementing `WindowHandler`
2. Implement `recogniseWindow()` using `SwingUtils` to match the dialog title and/or a unique component
3. Implement `filterEvent()` — typically accept only `WINDOW_OPENED`, possibly with state checks
4. Implement `handleWindow()` — use `SwingUtils.clickButton()`, `setText()`, etc. to interact
5. Register the instance in the handler list in `IbcTws.createWindowHandlers()` or `IbcGateway.createWindowHandlers()`

**Example:**

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

---

## Threading Model

- **Event Dispatch Thread (EDT)** — Swing's main thread; all GUI operations must happen here
- **TwsListener** — runs on EDT (invoked by `Toolkit`)
- **GuiDeferredExecutor / GuiSynchronousExecutor** — post work to EDT
- **CommandServer** — runs in a dedicated thread; accepts connections in a loop
- **CommandDispatcher** — runs in a thread pool; processes commands concurrently
- **Configuration Tasks** — run on background threads; acquire the config dialog, execute on EDT, then release

**Lock-free patterns** — most state is immutable or volatile. `SessionManager` and `LoginManager` use `volatile` for state flags and synchronization primitives (`Lock`, `Condition`) for initialization barriers.

---

## Debugging & Logging

Enable diagnostic output via `config.ini`:

```ini
LogComponents=yes          # Log window component structure (legacy)
LogStructureScope=known    # Which windows to log: known, unknown, untitled, all
LogStructureWhen=open      # When to log: activate, open, openclose, never
LogToConsole=yes           # Output to console (default: TWS log file)
```

Use `SwingUtils.getWindowStructure()` to dump the component tree of any window — helpful for writing new handlers.

---

## Key Patterns

| Pattern                | Classes                                                                                             | Purpose                                                    |
| ---------------------- | --------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| **Static Factory**     | `Settings`, `LoginManager`, `SessionManager`, `MainWindowManager`, `CommandServer`                  | Thread-safe singleton access via static methods            |
| **WindowHandler**      | ~50 handler classes                                                                                 | Plug-in model for dialog automation                        |
| **Executor Wrappers**  | `GuiDeferredExecutor`, `GuiSynchronousExecutor`, `MyCachedThreadPool`, `MyScheduledExecutorService` | Consistent exception handling, naming, and EDT integration |
| **Configuration Task** | `ConfigurationTask` + `ConfigurationAction` + `Configure*Task` classes                              | Multi-step dialogs with async/sync coordination            |
| **State Machine**      | `LoginManager.LoginState`                                                                           | Tracks login progress atomically                           |

---

## Related Documentation

- `/docs/ARCHITECTURE.md` — Detailed explanation of how same-JVM automation works
- `/docs/WINDOW_HANDLERS.md` — Catalog of all window handlers and their responsibilities
- `/docs/LOGIN_FLOW.md` — Step-by-step login sequence (first-time, 2FA, re-login)
- `/docs/THREADING_MODEL.md` — Concurrency guarantees and deadlock avoidance
- `/docs/STARTUP_SEQUENCE.md` — Startup lifecycle and initialization order
- `/docs/COMMAND_SERVER.md` — Remote command protocol and examples
- `/docs/CONFIGURATION_REFERENCE.md` — Complete `config.ini` settings reference
