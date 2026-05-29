# Window Handlers — Developer Reference

This document describes the window-handler subsystem of IBC: how it is wired together,
how individual handlers work, and how to add support for a new TWS/Gateway window.

---

## 1. The `WindowHandler` Interface

**File:** `src/ibcalpha/ibc/WindowHandler.java`

```java
interface WindowHandler {
    boolean recogniseWindow(Window window);
    boolean filterEvent(Window window, int eventId);
    void    handleWindow(Window window, int eventID);
}
```

Every concrete handler must implement these three methods. They are called in the
order shown above for every AWT window event that IBC intercepts.

### `boolean recogniseWindow(Window window)`

**Contract:** Return `true` if and only if this handler is responsible for the given
window. The implementation must be fast and side-effect-free because it is called for
every window event against every registered handler.

**When called:** Before any filtering or handling, once per event, inside the
`GuiDeferredExecutor` Swing thread. The first handler in the list whose
`recogniseWindow` returns `true` claims the window exclusively — no other handler
will be tried for that event.

**Typical logic:** Check the Java type of the window (`instanceof JDialog` /
`instanceof JFrame`), then inspect the title or a characteristic component.

---

### `boolean filterEvent(Window window, int eventId)`

**Contract:** Return `true` if the handler wants to act on this particular window
event (`eventId` is one of the `WindowEvent.*` constants). Return `false` to silently
skip the event without logging or taking any action.

**When called:** Only called after `recogniseWindow` has returned `true` for this
window and event pair. The handler is responsible for knowing which events are
meaningful for its window. For example, a login dialog only needs `WINDOW_OPENED`;
the Global Configuration dialog needs both `WINDOW_OPENED` and `WINDOW_CLOSED`.

**Typical pattern:**

```java
public boolean filterEvent(Window window, int eventId) {
    switch (eventId) {
        case WindowEvent.WINDOW_OPENED: return true;
        default:                        return false;
    }
}
```

---

### `void handleWindow(Window window, int eventID)`

**Contract:** Perform whatever action is needed for the window and event. Called only
when `filterEvent` returned `true`. All calls run on the Swing event dispatch thread
via `GuiDeferredExecutor`, so the implementation can safely manipulate Swing
components. Any unchecked exception propagates to `Utils.exitWithException`.

**When called:** After `recogniseWindow` and `filterEvent` have both returned `true`
for this event.

---

## 2. Dispatch Order — How `TwsListener` Iterates the List

**File:** `src/ibcalpha/ibc/TwsListener.java`

`TwsListener` is registered as an `AWTEventListener` at startup:

```java
Toolkit.getDefaultToolkit().addAWTEventListener(
    new TwsListener(createWindowHandlers()),
    AWTEvent.WINDOW_EVENT_MASK);
```

For every window event, `TwsListener.eventDispatched` schedules this logic on the
Swing thread:

```java
for (WindowHandler wh : windowHandlers) {
    if (wh.recogniseWindow(window)) {
        logWindowStructure(window, eventID, true);
        if (wh.filterEvent(window, eventID)) wh.handleWindow(window, eventID);
        return;           // <-- dispatch stops at the first matching handler
    }
}
// no handler matched → log as unknown window
logWindowStructure(window, eventID, false);
```

**Key consequences:**

1. **First match wins.** The list is ordered; a handler registered earlier in
   `createWindowHandlers()` will always take precedence over a later one if both
   `recogniseWindow` implementations would return `true` for the same window.

2. **Order is critical for overlapping recognisers.** The clearest documented
   example in the code is the comment at line 325–328 of `IbcTws.java`:

   ```java
   // this line must come before the one for SecurityCodeDialogHandler
   // because both contain an "Enter Read Only" button
   windowHandlers.add(SecondFactorAuthenticationDialogHandler.getInstance());
   windowHandlers.add(new SecurityCodeDialogHandler());
   ```

   `SecondFactorAuthenticationDialogHandler` matches by title
   ("Second Factor Authentication" or "Security Code Card Authentication").
   `SecurityCodeDialogHandler` matches purely by the presence of the
   "Enter Read Only" button — a component that also appears in the 2FA dialog.
   If `SecurityCodeDialogHandler` were registered first, it would claim the 2FA
   dialog before the dedicated handler could see it, and TOTP entry would never
   happen.

