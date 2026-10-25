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

`TWS_MAJOR_VRSN` in the start script defaults to `1050`, the major version of
the `10.50.1f` build; set it before the first run if you run another version (a
build string of `10.45.1g` means `1045`). IBC writes a log whose location is
printed at startup.

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

### Start scripts

`gatewaystart.sh` and `twsstart.sh` are the entry points. They export the
variables below; `scripts/displaybannerandlaunch.sh` opens the diagnostic log
and then runs `scripts/ibcstart.sh`, turning those values into arguments.

- With no argument the scripts open an xterm window that shows the IBC version,
  the program and the log location; with `-inline` as the first argument the
  same banner appears in the current terminal, which is the form to use from
  `cron` or `systemd`. Any other argument is ignored.
- They refuse to start while another process already runs with the same
  `config.ini` (`Error: process is already running`).
- With `LOG_PATH` set, everything `ibcstart.sh` prints is captured in
  `LOG_PATH/ibc-<ibcVersion>_<PROGRAM>-<majorVersion>_<Weekday>.txt`, one file
  per weekday, and a file left over from an earlier day is removed at startup;
  with it unset, that output stays on the terminal.

| Variable                    | Default                            | Purpose                                                                                                |
| --------------------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `TWS_MAJOR_VRSN`            | `1050`                             | Installed TWS/Gateway version: `1050` for build `10.50.1f`                                             |
| `IBC_INI`                   | `~/ibc/config.ini`                 | Location of the configuration file                                                                     |
| `IBC_PATH`                  | `/opt/ibc`                         | IBC installation directory                                                                             |
| `TWS_PATH`                  | `~/Jts`                            | TWS/Gateway installation directory                                                                     |
| `TWS_SETTINGS_PATH`         | `TWS_PATH`                         | Where TWS/Gateway stores its settings; set it here rather than with `IbDir`, which breaks auto-restart |
| `LOG_PATH`                  | `~/ibc/logs`                       | Diagnostic log directory; unset, the log goes to the terminal; empty, nothing is logged                |
| `TRADING_MODE`              | `TradingMode` in `config.ini`      | `live` or `paper` (case-insensitive)                                                                   |
| `TWOFA_TIMEOUT_ACTION`      | `exit`                             | `exit` or `restart` when the 2FA dialog times out (case-insensitive)                                   |
| `JAVA_PATH`                 | Java shipped with the installation | Java used to run TWS/Gateway and IBC; falls back to `PATH`, then `JAVA_HOME`                           |
| `TWSUSERID` / `TWSPASSWORD` | empty                              | Credentials passed on the command line; put them in `config.ini` instead                               |
| `FIXUSERID` / `FIXPASSWORD` | empty                              | FIX credentials, `gatewaystart.sh` only                                                                |
| `HIDE`                      | empty                              | `YES` or `TRUE` starts the diagnostic xterm iconified; no effect with `-inline`                        |

### Launcher arguments

`scripts/ibcstart.sh <twsVersion> [options]` is what the two start scripts
call. It can also be run directly: with no argument, or with `-h`, `-?` or
`--HELP` as its first argument, it prints the usage text. Option names are
case-insensitive (`--Gateway` selects the Gateway), and a value must be attached
with `=`: `--mode paper` is rejected, `--mode=paper` is not. The help form is
the exception — `--HELP` is recognised in capitals only, and only in first
position. Run directly, it names the IBC version only when `IBC_VRSN` is
exported, which is what `scripts/displaybannerandlaunch.sh` does after reading
`<ibc-path>/version`; without it the banner reads `Starting IBC version` and
nothing more.

| Argument                   | Default                    | Purpose                                                 |
| -------------------------- | -------------------------- | ------------------------------------------------------- |
| `<twsVersion>`             | —                          | Major version to run, mandatory; e.g. `1050`            |
| `-g`, `--gateway`          | off (TWS)                  | Load the IB Gateway instead of TWS                      |
| `--tws-path=`              | `~/Jts`                    | TWS/Gateway installation directory                      |
| `--tws-settings-path=`     | `--tws-path`               | Where TWS/Gateway stores its settings                   |
| `--ibc-path=`              | `/opt/ibc`                 | IBC installation directory                              |
| `--ibc-ini=`               | `~/ibc/config.ini`         | Location of the configuration file                      |
| `--java-path=`             | Java of the installation   | Directory holding the `java` executable to run IBC with |
| `--user=`, `--pw=`         | empty                      | IBKR credentials                                        |
| `--fix-user=`, `--fix-pw=` | empty                      | FIX credentials; the Gateway with `FIX=yes` only        |
| `--mode=`                  | `config.ini` `TradingMode` | `live` or `paper`, case-insensitive                     |
| `--on2fatimeout=`          | `exit`                     | `restart` or `exit`, case-insensitive                   |

Any other argument is rejected, and so are `--fix-user`/`--fix-pw` when TWS is
selected: the launcher names the offending parameter in the error. Everything is
checked before the application starts — settings path, installed version, IBC
path, `config.ini`, vmoptions file, java — and the first failure decides the
status:

