# IBC — Interactive Brokers Controller

IBC automates the unattended running of Interactive Brokers' TWS (Trader
Workstation) and IB Gateway: it logs in for you, answers the dialogs those
applications put up, keeps the session alive across daily restarts, and shuts
down on schedule. It is aimed at automated trading systems built on the IB API.

IBC works only with the **offline (standalone)** TWS/Gateway. It does **not**
work with the self-updating version. TWS must be set to English so its dialogs
can be recognised.

## Features

- **Automated login** — fills the username and password fields on the TWS or
  Gateway login dialog and clicks Login.
- **Automated TOTP two-factor authentication** — given a base32 secret, IBC
  computes the current 6-digit code itself (RFC 4226/6238 over the JDK's
  HMAC-SHA1, no external library) and submits the 2FA dialog.
- **Dialog automation** — clicks through the ~30 dialogs TWS/Gateway raise
  (newer-version prompts, the tip of the day, blind-trading warnings, incoming
  API connection requests, and more) according to your settings.
- **Daily auto-restart without re-authentication** — TWS/Gateway restart at a
  configured time each day; IBC handles the restart transparently.
- **Scheduled shutdown** — shut TWS/Gateway down at a fixed time (for example
  Friday evening) via `ClosedownAt`.
- **Remote command server** — a TCP server accepts `STOP`, `RESTART`,
  `RECONNECTDATA`, `RECONNECTACCOUNT`, `PAUSE`, `ENABLEAPI` and `EXIT`.
- **Configuration persistence** — API port, trading mode and related settings
  are written to the TWS `jts.ini` file and survive a restart.

## Structure

```text
IBC/
├── src/
│   ├── ibcalpha/ibc/          # the automation engine (Java, package ibcalpha.ibc)
│   └── assembly/              # Maven assembly descriptor for the Linux ZIP
├── resources/                 # shipped alongside the JAR: config.ini, *.sh, scripts/
├── test/ibcalpha/ibc/         # JUnit 5 suite and the Stub* test doubles
├── pom.xml                    # Maven build (Java 25, Maven wrapper in .mvn/)
├── Makefile                   # convenience targets over ./mvnw
└── CONTRIBUTING.md            # contributor guide
```

| Path                | Contents                                                                                                               |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------- |
| `src/ibcalpha/ibc/` | The Swing automation engine: entry points, window handlers, login, 2FA, configuration tasks, command server, settings. |
| `src/assembly/`     | Assembly descriptor that packages `resources/` into the Linux ZIP.                                                     |
| `resources/`        | `twsstart.sh` / `gatewaystart.sh`, `config.ini`, the command client scripts (`commandsend.sh`, `stop.sh`, ...).        |
| `test/ibcalpha/ibc` | JUnit 5 tests; `StubSettings` / `StubMainWindowManager` replace live TWS.                                              |

## Build

**Prerequisites:** Java 25 JDK, `IBC_BIN` set to the directory holding the
TWS/Gateway jars. The Maven version is pinned by the wrapper (`./mvnw`), so no
system Maven is needed.

```bash
export IBC_BIN=~/Jts/1050/jars

make dev            # compile + resources/IBC.jar (no distribution ZIP)
make dist           # compile + resources/IBC.jar + dist/IBCLinux-<version>.zip
make clean          # remove target/, dist/, resources/IBC.jar, resources/version
make rebuild        # clean + dist

./mvnw -B test            # JUnit 5 suite
./mvnw -B checkstyle:check
./mvnw -B spotbugs:check
```

`make dev` and `make jar` are the same Maven target
(`./mvnw package -DskipAssembly=true`); `make dist` is plain
`./mvnw package`.

The release ZIP is also checked inside Docker, without a local JDK 25:

```bash
make docker-verify   # archive structure, executable bits, version, jar sanity
make docker-smoke    # end-to-end: IBC drives a Gateway in the runtime image
```

See [`docker/README.md`](docker/README.md).

## Getting Started

```bash
git clone https://github.com/hartza-capital/IBC.git
cd IBC
export IBC_BIN=~/Jts/1050/jars   # contains the TWS/Gateway jars
make dist
sudo unzip dist/IBCLinux-*.zip -d /opt/ibc
cp /opt/ibc/config.ini ~/ibc/     # edit: IbLoginId, IbPassword, Ib2FA, ...
/opt/ibc/gatewaystart.sh          # or /opt/ibc/twsstart.sh for TWS
```

Set `TWS_MAJOR_VRSN` in the start script to your installed version before the
first run (a build string of `10.50.1f` means `1050`). IBC writes a log whose
location is printed at startup.

## Configuration

Credentials and behaviour are set in `config.ini` (a commented template ships
in `resources/`), and the start scripts are configured through shell variables.

| `config.ini` setting             | Purpose                                                                                      |
| -------------------------------- | -------------------------------------------------------------------------------------------- |
| `IbLoginId` / `IbPassword`       | IBKR credentials                                                                             |
| `TradingMode`                    | `paper` or `live`                                                                            |
| `Ib2FA`                          | Base32 TOTP secret; empty keeps the 2FA dialog open for a push notification or a manual code |
| `SecondFactorDevice`             | Device name to pick in the 2FA device list                                                   |
| `CommandServerPort`              | TCP port for the command server (`0` = disabled)                                             |
| `AutoRestartTime`                | Daily auto-restart time (no re-authentication)                                               |
| `ClosedownAt`                    | Scheduled shutdown time                                                                      |
| `AcceptIncomingConnectionAction` | `accept`, `reject` or `manual`                                                               |
| `ExistingSessionDetectedAction`  | `primary`, `secondary`, `primaryoverride` or `manual`                                        |
| `OverrideTwsApiPort`             | Force the API listening port regardless of TWS                                               |

| Start-script variable  | Purpose                                             |
| ---------------------- | --------------------------------------------------- |
| `IBC_BIN`              | TWS/Gateway `jars/` directory (required to build)   |
| `TWS_MAJOR_VRSN`       | Installed TWS/Gateway version                       |
| `IBC_INI`              | Path to `config.ini` (default `~/ibc/config.ini`)   |
| `IBC_PATH`             | IBC installation directory (default `/opt/ibc`)     |
| `TRADING_MODE`         | Overrides the `TradingMode` setting                 |
| `TWOFA_TIMEOUT_ACTION` | What to do when 2FA times out (`exit` or `restart`) |
| `JAVA_PATH`            | Java installation used to launch TWS/Gateway        |
| `LOG_PATH`             | Directory for IBC log files                         |

## Documentation

- [`docs/userguide.md`](docs/userguide.md) — the User Guide for end users,
  rendered to `resources/userguide.pdf` by `make userguide`.
- [`src/ibcalpha/ibc/README.md`](src/ibcalpha/ibc/README.md) — the package
  reference: every class, its role and its public surface.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — build from source, the `WindowHandler`
  pattern and the PR checklist.
- [`AGENTS.md`](AGENTS.md) — architecture notes, conventions and pitfalls for
  coding agents working in this repository.

## Licence

IBC is licensed under the
[GNU General Public License v3](http://www.gnu.org/licenses/gpl.html).
