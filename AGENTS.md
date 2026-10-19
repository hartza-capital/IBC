# AGENTS.md

Guidance for coding agents working in this repository.

IBC is a GUI automation agent for IB TWS and IB Gateway: it hooks into the Swing
event system, intercepts window events, and drives dialogs programmatically. This
is the **hartza-capital fork, which supports Linux only** — Windows and macOS
resources, the Ant/Ivy build and the samples were removed in favour of Maven and a
Linux-only distribution. `origin` is this fork (default branch `develop`),
`upstream` is `IbcAlpha/IBC` (retired 1 Sep 2026, archived read-only).

## Build System

**Prerequisites:** Java 25 JDK, Maven 3.9+, `IBC_BIN` env var pointing to the TWS/Gateway `jars/` directory (e.g. `~/Jts/1050/jars`). Use `./mvnw` — the Maven wrapper pins the version CI uses — rather than a system `mvn`. The JDK is keg-only: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home` (see Pitfalls). The `Makefile` picks the JDK itself: the ambient `JAVA_HOME` when it holds a javac >= 25, else the keg-only Homebrew `openjdk@25`, else whatever is on `PATH`, and `check-jdk` vets the result before Maven starts. **Your shell does not need to export `JAVA_HOME`** — export it only to force a specific JDK. `check-env` likewise rejects an `IBC_BIN` that is unset, not a directory, or without jars, which is what a TWS/Gateway upgrade leaves behind (the version in the path changes).

```bash
export IBC_BIN=~/Jts/1050/jars   # required for all builds

make dev      # compile + produce resources/IBC.jar (build only: no tests, no ZIP)
make dist     # compile + resources/IBC.jar + dist/IBCLinux-x.y.z.zip
make clean    # remove build artifacts (target/, dist/, resources/IBC.jar, resources/version)
make rebuild  # clean + dist
make format   # run prettier --write on **/*.md and **/*.json
make check-env  # fail fast if IBC_BIN is unset, not a directory, or holds no jars
make check-jdk  # fail fast if the active javac is older than 25
```

`make dev` and `make jar` are the same target
(`package -DskipTests -DskipAssembly=true`): they **build only**, so they stay
quiet. `make dist` additionally runs the test phase before packaging — a
shippable ZIP is never built from unvalidated code. `make checkstyle` and
`make test` exist so the git hook and humans share the same JDK selection; the
remaining gates run through Maven:

```bash
./mvnw -B test                # JUnit 5 suite in test/ibcalpha/ibc/ (19 files, 105 @Test)
./mvnw -B checkstyle:check    # config/checkstyle.xml — the same ruleset CI and reviewdog use
./mvnw -B spotbugs:check      # advisory only (failOnError=false)
./mvnw -B verify              # test + jacoco + jar + dist ZIP + spotbugs (checkstyle is not bound to any phase)
```

lefthook's pre-commit (`lefthook install`) runs `make checkstyle`, `make test` and `prettier --check` on staged files. It goes through `make` on purpose: a bare `./mvnw` inherits the developer's `JAVA_HOME` and fails with `release version 25 not supported`. `lefthook.yml` is generated from the toolbox template, so the same change belongs upstream or the next `sync-repos.sh` reverts it.

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

The dialog is submitted **only** once the passcode field has actually been written (`fillTotpCode` returns true). A secret that is not valid Base32 stops IBC at startup (`IbcTws.validateTwoFactorAuthenticationSetting`); a secret that decodes but is wrong cannot be detected — which is why `config.ini` ships `Ib2FA=` empty. Both paths exist because a wrong or empty submission spends a failed login attempt, and IBKR locks the account after a few.

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
- **Checkstyle applies to `src/` only** — all rules are suppressed for `test/` (see `checkstyle-suppressions.xml`). The ruleset is `config/checkstyle.xml`: defect-shaped rules (imports, empty blocks, switch defaults, fallthrough, `equals`/`hashCode`, `System.out` misuse), **not** the Sun formatting rules the plugin falls back to when no `configLocation` is set. Formatting debt (80-column lines, `final` parameters, javadoc on fields) is deliberately out of scope and tracked separately.
- Markdown and JSON are formatted with prettier (printWidth 80, `proseWrap: preserve`); run `make format` before committing docs, or the pre-commit hook fails.
- Fork-added and fork-touched methods carry Javadoc (see `Totp`, `Base32`, `SecondFactorAuthenticationDialogHandler`).
- CI (`.github/workflows/test-java.yml`) runs checkstyle, `./mvnw -B test`, reviewdog on PRs, and octocov. octocov's `acceptable` value is a **ratchet** on line coverage (currently 20%, measured); raise it as tests land, never lower it to unblock a build.

## Pitfalls

- **`IBC_BIN` must point at a directory containing the TWS/Gateway jars**; the default JDK on this workstation is Temurin 17, and `release=25` is pinned in the pom, so a build under 17 fails. Install the JDK keg-only (`brew install openjdk@25`) and export `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`, since `openjdk@25` is not linked into `/opt/homebrew`. A container works too: `docker run --rm -v "$PWD":/w -w /w -e IBC_BIN=/w/jars eclipse-temurin:25-jdk ./mvnw -B test`.
- `resources/IBC.jar` and `resources/version` are **build outputs**, gitignored and removed by `make clean`. Never commit them.
- `IbcVersionInfo.java` is generated — edit `<version>` in `pom.xml`, not the file.
- Upstream-derived files keep their **CRLF** line endings (e.g. `SecondFactorAuthenticationDialogHandler.java`) while newer ones are LF. Don't renormalize endings; keep diffs minimal.
- `resources/config.ini` ships `Ib2FA=` **empty** on purpose: any non-empty placeholder is encoded into a passcode and submitted, spending a failed login attempt. `SecondFactorAuthenticationDialogHandlerTest.shippedConfigShipsNoTwoFactorSecret` fails if a secret creeps back into the shipped config.
- The PR template's checklist names the Maven commands; there are no `make lint`/`make test` targets.
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

Project documentation generated by `app-documenter`.

### Directory Structure

```text
IBC/
├── README.md                    # front door: what IBC is, build, getting started
├── CONTRIBUTING.md              # contributor guide
├── AGENTS.md                    # agent guidance (this file)
├── src/
│   ├── ibcalpha/ibc/README.md   # package reference: every class and its surface
│   └── assembly/                # Maven assembly descriptor for the Linux ZIP
├── docker/                      # Dockerfile + release checks (verify, gateway smoke)
├── resources/                   # shipped config.ini, start scripts, command clients
└── test/ibcalpha/ibc/           # JUnit 5 suite and the Stub* doubles
```

### Modules

| Unit           | README                                                     | Language | Description                                                                                                                                    |
| -------------- | ---------------------------------------------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `ibcalpha.ibc` | [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md) | Java     | The Swing automation engine: entry points, window/dialog handlers, login and 2FA, configuration tasks, command server, settings, thread pools. |

### By Category

#### Entry Points and Handlers

- [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md) — `IbcTws` / `IbcGateway`
  entry points, the `WindowHandler` model, the login and 2FA flow, the
  `Configure*Task` configuration tasks.

#### Project Documentation

- [`README.md`](README.md) — end-user front door: features, structure, build,
  getting started, configuration.
- [`docker/README.md`](docker/README.md) — containerised release checks:
  `verify` on the ZIP, `gateway-smoke` end to end.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — build from source, the `WindowHandler`
  pattern, manual testing and the PR checklist.
