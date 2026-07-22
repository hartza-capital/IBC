---
name: java-expert-ibc
description: Knowledge about the IBC Java codebase and testing approach for non-IBKR code.
metadata:
  type: project
---

IBC has 82 Java classes in `src/ibcalpha/ibc/`. None imports IBKR/TWS packages directly; the IBKR dependency is functional (runtime GUI automation), not compile-time.

**Why:** This means far more code is unit-testable than it first appears. The main blockers for testing are static singletons (`Settings`, `SessionManager`, `LoginManager`, `TradingModeManager`, `MainWindowManager`) and `Utils.exitWithError` calling `System.exit`.

**How to apply:**

- Use the existing `initialise(...)` / `setDefault()` methods on singletons with test doubles.
- Use Mockito inline to stub `Utils.exitWithError(...)` for error-path tests.
- Run tests with `-Djava.awt.headless=true`; avoid constructing real `JFrame`/`JDialog` unless necessary.
- `CommandDispatcher` touches `Toolkit.getMenuShortcutKeyMaskEx()` for reconnect keyboard shortcuts; make such initialisation lazy so tests can load the class headlessly.
