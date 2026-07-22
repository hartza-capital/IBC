---
name: ibc-testability
description: Most ibcalpha.ibc classes are unit-testable; the real blockers are static singletons and System.exit, not the IBKR dependency.
metadata:
  type: project
---

Most classes in `src/ibcalpha/ibc/` are plain unit-testable Java. The real obstacles to testing are static singletons (`Settings`, `SessionManager`, `LoginManager`, `TradingModeManager`, `MainWindowManager`) and `Utils.exitWithError` calling `System.exit`.

**Why:** IBC looks untestable at first glance because it is GUI automation for a third-party app, so the instinct is to write only IBKR-driven integration checks. In practice only the handful of window handlers that drive real TWS dialogs need a live Gateway; everything else runs headless. Note this is *not* because the codebase is IBKR-free at compile time — see [[ibc-build-requirements]].

**How to apply:**

- Use the existing `initialise(...)` / `setDefault()` methods on singletons with test doubles.
- Use Mockito inline to stub `Utils.exitWithError(...)` for error-path tests.
- Run tests with `-Djava.awt.headless=true`; avoid constructing real `JFrame`/`JDialog` unless necessary.
- `CommandDispatcher` touches `Toolkit.getMenuShortcutKeyMaskEx()` for reconnect keyboard shortcuts; keep such initialisation lazy so the class loads headlessly in tests.