| Status | Cause                                                                                                                       |
| ------ | --------------------------------------------------------------------------------------------------------------------------- |
| `1`    | No usable `java`: neither the installation nor `PATH`/`JAVA_HOME` provides one, or `--java-path` holds no `java` executable |
| `2`    | No version argument                                                                                                         |
| `3`    | Invalid argument, trading mode, 2FA timeout action, or FIX with TWS                                                         |
| `4`    | That TWS/Gateway version is not installed                                                                                   |
| `5`    | Settings or IBC directory does not exist                                                                                    |
| `6`    | `config.ini` does not exist                                                                                                 |
| `7`    | The installation ships neither `tws.vmoptions` nor `ibgateway.vmoptions`                                                    |

`--user` and `--pw` reach the JVM as plain command-line arguments, so they are
visible to `ps`; only the command IBC echoes at startup masks them. A credential
belongs in `config.ini`, not in a wrapper that builds this command.

Those are the launcher's own statuses. Once the application runs, IBC's
`ErrorCodes` come back modulo 256: `76` to `86` for `1100` to `1110` — an
unhandled exception, a control IBC could not find, an invalid setting value —
`87` when the 2FA dialog times out, `88` when the login dialog does not appear
within `LoginDialogDisplayTimeout` seconds. The start scripts report `87` as a
clean stop when `TWOFA_TIMEOUT_ACTION=exit` and `143` (SIGTERM) as a clean stop
in every case; any other failure ends on the error banner and waits for Enter.

### Restart supervision

The launcher runs the JVM in a loop rather than once, and decides from the exit
status and the settings directory whether there is another run:

- `88`, the login dialog never appearing, always starts the application again.
- `COLDRESTART<sessionId>` in the settings directory — left by IBC when the
  session cannot be resumed (a login failure, a login error dialog, a lost
  connection, the `ColdRestartTime` setting) — starts it again with `-Drestart`
  cleared, so the next run authenticates from scratch.
- An `autorestart` file written by TWS/Gateway starts it again without
  re-authentication, unless `PAUSE<sessionId>` — left by `PAUSE` — is there:
  that marker is removed and the loop ends with the application closed.
- With no `autorestart` file, `87` starts it again only when
  `--on2fatimeout=restart` was given; every other status ends the loop.

The status the loop ends on is what the start scripts report. Before the first
run the launcher also renames the installation's own `tws` or `ibgateway` start
script to `tws1`/`ibgateway1`, so the application cannot restart itself outside
IBC.

### Java and JVM options

The launcher starts the installed application directly instead of going through
IB's install4j update stub, so it assembles the `java` command itself.
Everything it passes is read from the installation, which is what makes it work
across Gateway and TWS versions without editing the scripts:

- **The Java installation** — `--java-path=` when given; otherwise the JRE the
  installer records in `.install4j/pref_jre.cfg`, then `inst_jre.cfg`, then the
  bundled `jre/` directory, then `java` from `PATH`, then `$JAVA_HOME`.
- **The JVM options** — every option declared in
  `<program_path>/tws.vmoptions` or `<program_path>/ibgateway.vmoptions`, where
  `<program_path>` is `<tws-path>/<version>` for TWS and
  `<tws-path>/ibgateway/<version>` for the Gateway. A program directory that
  holds no `jars/` is not used: the launcher falls back to the other one, so
  asking for the Gateway on an installation that only carries TWS runs
  `<tws-path>/<version>` with its `tws.vmoptions`. Comment and blank lines are
  skipped, so the heap size (`-Xmx`) and GC flags IB ships take effect unless
  you edit that file. The properties IBC sets itself are filtered out and
  appended once, after the vendor's, so a single value applies: `-Dchannel`,
  `-DjtsConfigDir`, `-Dibcsessionid`, `-Dinstall4jType`, `-Dexe4j.isInstall4j`,
  `-Dtwslaunch.autoupdate.serviceImpl`, `-DjxBrowserKey` and `-Drestart`.
- **The properties only the installation knows** — `-DfullVersion` and
  `-Dchannel` from `.install4j/i4jparams.conf`, and `-DinstallDir=<program_path>/`.
  A `latest` build therefore reports itself as `latest`, not as `stable`.
- **The options IBC's own Swing code needs** — module exports,
  `--enable-native-access` and the JxBrowser key come from the installation's
  `javaOptions` variable in `i4jparams.conf`. An installation that does not
  declare `javaOptions` gets the launcher's own `--add-opens`/`--add-exports`
  set and the `jxBrowserKey` read out of the same file instead.
- **The environment** — `JAVA_TOOL_OPTIONS` is cleared before the command runs,
  so a JVM-wide setting exported on the host does not reach TWS/Gateway.

Startup prints both the resolved options (`Java VM Options=`) and the exact
command it runs, with the credentials replaced by `***`.

## Documentation

- [`docs/userguide.md`](docs/userguide.md) — the User Guide for end users,
  rendered to `resources/userguide.pdf` by `make userguide`.
- [`CONTRIBUTING.md`](CONTRIBUTING.md) — build from source, the `WindowHandler`
  pattern and the PR checklist.
- [`AGENTS.md`](AGENTS.md) — architecture notes, conventions and pitfalls for
  coding agents working in this repository, plus the `## Documentation Index`
  naming each unit's note.
- The Obsidian vault (MCP server `obsidian`, `vault_read`) — the functional
  notes: the repo note is `IBC/IBC.md` and the automation engine's note is
  `IBC/src/ibcalpha/ibc.md`.

## Licence

IBC is licensed under the
[GNU General Public License v3](http://www.gnu.org/licenses/gpl.html).
