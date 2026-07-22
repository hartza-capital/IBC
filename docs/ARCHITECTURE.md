# IBC Architecture — How to Automate a Java Swing Application

## The Fundamental Problem

TWS (Trader Workstation) and IB Gateway are closed Java Swing applications. They expose no automation API, no webhook, and no programmatic interface. The normal way to interact with them is a human sitting in front of a screen.

IBC solves this problem by exploiting a fundamental property of the Java runtime: **everything running in the same JVM shares the same graphical event space.**

---

## 1. The Same JVM — the Central Insight

IBC does not launch TWS as a child process (`ProcessBuilder`, `Runtime.exec`). It calls the `main()` of TWS directly in the **same Java process**:

```java
// IbcTws.java, startTws() method
jclient.LoginFrame.main(twsArgs);

// IbcGateway.java, startGateway() method
ibgateway.GWClient.main(twsArgs);
```

These classes (`jclient.LoginFrame`, `ibgateway.GWClient`) come from the TWS/Gateway jars, which are on the classpath at compile time (the `IBC_BIN` variable). This is why that variable is mandatory for building IBC.

**Direct consequence:** IBC and TWS share:

- The same JVM heap
- The same AWT `Toolkit` (graphical event manager)
- The same EDT (Event Dispatch Thread)
- The same `SecurityManager` (or its absence)

IBC can therefore interact with TWS windows exactly as TWS itself would.

---

## 2. The AWT Event Hook

Once in the same JVM, IBC installs a global listener on the AWT `Toolkit` — the central manager of all graphical events in the JVM:

```java
// IbcTws.java, createToolkitListener() method
Toolkit.getDefaultToolkit().addAWTEventListener(
    new TwsListener(createWindowHandlers()),
    AWTEvent.WINDOW_EVENT_MASK
);
```

`addAWTEventListener` is a public, official Java API (`java.awt.Toolkit`). It allows interception of **all** window events from **all** windows in the JVM, regardless of origin. When TWS opens its login window, IBC knows it.

The `AWTEvent.WINDOW_EVENT_MASK` mask filters on window-type events: `WINDOW_OPENED`, `WINDOW_ACTIVATED`, `WINDOW_CLOSED`, `WINDOW_CLOSING`, etc.

---

## 3. TwsListener — the Central Dispatcher

`TwsListener` implements `AWTEventListener`. Its `eventDispatched` method receives each window event:

```java
// TwsListener.java
@Override
public void eventDispatched(AWTEvent event) {
    final int eventID = event.getID();
    final Window window = ((WindowEvent) event).getWindow();

    GuiDeferredExecutor.instance().execute(() -> {
        for (WindowHandler wh : windowHandlers) {
            if (wh.recogniseWindow(window)) {
                if (wh.filterEvent(window, eventID)) {
                    wh.handleWindow(window, eventID);
                }
                return;
            }
        }
    });
}
```

Important points:

1. **`eventDispatched` is called on the EDT** — the main Swing thread. Processing is immediately delegated via `GuiDeferredExecutor` (a `SwingUtilities.invokeLater`) to avoid blocking the EDT during recognition and interaction.

2. **The handler list is traversed in order** — the first `recogniseWindow()` that returns `true` wins. The order in `createWindowHandlers()` is therefore significant (see the note in the code: `SecondFactorAuthenticationDialogHandler` must precede `SecurityCodeDialogHandler` because they share an identical button).

3. **Two separate steps:** `recogniseWindow` (is this the right window?) and `filterEvent` (is this the right event to act on?). A handler can recognize a window without acting on every event.

---

## 4. The WindowHandler Model

All handlers implement the `WindowHandler` interface:

```java
interface WindowHandler {
    boolean recogniseWindow(Window window);  // identify the window
    boolean filterEvent(Window window, int eventId);  // filter events
    void handleWindow(Window window, int eventID);  // act
}
```

### How `recogniseWindow` Works

TWS does not expose its internal classes. IBC identifies windows by their visible structure: title and components present. For example, for the TWS login window:

```java
// LoginFrameHandler.java (simplified)
@Override
public boolean recogniseWindow(Window window) {
    if (!(window instanceof JFrame)) return false;
    return SwingUtils.titleContains(window, "Login")
        && SwingUtils.findButton(window, "Login") != null;
}
```

The combination of title + presence of specific components identifies the window robustly even when the title alone is not enough.

