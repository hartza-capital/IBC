# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build System

**Prerequisites:** Java 17 JDK, Maven 3.8+, `IBC_BIN` env var pointing to the TWS/Gateway `jars/` directory (e.g. `~/Jts/1045/jars`).

```bash
export IBC_BIN=~/Jts/1045/jars   # required for all builds

make jar      # compile + produce resources/IBC.jar (no distribution ZIP)
make dist     # compile + resources/IBC.jar + dist/IBCLinux-x.y.z.zip
make clean    # remove build artifacts (target/, dist/, resources/IBC.jar)
make rebuild  # clean + dist
make format   # run prettier on *.md and *.json
```

Direct Maven equivalents: `mvn compile`, `mvn package -DskipAssembly=true`, `mvn clean`.

**Build output:** `resources/IBC.jar` — fat JAR with all dependencies (including `dev.samstevens.totp`).

**Non-standard Maven layout:** sources live in `src/ibcalpha/ibc/` (not `src/main/java`), tests in `test/`. The default `maven-compiler-plugin` is disabled; compilation is done via `maven-antrun-plugin` to include TWS/Gateway jars from `$IBC_BIN` on the compile classpath.

**Generated source:** `IbcVersionInfo.java` is generated during `generate-sources` phase into `target/generated-sources/java/ibcalpha/ibc/`. Never edit it manually.

## Architecture

IBC is a GUI automation agent for IB TWS and IB Gateway. It hooks into the Swing event system to intercept window events, then dispatches to handlers that programmatically interact with dialogs.

### Entry Points

- `IbcTws` — main class for TWS; loads config, sets up `TwsListener`, starts `CommandServer`
- `IbcGateway` — main class for IB Gateway; same pattern, Gateway-specific window handlers

### Core Event Loop

`TwsListener` is an `AWTEventListener` registered on the Swing Toolkit. Every `WINDOW_OPENED` / `WINDOW_ACTIVATED` / etc. event flows through it. For each event it iterates a list of `WindowHandler` instances, calling `recogniseWindow()` then `handleWindow()` on the first match. All handler execution is posted via `GuiDeferredExecutor` to avoid blocking the EDT.

### WindowHandler Pattern

All dialog and frame handlers implement `WindowHandler`:

```java
interface WindowHandler {
    boolean recogniseWindow(Window window);  // identify by title/component structure
    boolean filterEvent(Window window, int eventId);  // which event IDs to act on
    void handleWindow(Window window, int eventID);    // perform the automation
}
```

`SwingUtils` provides helpers to locate buttons and labels by text within a window. `ComponentIterator` walks the Swing component tree.

### Key Singletons (all use static factory pattern)

| Class             | Role                                                                                   |
| ----------------- | -------------------------------------------------------------------------------------- |
| `Settings`        | Config abstraction backed by `config.ini`                                              |
| `DefaultSettings` | `.ini` file parser implementation                                                      |
| `LoginManager`    | Login state machine (`LOGGED_OUT` → `LOGGING_IN` → `TWO_FA_IN_PROGRESS` → `LOGGED_IN`) |
| `SessionManager`  | Session lifecycle, daily auto-restart scheduling                                       |
| `CommandServer`   | TCP server for remote commands (`STOP`, `RESTART`, `RECONNECTDATA`, etc.)              |

### Configuration Tasks (`Configure*Task`)

These run after login to configure TWS/Gateway programmatically by opening the Global Configuration dialog and setting values. Each implements `ConfigurationTask` and is dispatched via `ConfigDialogManager` / `GetConfigDialogTask`.

### Two-Factor Authentication

`SecondFactorAuthenticationDialogHandler` handles the 2FA flow. When `Ib2FA` is a base32 string, it calls into `dev.samstevens.totp` to generate a TOTP code automatically. When it's `IBKR Mobile`, it waits for the push notification and can retry on timeout.

### GUI Execution Utilities

- `GuiDeferredExecutor` — posts work to the Swing EDT via `SwingUtilities.invokeLater`
- `GuiSynchronousExecutor` — runs on EDT synchronously (blocks caller)
- `MyCachedThreadPool` / `MyScheduledExecutorService` — thread pool wrappers with uncaught exception logging

### Command Server

