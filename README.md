# IBC — Interactive Brokers Controller

IBC automates many aspects of running [Interactive Brokers](https://www.interactivebrokers.com)
[Trader Workstation and IB Gateway](https://www.interactivebrokers.com/en/index.php?f=14099#tws-software)
that would otherwise require manual intervention. It is especially useful for
automated trading systems built on the [IB API](http://interactivebrokers.github.io).

> **IMPORTANT** — IBC only works with the **offline (standalone)** version of TWS/Gateway.
> It does **not** work with the self-updating version.

## Scope

> **Linux only.** This fork supports **Linux exclusively**. The original
> [IbcAlpha/IBC](https://github.com/IbcAlpha/IBC) repository shipped Windows,
> macOS and Linux resources; here the Windows (`.bat`, `.vbs`, `.lnk`, Task
> Scheduler XML) and macOS (`*macos.sh`, `.plist`) files have been removed, and
> the only distribution produced is `IBCLinux-x.y.z.zip`.

All of IBC's own functionality — login and 2FA automation, dialog handling,
session management, auto-restart and the command server — is available on Linux,
unchanged.

## What IBC does

- Automatically fills in your username and password on the login dialog and clicks Login
- Handles Two-Factor Authentication via the IBKR Mobile app, with automatic retries if the alert is missed
- Clicks through dialog boxes that would otherwise require user interaction
- Allows TWS/Gateway to auto-restart each day during the week without re-authentication
- Allows TWS/Gateway to shut down at a scheduled time
- Accepts or rejects incoming API connections based on your configuration
- Responds to remote commands (STOP, RESTART, RECONNECT, ENABLEAPI) via a TCP command server

## What this fork adds

The upstream project, [IbcAlpha/IBC](https://github.com/IbcAlpha/IBC), was retired
on 1 September 2026 and its repository archived read-only. This fork carries IBC
forward, focused on Linux (see [Scope](#scope)) and on unattended operation:

- **Automated TOTP two-factor authentication.** Set `Ib2FA` in `config.ini` to
  the base32 secret from your authenticator app: IBC computes the current
  6-digit code itself, fills it in and submits the 2FA dialog, with no manual
  step. The implementation is in-tree (`Totp`, `Base32` — RFC 4226/6238 over the
  JDK's own HMAC-SHA1), so there is no external dependency.
- **A current, enforced toolchain.** Java 25 and Maven replace the retired
  Ant/Ivy build. A JUnit 5 test suite runs in CI with JaCoCo coverage reported
  by octocov, Checkstyle (with reviewdog annotations on pull requests) and
  SpotBugs static analysis.
- **Testability refactors** behind that suite: settings, the login and session
  managers and the command dispatcher are reached through interfaces and stubs,
  so behaviour is verified against fakes rather than a live TWS.

## Getting Started

### 1. Install the offline TWS or IB Gateway

Download the **offline** installer from the
[IBKR software page](https://www.interactivebrokers.com/en/index.php?f=14099#tws-software).
Do **not** use the self-updating version.

TWS must be set to English so that IBC can recognise its dialogs. Set the language manually
on the first login.

### 2. Install IBC

Download the latest `IBCLinux-x.y.z.zip` from the
[Releases page](https://github.com/hartza-capital/IBC/releases) and extract it:

```bash
sudo unzip ~/Downloads/IBCLinux-3.24.1.zip -d /opt/ibc
cd /opt/ibc
sudo chmod o+x *.sh */*.sh
```

Default paths assumed by the scripts:

| Item              | Path               |
| ----------------- | ------------------ |
| TWS/Gateway jars  | `~/Jts/<version>/` |
| IBC program files | `/opt/ibc`         |
| `config.ini`      | `~/ibc/config.ini` |

### 3. Configure IBC

Copy `config.ini` from the IBC installation folder to a secure location (e.g. `~/ibc/`),
then edit it:

```bash
mkdir -p ~/ibc
cp /opt/ibc/config.ini ~/ibc/
```

Minimum settings to review:

| Setting                            | Description                                           |
| ---------------------------------- | ----------------------------------------------------- |
| `IbLoginId`                        | Your IBKR username                                    |
| `IbPassword`                       | Your IBKR password                                    |
| `TradingMode`                      | `paper` or `live` (default: `live`)                   |
| `AcceptNonBrokerageAccountWarning` | Set to `yes` to auto-accept the paper account warning |
| `AcceptIncomingConnectionAction`   | `accept`, `reject`, or `manual`                       |
| `CommandServerPort`                | TCP port for the command server (`0` = disabled)      |
| `AutoRestartTime`                  | Time for daily auto-restart without re-authentication |
| `ClosedownAt`                      | Time for scheduled shutdown (e.g. Friday evening)     |

The `config.ini` file contains detailed comments for every setting.

### 4. Set the TWS version in the start scripts

Edit `twsstart.sh` or `gatewaystart.sh` and set `TWS_MAJOR_VRSN` to match your installed
version. To find the version, open TWS manually and click `Help > About Trader Workstation`.
A build string like `Build 10.19.1f` means version `1019`.

### 5. Start IBC

```bash
/opt/ibc/twsstart.sh       # start TWS under IBC
/opt/ibc/gatewaystart.sh   # start IB Gateway under IBC
```

IBC writes a log file whose location is displayed at startup. Include this log when
reporting issues.

## Auto-restart

IBC supports TWS/Gateway's built-in weekly auto-restart mechanism (introduced in TWS 974).
With auto-restart configured, a single authentication on Sunday is enough to keep
TWS running all week:

1. In the TWS/Gateway Global Configuration dialog, set the **AutoRestart** time in the
   Lock and Exit section.
2. Alternatively, use the `AutoRestartTime` setting in `config.ini`.

TWS shuts down and restarts at the configured time each day; IBC handles the restart
transparently without re-authentication. On Sunday the session credentials expire and
a fresh login is required.

Use `ClosedownAt` in `config.ini` to shut down automatically on Friday evening.

## Running with crontab

Use `crontab` to start IBC automatically. The scripts detect if IBC is already running
and skip a duplicate start:

```cron
# Start gateway every 15 minutes Mon–Fri (only starts if not already running)
0,15,30,45 * * * 1-5 export DISPLAY=:10 && /bin/bash /opt/ibc/gatewaystart.sh
```

## Two-Factor Authentication

The `Ib2FA` setting in `config.ini` decides what IBC does with the Second Factor
Authentication dialog.

### Automated TOTP

Set `Ib2FA` to the base32 secret behind your authenticator app — the same key you
would scan as a QR code. IBC computes the current 6-digit code itself (an in-tree
implementation of RFC 4226/6238 over the JDK's own HMAC-SHA1, with no external
dependency), fills the code field, and submits the dialog:

```ini
Ib2FA=YOUR_BASE32_TOTP_SECRET
```

When several 2FA methods are registered, IBKR presents a device list before the code
field. `SecondFactorDevice` selects the entry whose name matches exactly:

```ini
SecondFactorDevice=TOTP
```

### Manual / IBKR Mobile

Leave `Ib2FA` empty. IBC submits nothing and logs an error; the dialog stays open
for you to acknowledge the push notification on your device, or to type a code.

If you acknowledge the alert but login does not complete within the time IBKR
allows, IBC can repeat the login sequence rather than give up:

```ini
ReloginAfterSecondFactorAuthenticationTimeout=yes
SecondFactorAuthenticationExitInterval=60
```

Set `TWOFA_TIMEOUT_ACTION=restart` in the start script so IBC relaunches and repeats
the authentication.

Any other value of `Ib2FA` is decoded as a base32 secret, so a string that is not one
makes IBC submit a wrong code.

## Command Server

IBC listens on a TCP port for remote commands (configure `CommandServerPort` in
`config.ini`). Send commands via `telnet` or the provided `commandsend.sh` script:

| Command            | Effect                                                                                                                                                               |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `STOP`             | Shuts down TWS/Gateway cleanly                                                                                                                                       |
| `RESTART`          | Initiates auto-restart (no re-authentication needed)                                                                                                                 |
| `PAUSE`            | Shuts down tidily like `STOP`, but preserves login credentials so the next IBC start resumes the session without re-authenticating (useful with a UPS on power loss) |
| `RECONNECTDATA`    | Refreshes all market data connections (Ctrl-Alt-F equivalent)                                                                                                        |
| `RECONNECTACCOUNT` | Reconnects to the IB login server (Ctrl-Alt-R equivalent)                                                                                                            |
| `ENABLEAPI`        | Enables the ActiveX/Socket Clients API checkbox                                                                                                                      |
| `EXIT`             | Closes the command server connection                                                                                                                                 |

## Multiple IBC Instances

To run multiple TWS/Gateway instances simultaneously (e.g. live + paper), create a
separate start script for each instance and set a different `TWS_SETTINGS_PATH` in each:

```bash
# In StartTWSLive.sh
TWS_SETTINGS_PATH=/home/user/JtsLive

# In StartTWSPaper.sh
TWS_SETTINGS_PATH=/home/user/JtsPaper
```

Each instance needs its own `config.ini` with separate credentials and a different
`LOG_PATH`.

## Building from Source

**Prerequisites:** Java 25 JDK, Maven 3.9+, environment variable `IBC_BIN` pointing
to your TWS/Gateway `jars/` directory. The pinned Maven version comes with the
wrapper (`./mvnw`), so an installed Maven is only needed if you bypass it.

```bash
export IBC_BIN=/home/user/Jts/1050/jars

make jar       # compile + build resources/IBC.jar (no ZIP)
make dist      # compile + build resources/IBC.jar + dist/IBCLinux-x.y.z.zip
make clean     # remove build artifacts
make rebuild   # clean + dist
```

Direct Maven equivalents, via the wrapper: `./mvnw compile`,
`./mvnw package -DskipAssembly=true` (jar only, matches `make jar`/`make dev`),
`./mvnw package` (jar + distribution ZIP, matches `make dist`), `./mvnw clean`.

The compiled output is `resources/IBC.jar` — a plain JAR containing only IBC's own
classes, since IBC has no external runtime dependencies.

To update the version: change `<version>` in `pom.xml`.

## Support

IBC is developed and supported in this fork:

- **Issue Tracker:** [https://github.com/hartza-capital/IBC/issues](https://github.com/hartza-capital/IBC/issues)

The upstream project, [IbcAlpha/IBC](https://github.com/IbcAlpha/IBC), was retired on
1 September 2026 and its repository archived read-only, so no issues or pull requests
can be opened there.

Please include the IBC log file (location shown at startup) in any problem reports,
along with the IBC and TWS/Gateway version numbers.

## Contributing

Please read the [contributor guidelines](CONTRIBUTING.md) and submit a pull request.

## License

IBC is licensed under the
[GNU General Public License v3](http://www.gnu.org/licenses/gpl.html).

## History

IBC is a fork of the original [IBController project](https://github.com/ib-controller/ib-controller),
maintained by Richard L King (rlktradewright on GitHub) from 2004 to 2018.
This fork was created in early 2018 to continue development and support.

If switching from IBController, note these key changes:

| IBController                                       | IBC                              |
| -------------------------------------------------- | -------------------------------- |
| `IBController.ini`                                 | `config.ini`                     |
| `ForceTwsApiPort`                                  | `OverrideTwsApiPort`             |
| `AcceptIncomingConnectionAction` default: `accept` | default: `manual`                |
| `IbControllerPort`                                 | `CommandServerPort` (default: 0) |
| `IbControlFrom`                                    | `ControlFrom`                    |
| `IbBindAddress`                                    | `BindAddress`                    |
| `IBControllerStart.sh`                             | `twsstart.sh`                    |
| `IBControllerGatewayStart.sh`                      | `gatewaystart.sh`                |