3. **Unmatched windows** are logged at the scope/level determined by
   `LogStructureScope` / `LogStructureWhen` settings, which is the mechanism used
   to investigate unknown windows (see Section 6).

---

## 3. Handler Registry

Handlers are registered in `IbcTws.createWindowHandlers()` (lines 304–346) in the
order shown below. The `#` column reflects registration order and therefore dispatch
priority.

| #   | Handler class                                     | Window matched                                                                                                                                                                   | Trigger event(s)                                       | config.ini setting(s)                                                                                              |
| --- | ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| 1   | `AcceptIncomingConnectionDialogHandler`           | JDialog containing label "Accept incoming connection"                                                                                                                            | OPENED                                                 | `AcceptIncomingConnectionAction` (accept/reject/manual)                                                            |
| 2   | `BlindTradingWarningDialogHandler`                | JDialog with title containing "Order Preview" AND JTextPane containing "blind trading"; OR JDialog with labels "blind trading" AND "Are you sure you want to submit this order?" | OPENED                                                 | `AllowBlindTrading` (boolean, default false)                                                                       |
| 3   | `LoginFrameHandler`                               | JFrame titled "Login" or "New Login" with a "Log In" or "Paper Log In" button (or during restart)                                                                                | OPENED                                                 | `StoreSettingsOnServer` (boolean), `TradingMode` (via `TradingModeManager`)                                        |
| 4   | `GatewayLoginFrameHandler`                        | JFrame with title containing "IBKR Gateway", "IB Gateway", or "Interactive Brokers Gateway" with a login button                                                                  | OPENED                                                 | `FIXUserName`, `FIXPassword`, `IBAPIUserName`, `IBAPIPassword` (via `LoginManager`), `TradingMode`                 |
| 5   | `MainWindowFrameHandler`                          | JFrame with menu path File > Lock Application in any menu bar                                                                                                                    | OPENED                                                 | — (calls `SessionManager.setMainWindow`)                                                                           |
| 6   | `GatewayMainWindowFrameHandler`                   | JFrame with menu path Help > About Gateway OR Help > About IB Gateway in any menu bar                                                                                            | OPENED                                                 | — (calls `SessionManager.setMainWindow`)                                                                           |
| 7   | `NewerVersionDialogHandler`                       | JDialog with label containing "Newer Version" OR JOptionPane message containing "Newer Version"                                                                                  | OPENED, ACTIVATED                                      | — (auto-dismissed)                                                                                                 |
| 8   | `NewerVersionFrameHandler`                        | JFrame with label containing "Newer Version"                                                                                                                                     | OPENED, ACTIVATED                                      | — (auto-dismissed)                                                                                                 |
| 9   | `NotCurrentlyAvailableDialogHandler`              | JDialog with title containing "Login" AND label containing "not currently available"                                                                                             | OPENED, ACTIVATED                                      | — (auto-dismissed)                                                                                                 |
| 10  | `TipOfTheDayDialogHandler`                        | JDialog with title containing "Tip of the Day"                                                                                                                                   | OPENED, ACTIVATED                                      | — (auto-dismissed)                                                                                                 |
| 11  | `NSEComplianceFrameHandler`                       | JFrame with title containing "NSE Compliance"                                                                                                                                    | OPENED, ACTIVATED                                      | `DismissNSEComplianceNotice` (boolean, default true)                                                               |
| 12  | `PasswordExpiryWarningFrameHandler`               | JFrame with title containing "Password Notice"                                                                                                                                   | OPENED, ACTIVATED                                      | `DismissPasswordExpiryWarning` (boolean, default false)                                                            |
| 13  | `GlobalConfigurationDialogHandler`                | JDialog with title containing "Configuration"                                                                                                                                    | OPENED, CLOSED                                         | — (registers/unregisters dialog in `ConfigDialogManager`)                                                          |
| 14  | `TradesFrameHandler`                              | JFrame with title containing "Trades"                                                                                                                                            | OPENED, CLOSING, CLOSED                                | `ShowAllTrades` (boolean), `MinimizeMainWindow` (boolean)                                                          |
| 15  | `ExistingSessionDetectedDialogHandler`            | JDialog with title containing "Existing session detected"                                                                                                                        | OPENED                                                 | `ExistingSessionDetectedAction` (manual/primary/primaryoverride/secondary)                                         |
| 16  | `ApiChangeConfirmationDialogHandler`              | JDialog with label containing "apply the new socket port setting"                                                                                                                | OPENED (only when API change confirmation is expected) | — (internal `ConfigDialogManager` state)                                                                           |
| 17  | `SplashFrameHandler`                              | JFrame with title containing "Starting application..."                                                                                                                           | CLOSED                                                 | — (signals Gateway login complete via `LoginManager`)                                                              |
| 18  | `SecondFactorAuthenticationDialogHandler`         | JDialog or JFrame with title containing "Second Factor Authentication" or "Security Code Card Authentication"                                                                    | OPENED, CLOSED                                         | `Ib2FA` (TOTP secret), `SecondFactorDevice` (device name)                                                          |
| 19  | `SecurityCodeDialogHandler`                       | JDialog with an "Enter Read Only" button                                                                                                                                         | OPENED                                                 | `ReadOnlyLogin` (boolean)                                                                                          |
| 20  | `ReloginDialogHandler`                            | JDialog with title containing "Re-login is required"                                                                                                                             | OPENED                                                 | — (auto-clicks Re-login)                                                                                           |
| 21  | `NonBrokerageAccountDialogHandler`                | JDialog with label containing "This is not a brokerage account"                                                                                                                  | OPENED, CLOSED                                         | `AcceptNonBrokerageAccountWarning` (boolean, default true)                                                         |
| 22  | `ExitConfirmationDialogHandler`                   | JDialog with label "Are you sure you want to exit?"                                                                                                                              | OPENED                                                 | — (auto-clicks Yes only when `StopTask` is running)                                                                |
| 23  | `TradingLoginHandoffDialogHandler`                | JDialog with title containing "Trading Login Handoff"                                                                                                                            | OPENED                                                 | `ExistingSessionDetectedAction`                                                                                    |
| 24  | `LoginFailedDialogHandler`                        | JDialog with title containing "Login failed"                                                                                                                                     | OPENED                                                 | — (triggers cold restart)                                                                                          |
| 25  | `TooManyFailedLoginAttemptsDialogHandler`         | JDialog with a JTextArea containing "Too many failed login attempts"                                                                                                             | OPENED                                                 | `ReloginAfterSecondFactorAuthenticationTimeout` (boolean)                                                          |
| 26  | `ShutdownProgressDialogHandler`                   | JDialog with title containing "Shutdown progress"                                                                                                                                | OPENED                                                 | — (shuts down thread pools and command server)                                                                     |
| 27  | `BidAskLastSizeDisplayUpdateDialogHandler`        | JDialog with a JTextPane containing "Bid, Ask and Last Size Display Update"                                                                                                      | OPENED                                                 | `AcceptBidAskLastSizeDisplayUpdateNotification` (ignore/accept/defer), `SendMarketDataInLotsForUSstocks` (boolean) |
| 28  | `LoginErrorDialogHandler`                         | JDialog with title containing "Login Error"                                                                                                                                      | OPENED                                                 | — (triggers cold restart)                                                                                          |
| 29  | `CryptoOrderConfirmationDialogHandler`            | JDialog with title containing "Cryptocurrency order confirmation"                                                                                                                | OPENED                                                 | `ConfirmCryptoCurrencyOrders` (manual/transmit/cancel)                                                             |
| 30  | `AutoRestartConfirmationDialog`                   | (registered but source not listed above — see `IbcTws.java` line 340)                                                                                                            | —                                                      | —                                                                                                                  |
| 31  | `RestartConfirmationDialogHandler`                | JDialog with label "Are you sure you would like to restart the application"                                                                                                      | OPENED                                                 | — (auto-clicks Yes)                                                                                                |
| 32  | `ResetOrderIdConfirmationDialogHandler`           | JDialog with label "Are you sure you want to reset API order ID sequence?"                                                                                                       | OPENED                                                 | `ConfirmOrderIdReset` (confirm/reject/ignore format: `<atStart>/<userInitiated>`)                                  |
| 33  | `ReconnectDataOrAccountConfirmationDialogHandler` | JDialog with title containing "IBKR Trader Workstation" or "IBKR Gateway" AND label containing "Are you sure you want to execute \"simulate"                                     | OPENED                                                 | — (auto-clicks OK)                                                                                                 |
| 34  | `GatewayDialogHandler`                            | JDialog with title containing "Gateway"                                                                                                                                          | OPENED                                                 | — (logs text; triggers cold restart on "Connection to server failed")                                              |