### How `handleWindow` Acts

Once the window is identified, IBC traverses its Swing component tree and interacts directly with it:

```java
// SwingUtils.java — click a button by its text
static boolean clickButton(final Window window, final String buttonText) {
    final JButton button = findButton(window, buttonText);
    if (button == null) return false;
    button.doClick();  // equivalent to a human click
    return true;
}

// Fill a text field
static JTextField findTextField(Container container, int index) { ... }
```

`doClick()` is a standard `JButton` method that triggers the same `ActionEvent` as a real mouse click — for TWS, it is indistinguishable from a human interaction.

---

## 5. Traversing the Component Tree

`ComponentIterator` performs a depth-first search (DFS) of the Swing component tree. Each `Container` contains `Component[]` accessible via `getComponents()`.

```java
// ComponentIterator.java — DFS traversal with explicit stack
private class NodeState {
    NodeState(Component component) {
        synchronized (component.getTreeLock()) {  // mandatory Swing lock
            if (component instanceof Container)
                subComponents = ((Container)component).getComponents();
        }
    }
}
```

Critical note: `component.getTreeLock()` is Swing's internal lock for modifying the component tree. Taking it for reading is the correct way to iterate without risking a `ConcurrentModificationException` while another part of the code modifies the UI.

---

## 6. Threading Model

```text
Main thread
    └── IbcTws.main()
            ├── startCommandServer()     → "CommandServer" thread (TCP)
            ├── createToolkitListener()  → registers TwsListener on Toolkit
            └── jclient.LoginFrame.main() → starts TWS → creates the EDT

EDT (Event Dispatch Thread)  ← single Swing thread
    ├── Receives all AWTEvent from TWS
    ├── TwsListener.eventDispatched()  ← called here
    │       └── GuiDeferredExecutor.execute(handler)
    │               └── SwingUtilities.invokeLater(handler)  ← reposts on the EDT
    └── Handler runs here: doClick(), setText(), etc.

Thread "CommandServer"
    └── Accepts TCP connections
            └── CommandDispatcher → GuiSynchronousExecutor
                    └── SwingUtilities.invokeAndWait()  ← blocks until execution on EDT
```

**Why `invokeLater` in `eventDispatched`?**

`eventDispatched` is called during the dispatching of an AWT event — potentially in the middle of TWS processing. Running a handler directly here would block the EDT and could cause deadlocks. `invokeLater` places the handler in the EDT queue for execution **after** the current event is processed.

**`GuiSynchronousExecutor` vs `GuiDeferredExecutor`:**

| Class                    | Swing Method    | Usage                                        |
| ------------------------ | --------------- | -------------------------------------------- |
| `GuiDeferredExecutor`    | `invokeLater`   | Window handlers (asynchronous, non-blocking) |
| `GuiSynchronousExecutor` | `invokeAndWait` | TCP commands (must wait for the result)      |

---

## 7. Complete Startup Sequence

```text
IbcTws.main(args)
    │
    ├─ 1. setupDefaultEnvironment()
    │       ├─ SessionManager.initialise()
    │       ├─ Settings.initialise(new DefaultSettings(args))  ← reads config.ini
    │       ├─ LoginManager.initialise(new DefaultLoginManager(args))
    │       └─ TradingModeManager.initialise()
    │
    ├─ 2. load()
    │       ├─ startCommandServer()           → TCP thread starts
    │       ├─ startShutdownTimerIfRequired() → schedules StopTask if ClosedownAt is configured
    │       ├─ createToolkitListener()        → TwsListener installed on Toolkit
    │       ├─ startSavingTwsSettingsAutomatically()
    │       └─ startTwsOrGateway()
    │               ├─ JtsIniManager.initialise()  ← prepares jts.ini
    │               └─ jclient.LoginFrame.main()   ← TWS starts, creates its windows
    │
    └─ From this point on, TWS runs and IBC reacts to events
            │
            ├─ TWS opens LoginFrame
            │       → TwsListener receives WINDOW_OPENED
            │       → LoginFrameHandler.recogniseWindow() → true
            │       → LoginFrameHandler.handleWindow() → fills username/password, clicks Login
            │
            ├─ TWS displays 2FA dialog
            │       → SecondFactorAuthenticationDialogHandler
            │       → generates TOTP code (internal Totp/Base32 classes) or waits for IBKR Mobile
            │
            └─ TWS opens MainWindow
                    → MainWindowFrameHandler detects that the connection is established
                    → Configure*Task run (API port, master client ID, etc.)
```