`CommandServer` (TCP, port from `CommandServerPort` setting) accepts one connection at a time. `CommandDispatcher` maps command strings to actions. `CommandChannel` handles per-connection I/O.

### JTS INI Management

`JtsIniManager` / `JtsIniSectionSetting` read and write the TWS `jts.ini` file to persist configuration changes (e.g. trading mode, API port) across sessions.

## Adding a New Dialog Handler

1. Create `MyDialogHandler.java` implementing `WindowHandler`
2. Implement `recogniseWindow()` using `SwingUtils` to match the dialog title or a unique component
3. Implement `filterEvent()` — typically accept only `WINDOW_OPENED`
4. Implement `handleWindow()` — interact via `SwingUtils.clickButton()` etc.
5. Register the instance in the handler list built in `IbcTws` or `IbcGateway`

## Key Configuration Settings (config.ini)

| Setting                          | Purpose                                        |
| -------------------------------- | ---------------------------------------------- |
| `IbLoginId` / `IbPassword`       | IBKR credentials                               |
| `TradingMode`                    | `paper` or `live`                              |
| `Ib2FA`                          | `none`, `IBKR Mobile`, or a base32 TOTP secret |
| `CommandServerPort`              | TCP port (`0` = disabled)                      |
| `AutoRestartTime`                | Daily auto-restart time (no re-auth)           |
| `ClosedownAt`                    | Scheduled shutdown time                        |
| `AcceptIncomingConnectionAction` | `accept`, `reject`, `manual`                   |
| `OverrideTwsApiPort`             | Force API port regardless of TWS config        |

## Documentation Index

Project documentation generated by `app-documenter`.

### Directory Structure

```text
IBC/
├── README.md                          ← End-user installation & usage guide
├── CONTRIBUTING.md                    ← Contributor guide
├── src/
│   └── ibcalpha/ibc/
│       └── README.md                  ← Developer package reference (81 classes)
└── docs/
    ├── ARCHITECTURE.md                ← System architecture overview
    ├── COMMAND_SERVER.md              ← TCP command server protocol
    ├── CONFIGURATION_REFERENCE.md    ← Full config.ini reference
    ├── DEBUGGING.md                   ← Debugging and diagnostics guide
    ├── LOGIN_FLOW.md                  ← Login and 2FA flow documentation
    ├── SHELL_SCRIPTS.md               ← Startup scripts reference
    ├── STARTUP_SEQUENCE.md            ← Startup sequence walkthrough
    ├── THREADING_MODEL.md             ← Threading model and EDT patterns
    └── WINDOW_HANDLERS.md             ← Window handler catalog
```

### Modules

| Module         | Path                                                       | Language | Description                                                                                                                                    |
| -------------- | ---------------------------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `ibcalpha.ibc` | [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md) | Java     | Core IBC package — 81 classes covering event dispatch, dialog handlers, login/2FA, configuration tasks, command server, and session management |

### By Category

#### Getting Started

- [`README.md`](README.md) — End-user guide: installation, configuration, startup scripts
- [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md) — Developer guide: architecture, subsystems, extending IBC

#### Deep-Dive Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — System architecture and design decisions
- [`docs/LOGIN_FLOW.md`](docs/LOGIN_FLOW.md) — Login state machine and 2FA handling
- [`docs/WINDOW_HANDLERS.md`](docs/WINDOW_HANDLERS.md) — Catalog of all dialog/frame handlers
- [`docs/THREADING_MODEL.md`](docs/THREADING_MODEL.md) — EDT, thread pools, concurrency model
- [`docs/STARTUP_SEQUENCE.md`](docs/STARTUP_SEQUENCE.md) — Step-by-step startup walkthrough
- [`docs/COMMAND_SERVER.md`](docs/COMMAND_SERVER.md) — TCP command protocol and client usage
- [`docs/CONFIGURATION_REFERENCE.md`](docs/CONFIGURATION_REFERENCE.md) — All `config.ini` settings
- [`docs/DEBUGGING.md`](docs/DEBUGGING.md) — Diagnostics, logging, and troubleshooting
- [`docs/SHELL_SCRIPTS.md`](docs/SHELL_SCRIPTS.md) — `twsstart.sh` / `gatewaystart.sh` reference