**Note on `AbstractLoginHandler`:** `LoginFrameHandler` and `GatewayLoginFrameHandler`
both extend `AbstractLoginHandler`, which provides the shared `filterEvent` and
`handleWindow` implementations for login state management. The subclasses supply
`recogniseWindow`, `initialise`, `preLogin`, `setFields`, and credential-field-index
logic.

---

## 4. Recognition Techniques in `recogniseWindow()`

Three patterns are used throughout the codebase. They can be combined with `&&` when
a single signal is ambiguous.

### 4.1 Title Only

Match by the window's title string. Used when the title is globally unique.

```java
// TipOfTheDayDialogHandler — title is unique enough on its own
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JDialog)) return false;
    return SwingUtils.titleContains(window, "Tip of the Day");
}
```

```java
// SplashFrameHandler — exact substring match
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JFrame)) return false;
    return SwingUtils.titleContains(window, "Starting application...");
}
```

`SwingUtils.titleContains` is case-insensitive. `SwingUtils.titleEquals` performs a
full case-insensitive equality check.

### 4.2 Title + Component

Match by title first, then verify with a component probe to avoid false positives.
Used when the same title string can appear on different dialogs.

```java
// BlindTradingWarningDialogHandler — "Order Preview" dialog can appear
// for other reasons; we verify by looking for the specific text in a JTextPane
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JDialog)) return false;
    return ((SwingUtils.titleContains(window, "Order Preview")) &&
            SwingUtils.findTextPane(window, "blind trading") != null)
            ||
            ((SwingUtils.findLabel(window, "blind trading") != null &&
            SwingUtils.findLabel(window, "Are you sure you want to submit this order?") != null));
}
```