---

## 8. Why This Approach Works Only with TWS

| Application                | UI Technology | Can it be automated like IBC?                      |
| -------------------------- | ------------- | -------------------------------------------------- |
| TWS / IB Gateway           | Java Swing    | Yes — same JVM, global AWTEventListener            |
| Native Windows application | Win32/GDI     | No — requires FindWindow/SendMessage (WinAPI)      |
| Electron application       | Chromium/JS   | No — would require DevTools Protocol or Playwright |
| Another Java Swing app     | Java Swing    | Yes — same technique applicable                    |

The complete openness of the Java runtime (no sandbox between libraries in the same process, fully accessible Swing tree, global Toolkit) is what makes IBC possible without any patch to TWS or access to TWS source code.

---

## 9. Robustness Against TWS Updates

The fragile point of IBC is window recognition in `recogniseWindow()`: if Interactive Brokers renames a button or changes a dialog title, the handler stops working.

Robustness strategies in the code:

- **Do not rely solely on the title**: combine title + presence of specific components (`findButton("Login") != null`)
- **`LogStructureScope` / `LogStructureWhen`**: debug parameters in `config.ini` that log the complete Swing tree structure of every window — useful for identifying how to recognize a new window after a TWS update
- **`GatewayDialogHandler`**: generic last-resort handler for unknown Gateway dialogs, at the bottom of the handler list

The `createWindowHandlers()` list in `IbcTws.java` is the exhaustive registry of everything IBC knows how to handle. Any new TWS window requires adding a handler there.

---

## 10. Configuration Tasks

### Why programmatic configuration is needed

After login TWS is running with whatever settings were last saved to disk. For unattended operation, IBC must ensure that the API socket is enabled, the port matches what trading clients expect, and optional precautions (bond warnings, volatility warnings, etc.) are in the correct state. TWS offers no CLI flags or config-file overrides for these settings: the only interface is the **Global Configuration** dialog, which is normally opened by a human from the menu. IBC opens and drives this dialog programmatically after every login.

### The ConfigDialogManager abstraction

`ConfigDialogManager` is a singleton that acts as a shared handle to the Global Configuration dialog. Any code that needs to modify TWS settings calls `ConfigDialogManager.configDialogManager().getConfigDialog()` and receives the live `JDialog` reference, blocking until the dialog is ready.

The default implementation is `DefaultConfigDialogManager`, which coordinates concurrent callers:

- It maintains a **usage counter** (`usageCount`). Each caller to `getConfigDialog()` increments it; each call to `releaseConfigDialog()` decrements it.
- When the counter reaches zero and the dialog was **not** opened by the user, the manager automatically clicks the **OK** button to close the dialog, then iconizes the main window if required.
- If a caller changes the API port while the "Enable ActiveX and Socket Clients" checkbox is already checked, TWS will show a confirmation prompt. The manager tracks this expectation via `setApiConfigChangeConfirmationExpected()` so the confirmation handler can dismiss it correctly.

### GetConfigDialogTask — opening the dialog

`GetConfigDialogTask` implements `Callable<JDialog>` and runs on a dedicated single-thread executor. Its `call()` method:

1. Retrieves the main TWS/Gateway window via `MainWindowManager`.
2. Calls `SessionManager.awaitReady()` — blocks until the splash screen has disappeared and all initialisation is complete (critical for Gateway, where menus are unresponsive during startup even after the window appears).
3. Invokes the appropriate menu item to open the dialog:
   - Gateway: `Configure > Settings`
   - TWS Classic layout: `Edit > Global Configuration...`
   - TWS Mosaic layout: `File > Global Configuration...`
4. Waits on a `ReentrantLock` / `Condition` pair until `setConfigDialog()` is called by the AWT event handler that detects the dialog opening (`WINDOW_OPENED` event).

```java
// GetConfigDialogTask.java — simplified flow
if (SessionManager.isGateway()) {
    Utils.invokeMenuItem(mainForm, new String[] {"Configure", "Settings"});
} else if (!Utils.invokeMenuItem(mainForm, new String[] {"Edit", "Global Configuration..."})) {
    Utils.invokeMenuItem(mainForm, new String[] {"File", "Global Configuration..."});
}
// then blocks until the dialog's WINDOW_OPENED event signals the condition
```

