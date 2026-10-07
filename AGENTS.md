# AGENTS.md

Guidance for coding agents working in this repository.

IBC is a GUI automation agent for IB TWS and IB Gateway: it hooks into the Swing
event system, intercepts window events, and drives dialogs programmatically. This
is the **hartza-capital fork, which supports Linux only** — Windows and macOS
resources, the Ant/Ivy build and the samples were removed in favour of Maven and a
Linux-only distribution. `origin` is this fork (default branch `develop`),
`upstream` is `IbcAlpha/IBC` (retired 1 Sep 2026, archived read-only).

## Build System

**Prerequisites:** Java 25 JDK, Maven 3.9+, `IBC_BIN` env var pointing to the TWS/Gateway `jars/` directory (e.g. `~/Jts/1050/jars`). Use `./mvnw` — the Maven wrapper pins the version CI uses — rather than a system `mvn`.

```bash
export IBC_BIN=~/Jts/1050/jars   # required for all builds

make dev      # compile + produce resources/IBC.jar (no distribution ZIP)
make dist     # compile + resources/IBC.jar + dist/IBCLinux-x.y.z.zip
make clean    # remove build artifacts (target/, dist/, resources/IBC.jar, resources/version)
make rebuild  # clean + dist
make format   # run prettier --write on **/*.md and **/*.json
make check-env  # fail fast if IBC_BIN is unset
```

`make dev` and `make jar` are the same target (`package -DskipAssembly=true`).
Tests, lint and static analysis have **no Makefile target** — run them through Maven:

```bash
./mvnw -B test                # JUnit 5 suite in test/ibcalpha/ibc/ (18 files, 97 @Test)
./mvnw -B checkstyle:check    # same ruleset reviewdog annotates on PRs
./mvnw -B spotbugs:check      # advisory only (failOnError=false)
./mvnw -B verify              # test + jacoco + jar + dist ZIP + spotbugs (checkstyle is not bound to any phase)
```

lefthook's pre-commit (`lefthook install`) runs `./mvnw -q checkstyle:check`, `./mvnw -q -B test` and `prettier --check` on staged files.

Direct Maven equivalents, via the wrapper: `./mvnw compile`, `./mvnw package -DskipAssembly=true`, `./mvnw clean`.

**Build output:** `resources/IBC.jar` — plain JAR of IBC's own classes. IBC has **no compile-scope dependencies**; anything added to `<dependencies>` in `pom.xml` outside test scope ends up shipped inside this JAR.

**Non-standard Maven layout:** sources live in `src/ibcalpha/ibc/` (not `src/main/java`), tests in `test/`. The default `maven-compiler-plugin` is disabled; compilation is done via `maven-antrun-plugin` to include TWS/Gateway jars from `$IBC_BIN` on the compile classpath. `-Xlint:all` is active — a clean build produces no warnings.

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

| Class            | Role                                                                                   |
| ---------------- | -------------------------------------------------------------------------------------- |
| `Settings`       | Config abstraction backed by `config.ini`; `DefaultSettings` is the `.ini` parser      |
| `LoginManager`   | Login state machine (`LOGGED_OUT` → `LOGGING_IN` → `TWO_FA_IN_PROGRESS` → `LOGGED_IN`) |
| `SessionManager` | Session lifecycle, daily auto-restart scheduling                                       |
| `CommandServer`  | TCP server for remote commands (`STOP`, `RESTART`, `RECONNECTDATA`, etc.)              |

Interfaces (`Settings`, `LoginManager`, `SessionManager`, `MainWindowManager`, `TradingModeManager`) have `Default*` implementations and `Stub*` doubles under `test/` so behaviour can be tested without a live TWS.

### Configuration Tasks (`Configure*Task`)

These run after login to configure TWS/Gateway programmatically by opening the Global Configuration dialog and setting values. Each implements `ConfigurationTask` and is dispatched via `ConfigDialogManager` / `GetConfigDialogTask`.

### Two-Factor Authentication