```java
// NotCurrentlyAvailableDialogHandler — title "Login" is shared with
// the reconnection info dialog; label disambiguates
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JDialog)) return false;
    return (SwingUtils.titleContains(window, "Login") &&
            SwingUtils.findLabel(window, "not currently available") != null);
}
```

### 4.3 Component Only (No Title)

Match purely by the presence of a characteristic component. Used when the window has
no reliable title.

```java
// AcceptIncomingConnectionDialogHandler — no title to rely on
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JDialog)) return false;
    return (SwingUtils.findLabel(window, "Accept incoming connection") != null);
}
```

```java
// SecurityCodeDialogHandler — identified solely by the "Enter Read Only" button
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JDialog)) return false;
    return (SwingUtils.findButton(window, "Enter Read Only") != null);
}
```

```java
// MainWindowFrameHandler — identified by a specific menu path
public boolean recogniseWindow(Window window) {
    if (! (window instanceof JFrame)) return false;
    return (SwingUtils.findMenuItemInAnyMenuBar(
                window, new String[]{"File", "Lock Application"}) != null);
}
```

---

## 5. `SwingUtils` — Available Methods

**File:** `src/ibcalpha/ibc/SwingUtils.java`

All methods are `static`. The `ComponentIterator` used internally performs a
depth-first traversal of the component tree, taking the `TreeLock` at each node.

### Component Finders

