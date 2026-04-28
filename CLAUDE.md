# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Does

IBC (Interactive Brokers Controller) automates Interactive Brokers TWS (Trader Workstation) and IB Gateway — handling login, 2FA prompts, dialog dismissal, scheduled restarts, and remote shutdown. It works by injecting a Java agent into the TWS/Gateway JVM and automating GUI interactions using Swing/AWT APIs. It targets Java 8 for compatibility with all supported TWS versions.

## Build System

**Apache Ant + Ivy** (not Maven/Gradle).

**Prerequisite:** `IBC_BIN` environment variable must point to the directory containing TWS/Gateway `.jar` files (e.g. the TWS installation `jars/` folder). Without it, all build targets fail.

```bash
# Convenience wrapper around Ant
make dev        # compile + jar (fast dev cycle)
make jar        # full build including Ivy dependency resolution
make dist       # build + create platform distribution ZIPs
make clean      # remove target/ and dist/
make rebuild    # clean + dist
make check-env  # verify IBC_BIN is set
```

Direct Ant equivalents: `ant compile`, `ant jar`, `ant dist`, `ant clean`.

The compiled output is `resources/IBC.jar` (fat JAR with bundled dependencies).

## No Automated Tests

The `test/` directory exists but is empty. There is no test framework configured. Validation is done by running IBC against a paper trading TWS/Gateway instance.

## Architecture

Single Java package `ibcalpha.ibc` (~83 classes). Two entry points:
- `IbcTws.main()` — for TWS
- `IbcGateway.main()` — for Gateway (delegates to IbcTws after setup)

Both follow: `checkArguments()` → `setupDefaultEnvironment()` → `IbcTws.load()`

**Key subsystems:**

| Area | Key Classes |
|------|-------------|
| Login/Auth | `AbstractLoginHandler`, `DefaultLoginManager`, `SecondFactorAuthenticationDialogHandler` |
| Configuration | `DefaultSettings`, `JtsIniManager` (manages TWS `.ini` file), `Configure*Task` classes |
| GUI automation | `SwingUtils` (879 lines, core utility), `TwsListener` (AWT event monitor), `*FrameHandler`/`*DialogHandler` classes |
| Main window | `DefaultMainWindowManager`, `MainWindowFrameHandler` |
| Remote control | `CommandServer` (TCP), `CommandDispatcher`, `CommandChannel` |
| Threading | `MyCachedThreadPool`, `MyScheduledExecutorService`, `GuiExecutor`/`GuiDeferredExecutor` |

**Dialog handler pattern:** Each TWS dialog/window gets its own `*DialogHandler` or `*FrameHandler` class implementing `WindowHandler`. `TwsListener` receives all AWT window events and routes them to the registered handlers via `MainWindowManager`.

**Configuration flow:** `config.ini` → `DefaultSettings` → `Configure*Task` classes that read settings and call `JtsIniManager` to write TWS's own `.ini` configuration file before TWS initializes.

## Configuration

Main template: `resources/config.ini`. Key settings:
- `IbLoginId`, `IbPassword` — credentials
- `Ib2FA` — 2FA mode (`IBKR Mobile` | `TOTP` | `none`)
- `IbcPortOffset`, `TwsApiPort` — API port configuration
- `AutoRestartTime`, `ClosedownAt` — scheduling
- Command server bind addresses/ports for remote control

## Runtime Scripts

Platform scripts in `resources/scripts/`: `twsstart.sh`, `gatewaystart.sh` (Linux), `twsstartmacos.sh`, `gatewaystartmacos.sh`, plus `stop.sh`, `restart.sh`, `reconnectaccount.sh`, `reconnectdata.sh`. Windows equivalents are `.bat` files.

## Version

Defined in `build.xml` (`ver` property). Updating it regenerates `IbcVersionInfo.java` at compile time and writes `resources/version`.
