# IBC Shell Scripts Reference

This guide covers the shell scripts shipped in the `resources/` directory. They fall into two categories: **startup scripts** that launch TWS or IB Gateway under IBC control, and **command scripts** that send runtime instructions to a running IBC instance.

---

## Startup Scripts

Two startup scripts are provided, covering two applications (TWS and Gateway) on Linux:

| Script            | Application        | Platform |
| ----------------- | ------------------ | -------- |
| `twsstart.sh`     | Trader Workstation | Linux    |
| `gatewaystart.sh` | IB Gateway         | Linux    |

### Configuration Variables

Each startup script exposes the same set of variables at the top of the file. Edit only that section — the rest of the script must not be modified.

| Variable               | Default value      | What it does                                                                                                                                                     |
| ---------------------- | ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TWS_MAJOR_VRSN`       | `1019`             | Major version number of the installed TWS or Gateway. Find it under Help > About in the application.                                                             |
| `IBC_INI`              | `~/ibc/config.ini` | Path to the IBC configuration file (`config.ini`). Keep this inside an encrypted personal folder.                                                                |
| `TRADING_MODE`         | _(empty)_          | `live` or `paper`. When empty, the value falls back to `TradingMode` in `config.ini`, then defaults to `live`.                                                   |
| `TWOFA_TIMEOUT_ACTION` | `exit`             | What IBC does when IBKR Mobile 2FA times out without completing login. `exit` terminates IBC; `restart` relaunches it and repeats the authentication sequence.   |
| `IBC_PATH`             | `/opt/ibc`         | Directory containing the IBC installation (the `scripts/` subfolder must be present here).                                                                       |
| `TWS_PATH`             | `~/Jts`            | Installation directory of TWS or Gateway. The official installer always uses this path; there is rarely a reason to change it.                                   |
| `TWS_SETTINGS_PATH`    | _(empty)_          | Where TWS/Gateway stores its settings. Defaults to `TWS_PATH` when empty. Must match `IbDir` in `config.ini` if that setting is used, or auto-restart will fail. |
| `LOG_PATH`             | `~/ibc/logs`       | Directory for IBC diagnostic logs. Leave empty to log to the terminal; remove the line entirely to suppress all logging (not recommended).                       |
| `TWSUSERID`            | _(empty)_          | IBKR username. Prefer setting credentials inside `config.ini` in a protected location rather than here.                                                          |
| `TWSPASSWORD`          | _(empty)_          | IBKR password in plain text. Same security warning as `TWSUSERID`.                                                                                               |
| `FIXUSERID`            | _(empty)_          | FIX Gateway username. Only relevant when `FIX=yes` is set in `config.ini`. Present in gateway scripts only.                                                      |
| `FIXPASSWORD`          | _(empty)_          | FIX Gateway password. Only relevant when `FIX=yes` is set in `config.ini`. Present in gateway scripts only.                                                      |
| `JAVA_PATH`            | _(empty)_          | Path to a custom Java installation. The TWS/Gateway installer bundles its own JVM; leave this empty unless you have a specific reason to override it.            |
| `HIDE`                 | _(empty)_          | Set to `YES` or `TRUE` (case-insensitive) to iconify the diagnostic window on launch. Has no effect when running with `-inline`. Linux only.                     |

### Order of Operations

Every startup script follows the same sequence once executed:

1. **Validate IBC scripts** — checks that `displaybannerandlaunch.sh` or `ibcstart.sh` is present and executable inside `${IBC_PATH}/scripts/`. Exits with an error if neither is found.
2. **Detect an already-running process** — runs `pgrep -f "java.*${IBC_INI}"` to look for a JVM process that was started with the same config file. If one is found, the script prints an error and exits without launching a second instance.
3. **Set the `APP` environment variable** — `TWS` for TWS scripts, `GATEWAY` for Gateway scripts.
4. **Export all variables** to the environment so that the downstream IBC scripts can read them.
5. **Handle the `HIDE` flag** (Linux only) — normalises the value to uppercase and sets the `-iconic` flag for `xterm` if applicable.
6. **Launch the application**:
   - With `-inline` argument: calls `displaybannerandlaunch.sh` directly in the current terminal session using `exec`.
   - Without argument: opens a new window and runs `displaybannerandlaunch.sh` inside it.

---

## Command Scripts

These scripts send a single command to a running IBC instance over a TCP connection. They all delegate to `commandsend.sh`.

### `commandsend.sh` — the transport layer

`commandsend.sh` is the only script that opens a network connection. It:

1. Requires exactly one argument: the command name.
2. Connects to `$server_address` (default `127.0.0.1`) on `$command_server_port` (default `7462`) via `telnet`.
3. Sends the command string, waits 1 second, then sends `EXIT` and `quit` to close the telnet session cleanly.

The port `7462` must match the `CommandServerPort` value in your `config.ini`. If IBC runs on a different machine, change `server_address` to the appropriate IP.

### Command wrappers

Each wrapper resolves its own directory with `dirname`/`realpath` so it can be called from any working directory.

| Script                | Command sent       | Effect                                                                                       |
| --------------------- | ------------------ | -------------------------------------------------------------------------------------------- |
| `stop.sh`             | `STOP`             | Initiates a clean shutdown of TWS or Gateway.                                                |
| `restart.sh`          | `RESTART`          | Initiates a clean shutdown followed by an automatic restart of TWS or Gateway.               |
| `reconnectdata.sh`    | `RECONNECTDATA`    | Refreshes all market data connections. Equivalent to pressing Ctrl-Alt-F inside TWS/Gateway. |
| `reconnectaccount.sh` | `RECONNECTACCOUNT` | Reconnects to the IB login server. Equivalent to pressing Ctrl-Alt-R inside TWS/Gateway.     |
| `enableapi.sh`        | `ENABLEAPI`        | Ensures the "Enable ActiveX and Socket Clients" checkbox in the API settings is checked.     |

---

## Practical Examples

### Running two instances simultaneously (live + paper)

TWS/Gateway detects a running process by matching the config file path against the running JVM command line (`pgrep -f "java.*${IBC_INI}"`). Two instances can coexist as long as they each use a distinct `IBC_INI` path.

Create two copies of the startup script — for example `twsstart-live.sh` and `twsstart-paper.sh` — and configure them as follows:

**`twsstart-live.sh`**

```bash
TWS_MAJOR_VRSN=1019
IBC_INI=~/ibc/live/config.ini
TRADING_MODE=live
TWS_SETTINGS_PATH=~/Jts/live
LOG_PATH=~/ibc/live/logs
```

**`twsstart-paper.sh`**

```bash
TWS_MAJOR_VRSN=1019
IBC_INI=~/ibc/paper/config.ini
TRADING_MODE=paper
TWS_SETTINGS_PATH=~/Jts/paper
LOG_PATH=~/ibc/paper/logs
```

Key points:

- `IBC_INI` must differ between instances — this is what the duplicate-process check (`pgrep`) uses to tell them apart.
- `TWS_SETTINGS_PATH` must also differ. If both instances share the same settings folder and both auto-restart at the same time, the concurrent restarts can interfere with each other.
- `LOG_PATH` should differ so the log files do not overwrite each other.
- Each `config.ini` should specify a different `CommandServerPort` so that the command scripts (`stop.sh`, `restart.sh`, etc.) can target each instance independently. Update `command_server_port` in `commandsend.sh` accordingly, or maintain two copies of `commandsend.sh`.

### Automatic retry on 2FA timeout (`TWOFA_TIMEOUT_ACTION=restart`)

By default `TWOFA_TIMEOUT_ACTION=exit`. When using IBKR Mobile for two-factor authentication, if you acknowledge the push notification but the login does not proceed, IBC will simply terminate.

To have IBC restart automatically and present the 2FA prompt again, two changes are required:

**Step 1** — In the startup script, set:

```bash
TWOFA_TIMEOUT_ACTION=restart
```

**Step 2** — In `config.ini`, add or update:

```ini
ReloginAfterSecondFactorAuthenticationTimeout=yes
```

Both changes are mandatory. `TWOFA_TIMEOUT_ACTION=restart` without the `config.ini` setting has no effect.

When using an external scheduler (cron, launchd, systemd) that already restarts IBC on exit, leave `TWOFA_TIMEOUT_ACTION=exit` to avoid double-restart conflicts where IBC's internal restart and the scheduler's restart race against each other.