| Signature                                                                | Description                                                                                           |
| ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| `JButton findButton(Container container, String text)`                   | Returns the first `JButton` whose label matches `text` (case-insensitive).                            |
| `JCheckBox findCheckBox(Container container, String text)`               | Returns the first `JCheckBox` with the given label (case-insensitive).                                |
| `JComboBox<?> findComboBox(Container container, int ith)`                | Returns the `ith` `JComboBox` (0-based).                                                              |
| `Component findComponent(Container container, String text)`              | Returns the component whose `getName()` equals `text` (case-insensitive).                             |
| `JRadioButton findRadioButton(Container container, String text)`         | Returns the first `JRadioButton` with the given label (case-insensitive).                             |
| `JLabel findLabel(Container container, String text)`                     | Returns the first `JLabel` whose text **contains** `text` (case-insensitive).                         |
| `JList<?> findList(Container container, int ith)`                        | Returns the `ith` `JList` (0-based).                                                                  |
| `JTextArea findTextArea(Container container, String text)`               | Returns the first `JTextArea` whose content **contains** `text` (case-insensitive).                   |
| `JTextField findTextField(Container container, int ith)`                 | Returns the `ith` `JTextField` (0-based). Note: `JPasswordField` extends `JTextField` and is counted. |
| `JTextPane findTextPane(Container container, String text)`               | Returns the first `JTextPane` whose content **contains** `text` (case-insensitive).                   |
| `JToggleButton findToggleButton(Container container, String text)`       | Returns the first `JToggleButton` with the given label (case-insensitive).                            |
| `JMenuBar findMenuBar(Container container)`                              | Returns the first `JMenuBar` found in the hierarchy.                                                  |
| `JMenuBar findMenuBar(Container container, int ith)`                     | Returns the `ith` `JMenuBar` (0-based).                                                               |
| `JMenuItem findMenuItem(MenuElement container, String text)`             | Recursively searches sub-elements for the first `JMenuItem` matching `text`.                          |
| `JMenuItem findMenuItem(Container container, String[] path)`             | Finds a `JMenuItem` by path from the **first** `JMenuBar` in the container.                           |
| `JMenuItem findMenuItemInAnyMenuBar(Container container, String[] path)` | Finds a `JMenuItem` by path, trying every `JMenuBar` in the container until found.                    |
| `JOptionPane findOptionPane(Container container)`                        | Returns the first `JOptionPane`.                                                                      |
| `JTree findTree(Container container)`                                    | Returns the first `JTree`.                                                                            |
| `Object findChildNode(TreeModel model, Object node, String text)`        | Finds a child node in a `TreeModel` by `toString()` value (case-insensitive).                         |

### Click and State Manipulation

| Signature                                                                      | Description                                                                                                          |
| ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------- |
| `boolean clickButton(Window window, String buttonText)`                        | Finds the button by label and calls `doClick()`. Returns `false` if not found. Enables the button first if disabled. |
| `void clickButton(JButton button)`                                             | Enables and clicks a `JButton` directly.                                                                             |
| `boolean isButtonEnabled(Window window, String buttonText)`                    | Returns `true` if the named button exists and is enabled.                                                            |
| `boolean isCheckBoxSelected(Window window, String buttonText)`                 | Returns `true` if the named checkbox exists and is selected.                                                         |
| `boolean isRadioButtonSelected(Window window, String buttonText)`              | Returns `true` if the named radio button exists and is selected.                                                     |
| `boolean setCheckBoxSelected(Window window, String buttonText, boolean value)` | Sets the selection state of the named checkbox. Returns `false` if not found.                                        |
| `boolean setRadioButtonSelected(Window window, String buttonText)`             | Clicks the named radio button if not already selected. Returns `false` if not found.                                 |
| `boolean setTextField(Window window, int fieldNumber, String value)`           | Sets the `ith` `JTextField` value. Returns `false` if not found.                                                     |

### Title Inspection

| Signature                                           | Description                                                                               |
| --------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `String getWindowTitle(Window window)`              | Returns the window title for `JFrame` or `JDialog`; returns `"** no title **"` otherwise. |
| `boolean titleContains(Window window, String text)` | Case-insensitive substring check on the window title.                                     |
| `boolean titleEquals(Window window, String text)`   | Case-insensitive equality check on the window title.                                      |

### Text Extraction

| Signature                                   | Description                                                                                              |
| ------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `String getLabelTexts(Container container)` | Concatenates all non-null `JLabel` texts found in the hierarchy, separated by the system line separator. |
| `String getTexts(Container container)`      | Concatenates all non-null `JTextArea` texts found in the hierarchy.                                      |

### Diagnostics

| Signature                                  | Description                                                                                                                                   |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------- |
| `String getWindowStructure(Window window)` | Returns a hierarchical string representation of all components in the window, suitable for logging. Used by `TwsListener.logWindowStructure`. |
| `String windowEventToString(int eventID)`  | Converts a `WindowEvent` ID to a human-readable string ("Opened", "Closed", etc.).                                                            |

---

## 6. Identifying an Unknown Window

When IBC encounters a window it does not recognise, it logs `"detected ... ; event=..."`
but takes no action. The `LogStructureScope` and `LogStructureWhen` settings in
`config.ini` control whether the full component tree is also logged.

### Step 1 — Enable full structure logging

In `config.ini`, add:

```ini
LogStructureScope=unknown
LogStructureWhen=open
```

- `LogStructureScope=unknown` logs the structure only for windows that no handler
  recognised. Values: `known`, `unknown`, `untitled`, `all`.
- `LogStructureWhen=open` logs on `WINDOW_OPENED`. Values: `open`, `activate`,
  `openclose`, `never`, or any event name returned by `windowEventToString`.