### Configure\*Task — the actual settings changes

Each `ConfigurationAction` implementation receives the dialog reference via `initialise()` and then performs its specific settings changes in `run()`. The tasks that run after a normal login are:

| Task                          | What it does                                                                                                                                                                                                      |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ConfigureTwsApiPortTask`     | Navigates to `API > Settings`, finds the "Socket port" text field, and sets it to the configured port number. Sets `apiConfigChangeConfirmationExpected` if the API was already enabled and the port is changing. |
| `ConfigureApiPrecautionsTask` | Navigates to `API > Precautions`, finds each precaution checkbox by its label text, and sets it to `yes` / `no` / leaves it unchanged (empty string).                                                             |
| `EnableApiTask`               | Finds the "Enable ActiveX and Socket Clients" checkbox; if not already checked, clicks it and immediately closes the dialog. Used by the `ENABLEAPI` TCP command rather than the startup sequence.                |

Navigation inside the dialog (e.g. `Utils.selectApiSettings()`, `Utils.selectConfigSection()`) works by clicking on tree nodes in the left-panel navigation tree of the Global Configuration dialog — the same way a human would expand "API" and then click "Settings".

### Execution sequence after login

```text
MainWindowFrameHandler.handleWindow(WINDOW_OPENED)
    └─ SessionManager.setMainWindow()          ← signals awaitReady()
            └─ triggers Configure*Task chain
                    ├─ DefaultConfigDialogManager.getConfigDialog()
                    │       ├─ submits GetConfigDialogTask on executor thread
                    │       │       └─ invokes menu → dialog opens → WINDOW_OPENED event
                    │       │               └─ ConfigDialogHandler.setConfigDialog() → signals condition
                    │       └─ returns JDialog to each waiting task
                    ├─ ConfigureTwsApiPortTask.run()
                    ├─ ConfigureApiPrecautionsTask.run()
                    └─ releaseConfigDialog() × N
                            └─ usageCount == 0 → click OK → close dialog → iconize
```

---

## 11. Session Lifecycle

### Session state flags

`SessionManager` holds the key flags that shape IBC's behaviour throughout a session:

| Flag        | Source                                                                | Effect                                                                                                                                                                                           |
| ----------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `isGateway` | Set at startup via `SessionManager.initialise(true/false)`            | Selects the correct menu paths (`Configure > Settings` vs `Edit/File > Global Configuration`), controls splash-screen handling, and changes the shutdown menu (`File > Close` vs `File > Exit`). |
| `isFIX`     | Read from `config.ini` key `FIX=true/false` at `startSession()`       | Determines whether IBC is controlling a FIX session (Order Management System mode) rather than a standard TWS/Gateway session.                                                                   |
| `isRestart` | Detected from the JVM system property `-Drestart` at `startSession()` | When set, IBC knows TWS will re-establish the previous session automatically without showing a login dialog. No login automation is attempted.                                                   |

### The `awaitReady()` barrier

Before any code tries to access menus or configuration, it must call `SessionManager.awaitReady()`. This call blocks on a `ReentrantLock` / `Condition` until `_InitialisationCompleted` is set to `true`.

For **TWS**, initialisation completes as soon as `MainWindowFrameHandler` sees the main window open (`setMainWindow()` signals the condition directly).

For **Gateway**, two independent events must both arrive before the condition is signalled:

1. The splash screen (`"Starting application..."` frame) closes — `setSplashScreenClosed()`.
2. The NonBrokerageAccount dialog closes (if shown) — `setNonBrokerageAccountDialogClosed()`.

This two-gate design prevents `GetConfigDialogTask` from invoking the `Configure > Settings` menu before Gateway has finished loading, which would cause a logged exception inside Gateway even though the menu item is already visible.

### Daily auto-restart

TWS and Gateway perform a mandatory daily restart at a configurable time (typically around 23:45 ET). This restart is entirely managed by TWS itself: it closes the current session and relaunches the application. IBC does not orchestrate this restart — it simply stays resident in the JVM and waits for TWS to go through its normal window sequence again.

From IBC's perspective, the auto-restart looks identical to the initial startup:

```text
TWS daily restart
    ├─ TWS closes its windows (IBC detects WINDOW_CLOSING / WINDOW_CLOSED events)
    ├─ TWS reopens the login dialog (if credentials have expired) or skips directly to main window
    │       → LoginFrameHandler handles credentials entry if needed
    │       → 2FA dialog may reappear → SecondFactorAuthenticationDialogHandler
    └─ TWS reopens main window
            → MainWindowFrameHandler triggers Configure*Task chain again