`SecondFactorAuthenticationDialogHandler` handles the 2FA flow. When `Ib2FA` is a base32 string, it generates a TOTP code automatically via the in-tree `Totp`/`Base32` classes (RFC 4226/4648/6238 over the JDK's HMAC-SHA1 — no external library). Any non-empty value is decoded as base32; there is **no** special-casing of `none` or `IBKR Mobile`. Leave `Ib2FA` empty to keep the dialog open for a push notification or manual code.

### GUI Execution Utilities

- `GuiDeferredExecutor` — posts work to the Swing EDT via `SwingUtilities.invokeLater`
- `GuiSynchronousExecutor` — runs on EDT synchronously (blocks caller)
- `MyCachedThreadPool` / `MyScheduledExecutorService` — thread pool wrappers with uncaught exception logging

### Command Server

`CommandServer` (TCP, port from `CommandServerPort` setting) accepts one connection at a time. `CommandDispatcher` maps command strings to actions. `CommandChannel` handles per-connection I/O.

### JTS INI Management

`JtsIniManager` / `JtsIniSectionSetting` read and write the TWS `jts.ini` file to persist configuration changes (e.g. trading mode, API port) across sessions.

## Conventions

- Commit messages follow Conventional Commits: `<type>(<scope>): <subject>` (`feat`, `fix`, `docs`, `style`, `test`, `chore`, `build`); squash-merged PR titles end with `(#<issue>)`.
- Production code lives in `src/ibcalpha/ibc/`, package `ibcalpha.ibc`; new classes are declared `public`/package-private deliberately, and handlers are singletons exposing `getInstance()`.
- **Checkstyle applies to `src/` only** — all rules are suppressed for `test/` (see `checkstyle-suppressions.xml`).
- Markdown and JSON are formatted with prettier (printWidth 80, `proseWrap: preserve`); run `make format` before committing docs, or the pre-commit hook fails.
- Fork-added and fork-touched methods carry Javadoc (see `Totp`, `Base32`, `SecondFactorAuthenticationDialogHandler`).
- CI (`.github/workflows/test-java.yml`) runs checkstyle, `./mvnw -B test`, reviewdog on PRs, and octocov, which requires **80% line coverage** (`.octocov.yml`).

## Pitfalls

- **`IBC_BIN` must point at a directory containing the TWS/Gateway jars**, and this workstation has **no JDK 25** (`java` is Homebrew openjdk 27, and can be absent) and no local `jars/` directory. Build and test in a container instead, mounting the Gateway jars at `/w/jars`; the wrapper downloads Maven, but the image still has to supply JDK 25, which none available on the host does: `docker run --rm -v "$PWD":/w -w /w -e IBC_BIN=/w/jars eclipse-temurin:25-jdk ./mvnw -B test`.
- `resources/IBC.jar` and `resources/version` are **build outputs**, gitignored and removed by `make clean`. Never commit them.
- `IbcVersionInfo.java` is generated — edit `<version>` in `pom.xml`, not the file.
- Upstream-derived files keep their **CRLF** line endings (e.g. `SecondFactorAuthenticationDialogHandler.java`) while newer ones are LF. Don't renormalize endings; keep diffs minimal.
- `resources/config.ini` ships `Ib2FA=XXXXXXXXXX` as a placeholder. Because any non-empty value is base32-decoded, leaving it makes IBC submit a wrong code — set a real secret or leave the value empty.
- The PR template's checklist (`make lint`, `make test`, `make format-check`) names targets that **do not exist**; use the Maven commands above.
- Tests run headless (`java.awt.headless=true` with `--add-opens` flags in the surefire config); Swing tests must not require a display.

## Adding a New Dialog Handler

1. Create `MyDialogHandler.java` in `src/ibcalpha/ibc/` implementing `WindowHandler`
2. Implement `recogniseWindow()` using `SwingUtils` to match the dialog title or a unique component
3. Implement `filterEvent()` — typically accept only `WINDOW_OPENED`
4. Implement `handleWindow()` — interact via `SwingUtils.clickButton()` etc.
5. Register the instance in the handler list built in `IbcTws` or `IbcGateway`

`CONTRIBUTING.md` documents the workflow in full, including how to capture a dialog's component tree with `LogStructureScope` / `LogStructureWhen`.

## Key Configuration Settings (config.ini)

| Setting                          | Purpose                                      |
| -------------------------------- | -------------------------------------------- |
| `IbLoginId` / `IbPassword`       | IBKR credentials                             |
| `TradingMode`                    | `paper` or `live`                            |
| `Ib2FA`                          | Base32 TOTP secret (empty = manual/push 2FA) |
| `SecondFactorDevice`             | Device name to pick in the 2FA device list   |
| `CommandServerPort`              | TCP port (`0` = disabled)                    |
| `AutoRestartTime`                | Daily auto-restart time (no re-auth)         |
| `ClosedownAt`                    | Scheduled shutdown time                      |
| `AcceptIncomingConnectionAction` | `accept`, `reject`, `manual`                 |
| `OverrideTwsApiPort`             | Force API port regardless of TWS config      |

## Documentation Index

| Path                                                                 | Contents                                                     |
| -------------------------------------------------------------------- | ------------------------------------------------------------ |
| [`README.md`](README.md)                                             | End-user guide: installation, configuration, startup scripts |
| [`CONTRIBUTING.md`](CONTRIBUTING.md)                                 | Contributor guide: build, handler pattern, PR checklist      |
| [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md)           | Developer package reference (83 classes)                     |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)                       | System architecture and design decisions                     |
| [`docs/LOGIN_FLOW.md`](docs/LOGIN_FLOW.md)                           | Login state machine and 2FA handling                         |
| [`docs/WINDOW_HANDLERS.md`](docs/WINDOW_HANDLERS.md)                 | Catalog of all dialog/frame handlers                         |
| [`docs/THREADING_MODEL.md`](docs/THREADING_MODEL.md)                 | EDT, thread pools, concurrency model                         |
| [`docs/STARTUP_SEQUENCE.md`](docs/STARTUP_SEQUENCE.md)               | Step-by-step startup walkthrough                             |
| [`docs/COMMAND_SERVER.md`](docs/COMMAND_SERVER.md)                   | TCP command protocol and client usage                        |
| [`docs/CONFIGURATION_REFERENCE.md`](docs/CONFIGURATION_REFERENCE.md) | All `config.ini` settings                                    |
| [`docs/DEBUGGING.md`](docs/DEBUGGING.md)                             | Diagnostics, logging, and troubleshooting                    |
| [`docs/SHELL_SCRIPTS.md`](docs/SHELL_SCRIPTS.md)                     | `twsstart.sh` / `gatewaystart.sh` reference                  |