Restart IBC and trigger the unknown window. The log will contain a block like:

```
detected dialog entitled: Some Dialog Title; event=Opened
Window structure for dialog entitled: Some Dialog Title; event=Opened

null(javax.swing.JRootPane){...}
|   null(javax.swing.JPanel){...}
|   |   null(javax.swing.JPanel){...}
|   |   |   null(javax.swing.JPanel){...}
|   |   |   |   JButton: OK
|   |   |   |   JLabel: Are you sure you want to proceed?
```

### Step 2 — Read the structure

From the log output, identify:

- **Window type:** `frame entitled:` → `JFrame`; `dialog entitled:` → `JDialog`.
- **Window title:** the string after `entitled:`.
- **Components:** all `JButton`, `JLabel`, `JTextField`, `JTextArea`, `JTextPane`,
  `JCheckBox`, etc., with their text values.

### Step 3 — Choose a recognition strategy

- If the title is unique and stable, use `titleContains` or `titleEquals`.
- If the title is shared with another window, add a component check with
  `findLabel` / `findTextPane` / `findButton`.
- If there is no title, identify a component that is unique to this window.

---

## 7. Adding a New Handler

### 7.1 Java template

```java
package ibcalpha.ibc;

import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog; // or JFrame

class MyNewDialogHandler implements WindowHandler {

    @Override
    public boolean recogniseWindow(Window window) {
        // 1. Check the Java type first — fast path to avoid scanning
        if (!(window instanceof JDialog)) return false;

        // 2. Identify by title (if unique)
        if (!SwingUtils.titleContains(window, "My Dialog Title")) return false;

        // 3. Optionally confirm with a component (if title alone is ambiguous)
        // return SwingUtils.findLabel(window, "some distinctive text") != null;

        return true;
    }

    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            // Add WindowEvent.WINDOW_CLOSED if you also need to react to closing
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(Window window, int eventID) {
        // Read the relevant config.ini setting
        String action = Settings.settings().getString("MyNewSetting", "default");

        if (action.equalsIgnoreCase("auto")) {
            if (!SwingUtils.clickButton(window, "OK")) {
                Utils.logError("MyNewDialogHandler: could not find OK button");
            }
        }
        // For "manual" or unknown values, do nothing and let the user interact
    }
}
```

### 7.2 Registration in `IbcTws.createWindowHandlers()`

**File:** `src/ibcalpha/ibc/IbcTws.java`, method `createWindowHandlers()`, around
line 304.

```java
private static List<WindowHandler> createWindowHandlers() {
    List<WindowHandler> windowHandlers = new ArrayList<>();

    // ... existing handlers ...

    // Insert your handler at the correct position.
    // If your recogniseWindow could match a window already handled
    // by an existing handler, place it BEFORE that handler.
    // If your handler is self-contained and shares no recognition
    // criteria with others, append it at the end (before GatewayDialogHandler,
    // which acts as a catch-all for any JDialog titled "Gateway").
    windowHandlers.add(new MyNewDialogHandler());

    windowHandlers.add(new GatewayDialogHandler()); // keep last (catch-all)
    return windowHandlers;
}
```

**Registration rules:**

1. Place the new handler before any existing handler whose `recogniseWindow` might
   also match the same window — or after it if the existing handler should take
   priority.
2. `GatewayDialogHandler` (matches any `JDialog` titled "Gateway") should remain
   the last entry because it is a low-specificity catch-all.
3. The `SecondFactorAuthenticationDialogHandler` / `SecurityCodeDialogHandler` pair
   must always stay in that order (see Section 2).
4. Add a unit test or integration note if the new window is associated with a
   config.ini setting, so that future regressions can be caught early.

### 7.3 Checklist

- [ ] `recogniseWindow` returns `false` for all windows it should not handle
- [ ] `filterEvent` returns `false` for events the handler does not need to act on
- [ ] `handleWindow` uses `Settings.settings().getString/getBoolean` to read
      config.ini values, with sensible defaults
- [ ] All error paths call `Utils.logError` (non-fatal) or
      `Utils.exitWithError` / `Utils.exitWithException` (fatal)
- [ ] The handler is added to `createWindowHandlers()` in the correct position
- [ ] `LogStructureScope` / `LogStructureWhen` settings are reset to avoid
      verbose output in production
