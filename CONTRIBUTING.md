# Contributing to IBC

IBC is a 100% Java project. It automates TWS and IB Gateway by listening to AWT window events and dispatching each window to a dedicated `WindowHandler`. This guide covers everything you need to add a handler, test it, and submit a pull request.

---

## Prerequisites

- **Java 25 JDK** — `javac --version` must report `javac 25.x`
- **Maven 3.9+** — `./mvnw --version` (the wrapper pins the exact version used by CI)
- **TWS or IB Gateway installed offline** — IBC compiles against their JAR files
- **`IBC_BIN` environment variable** — must point to the directory that contains the TWS/Gateway JARs (e.g., the `jars/` folder inside the TWS installation)

```bash
export IBC_BIN=/opt/ibgateway/1019/jars   # adjust to your installation
```

The build enforces this variable at the `validate` phase; Maven will fail immediately if it is not set.

---

## Local build

```bash
# Fast build — produces resources/IBC.jar, skips the Linux distribution ZIP
make dev

# Equivalent Maven command (same result, via the wrapper)
./mvnw -q package -DskipAssembly=true

# Full build — also produces dist/IBCLinux-<version>.zip
make dist

# Clean all build outputs (target/, dist/, resources/IBC.jar, resources/version)
make clean

# Clean then full rebuild
make rebuild
```

The JAR is written to `resources/IBC.jar` and is the only artifact IBC needs at runtime. It contains just IBC's own classes — there are no external runtime dependencies to bundle.

---

## How TWS window handling works

`IbcTws.createWindowHandlers()` registers a list of `WindowHandler` instances with `TwsListener`. `TwsListener` is an `AWTEventListener` that receives every AWT window event. For each event it iterates the handler list and calls:

1. `recogniseWindow(window)` — returns `true` if this handler owns the window.
2. `filterEvent(window, eventId)` — returns `true` if this event type should be acted on.
3. `handleWindow(window, eventId)` — performs the actual automation (click a button, fill a field, etc.).

The first handler whose `recogniseWindow` returns `true` wins; the others are skipped.

---

## Identifying a new TWS window

Before writing a handler you need to know the window's title and component structure. Enable structure logging in `config.ini`:

```ini
LogStructureScope=all
LogStructureWhen=open
```

- `LogStructureScope=all` logs every window IBC sees, including ones not yet handled.
- `LogStructureWhen=open` dumps the component tree when the window first opens.

Run IBC against a paper account (see below). The IBC log will print lines like:

```
detected dialog entitled: Some New Dialog; event=opened
Window structure for dialog entitled: Some New Dialog; event=opened
  JDialog [title=Some New Dialog]
    JPanel
      JLabel [text=Some message text]
      JButton [text=OK]
```

Use the title string and button/label texts to write your `recogniseWindow` and `handleWindow` implementations.

---

## Adding a WindowHandler

### 1. Create the handler class

Create `src/ibcalpha/ibc/MyNewDialogHandler.java`. Use this template:

```java
package ibcalpha.ibc;

import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;

class MyNewDialogHandler implements WindowHandler {

    @Override
    public boolean recogniseWindow(Window window) {
        if (!(window instanceof JDialog)) return false;
        return SwingUtils.titleContains(window, "Exact Or Partial Title Here");
    }

    @Override
    public boolean filterEvent(Window window, int eventId) {
        return eventId == WindowEvent.WINDOW_OPENED
            || eventId == WindowEvent.WINDOW_ACTIVATED;
    }

    @Override
    public void handleWindow(Window window, int eventId) {
        if (!SwingUtils.clickButton(window, "OK")) {
            Utils.logError("MyNewDialogHandler: could not find the OK button.");
        }
    }
}
```

Tips:

- `SwingUtils.titleContains(window, text)` — partial, case-sensitive match on the window title.
- `SwingUtils.clickButton(window, label)` — finds and clicks a `JButton` by its text label.
- `SwingUtils.findButton(window, label)` — returns the `JButton` without clicking it.
- Use `JFrame` instead of `JDialog` in `recogniseWindow` for top-level frames.
- Log errors with `Utils.logError(...)`, not `System.err`.

### 2. Register the handler

Open `src/ibcalpha/ibc/IbcTws.java` and add your handler to `createWindowHandlers()`:

```java
private static List<WindowHandler> createWindowHandlers() {
    List<WindowHandler> windowHandlers = new ArrayList<>();
    // ... existing handlers ...
    windowHandlers.add(new MyNewDialogHandler());  // <-- add here
    return windowHandlers;
}
```

Order matters only when two handlers could both match the same window. Read the comment above `SecondFactorAuthenticationDialogHandler` for an example of ordering that is intentional.

### 3. Build and verify

```bash
make dev
```

A clean compilation with no warnings is expected (`-Xlint:all` is active).

---

## Manual testing

IBC must be tested against a live TWS or Gateway process. Use a **paper trading account** to avoid any risk.

1. Install TWS or IB Gateway offline (so IBC can launch it).
2. Set `IBC_BIN` to point to its JARs and build IBC (`make dev`).
3. Edit `resources/config.ini`:
   - Set `IbLoginId` and `IbPassword` (paper account credentials).
   - Set `TradingMode=paper`.
   - During handler development, set `LogStructureScope=all` and `LogStructureWhen=open`.
4. Launch IBC with the config file:

   ```bash
   java -cp resources/IBC.jar ibcalpha.ibc.IbcTws resources/config.ini "" "" paper
   ```

5. Watch the console output. Confirm your handler fires (`detected dialog entitled: ...`) and that `handleWindow` completes without logging an error.

---

## Pull request checklist

Include in your PR description:

- **What dialog or window** the handler targets (title, TWS version where you observed it).
- **What action** the handler takes (which button it clicks, what it fills in).
- **config.ini setting** that controls the behavior, if you added one.
- **Manual test result** — confirm you ran IBC against a paper account and observed the handler firing correctly.

Make sure before opening the PR:

- [ ] `make dev` compiles with no errors or warnings.
- [ ] The new handler is registered in `IbcTws.createWindowHandlers()`.
- [ ] Manual test was performed against a paper trading account.
- [ ] `resources/config.ini` documents any new setting you added.