```

If the restart is a **cold restart** (forced via IBC's `STOP` command with `ForceColdRestart=yes`), IBC creates a `COLDRESTART<sessionid>` flag file in the `jtsConfigDir` before exiting. The external wrapper script reads this file to decide whether to re-launch IBC from scratch rather than waiting for TWS's internal restart.

### Login timeout watchdog

At `startSession()`, if `isRestart` is false, IBC schedules a watchdog that fires after `LoginDialogDisplayTimeout` seconds (default 60). If the login dialog has not been shown by then (`loginState == LOGGED_OUT`), IBC exits with error code `LOGIN_DIALOG_DISPLAY_TIMED_OUT`. This prevents IBC from hanging indefinitely if TWS fails to start.

---

## 12. Edge Cases

### NonBrokerageAccount dialog (paper trading warning)

When a paper-trading account is used with IB Gateway, Gateway displays a dialog with the label `"This is not a brokerage account"` immediately after login. This dialog must be dismissed before the Gateway menus become responsive.

`NonBrokerageAccountDialogHandler` handles two events:

- `WINDOW_OPENED`: if `AcceptNonBrokerageAccountWarning=true` (the default), clicks `"I understand and accept"` to dismiss the dialog automatically. Also schedules an iconize of the main window.
- `WINDOW_CLOSED`: calls `SessionManager.setNonBrokerageAccountDialogClosed()`, which contributes to the two-gate `awaitReady()` barrier described above.

If `AcceptNonBrokerageAccountWarning=false`, the handler does nothing on `WINDOW_OPENED` and the dialog stays open, blocking `GetConfigDialogTask` until a human dismisses it. This is intentional for users who want to confirm the paper-trading warning manually.

The recognition is purely label-based (no title check, since Gateway does not set a title on this dialog):

```java
// NonBrokerageAccountDialogHandler.java
public boolean recogniseWindow(Window window) {
    if (!(window instanceof JDialog)) return false;
    return (SwingUtils.findLabel(window, "This is not a brokerage account") != null);
}
```

### ExitConfirmation dialog

When IBC sends a STOP command, `StopTask.stop()` invokes the `File > Exit` menu item on TWS (or `File > Close` on Gateway). TWS responds by showing an "Are you sure you want to exit?" confirmation dialog. Without IBC intercepting it, the dialog would block the shutdown indefinitely.

`ExitConfirmationDialogHandler` handles this dialog, but only when a shutdown is actually in progress:

```java
// ExitConfirmationDialogHandler.java
public void handleWindow(Window window, int eventID) {
    if (!StopTask.shutdownInProgress()) return;  // user opened the dialog manually — ignore
    SwingUtils.clickButton(window, "Yes");
}
```

The `StopTask.shutdownInProgress()` guard is critical: the same dialog can appear if the user clicks the window close button manually. In that case IBC must not auto-confirm the exit (the user may have clicked it by accident). IBC only auto-confirms when it was IBC itself that triggered the shutdown via `StopTask`.

Recognition is label-based:

```java
SwingUtils.findLabel(window, "Are you sure you want to exit?") != null
```

### Dialogs without a title (untitled)

Some TWS/Gateway dialogs appear without a window title. This makes recognition via `SwingUtils.titleContains()` impossible, and forces IBC to identify them exclusively by the presence of specific labels or buttons inside the dialog.

When a new untitled dialog appears and no existing handler matches it, it will be silently ignored by `TwsListener` (the handler loop exits with no match). To diagnose what the dialog contains, use the `LogStructureScope` and `LogStructureWhen` settings in `config.ini`:

```text
# config.ini — log the full Swing component tree of every untitled dialog
LogStructureScope=untitled
LogStructureWhen=open
```

`LogStructureScope=untitled` restricts logging to windows whose title is empty or blank. The output lists every `Component` in the tree with its class, text/label, and position, giving enough information to write a new `recogniseWindow()` implementation.

Common untitled dialogs in practice:

- The NonBrokerageAccount paper-trading warning (Gateway only)
- Occasional API connection warning dialogs that vary by TWS version

Any handler for an untitled dialog must rely on a distinctive label or button text, and that text should be checked across multiple TWS versions to confirm stability.
