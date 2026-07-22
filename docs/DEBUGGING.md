# IBC Debugging Guide

A practical troubleshooting reference for IBC operators.

---

## 1. Enabling Detailed Logs: LogStructureScope and LogStructureWhen

IBC can dump the full Swing component tree of every TWS window it encounters. This is the primary diagnostic tool when IBC fails to interact with a window after a TWS update.

Configure both settings in `config.ini` under section 9 (Diagnostic Settings).

### LogStructureScope — which windows are eligible

| Value             | Windows logged                                                                         |
| ----------------- | -------------------------------------------------------------------------------------- |
| `known` (default) | Only windows IBC **recognizes** and has a handler for.                                 |
| `unknown`         | Only windows IBC does **not** recognize. This covers the vast majority of TWS windows. |
| `untitled`        | Only unrecognized windows that have **no title** (typically popup message boxes).      |
| `all`             | **Every** window displayed by TWS.                                                     |

### LogStructureWhen — when to log eligible windows

| Value                           | Trigger                                                                                                               |
| ------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `never` (default)               | Structure is never logged.                                                                                            |
| `open` (aliases: `yes`, `true`) | Logs the structure the **first time** the window appears (WINDOW_OPENED event).                                       |
| `openclose`                     | Logs the structure on every **open and close** (WINDOW_OPENED and WINDOW_CLOSED events).                              |
| `activate`                      | Logs the structure every time the window is **brought to the foreground** (WINDOW_OPENED or WINDOW_ACTIVATED events). |

Any invalid value for either setting is reported as an error and the setting falls back to its default (`known` / `never`).

> **Note:** The deprecated `LogComponents` setting overrides both `LogStructureScope` and `LogStructureWhen`. If it is set to any value, it is treated as equivalent to `LogStructureWhen=<that value>` and `LogStructureScope=all`. Remove or comment out `LogComponents` to use the explicit settings.

### Recommended configuration for diagnosing an unknown window

```ini
LogStructureScope=all
LogStructureWhen=open
```

This logs the component tree of every TWS window the first time it opens, with minimal log volume compared to `activate` or `openclose`.

### What appears in the log

For every eligible event, IBC writes two lines:

1. A one-line summary: `Window structure for <type> entitled: <title>; event=<EventName>`
2. The full indented component tree. Each node shows:
   - Component name (Java name field)
   - Full class name in parentheses
   - Details in braces: for `JButton` the label, for `JCheckBox` the label and selection state, for `JTextField` the current value, for `JPasswordField` a literal `***`, for `JLabel` the text, etc.
   - Tree nodes, list items, and combo box items are shown with `|   ` indentation.

---

## 2. Common Log Messages and Their Meaning

All IBC messages are written to stdout (or the log file) with a timestamp prefix in the format `yyyy-MM-dd HH:mm:ss:SSS IBC: <message>`. Error messages are additionally surrounded by a line of `!` characters on stderr.

| Log message                                                    | Meaning                                                                                               |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `detected frame entitled: <title>; event=<event>`              | IBC received a window event for a TWS main frame. Normal during startup and operation.                |
| `detected dialog entitled: <title>; event=<event>`             | IBC received a window event for a TWS dialog. Check the title if login is stalling.                   |
| `detected window: type=<classname>; event=<event>`             | A non-Frame, non-Dialog window appeared. Usually benign.                                              |
| `Window structure for ...; event=<event>`                      | Structure logging is active and IBC is about to dump the component tree.                              |
| `Login attempt: <n>`                                           | IBC is starting login attempt number n.                                                               |
| `Login dialog WINDOW_OPENED: LoginState is <state>`            | The login dialog appeared; shows the current login state machine value.                               |
| `Click button: <label>`                                        | IBC is clicking a button. Confirms the handler found the expected control.                            |
| `Button was disabled, has been enabled: <label>`               | IBC forcibly enabled a disabled button before clicking it.                                            |
| `Button now disabled: <label>`                                 | The button became disabled immediately after the click (can indicate the click had no effect).        |
| `Setting Trading mode = <mode>`                                | IBC is writing the trading mode to jts.ini.                                                           |
| `Some settings updated in <path>`                              | jts.ini was modified by IBC.                                                                          |
| `Missing lines in <path>`                                      | Expected entries were absent from jts.ini.                                                            |
| `TWS API socket port is already set to <port>`                 | Port override not needed; value already matches.                                                      |
| `TWS API socket port now set to <port>`                        | Port override applied.                                                                                |
| `Will re-login at <datetime>`                                  | 2FA timed out; IBC will attempt login again at the stated time.                                       |
| `CommandServer listening on address: <addr>`                   | The command server started successfully.                                                              |
| `CommandServer accepted connection from: <addr>`               | A client connected to the command server.                                                             |
| `CommandServer denied access to: <addr>`                       | A connection was refused because the source IP is not in `ControlFrom`.                               |
| `CommandServer received command: <cmd>`                        | IBC received a command string.                                                                        |
| `Exiting with exit code=<n>`                                   | IBC is about to call `System.exit(n)`. The code identifies the error (see section 3).                 |
| `An exception has occurred: <message>`                         | Unhandled Java exception. Enable `IncludeStackTraceForExceptions=yes` in config.ini for full details. |
| `the LogStructureScope setting '<value>' is invalid.`          | Unrecognized value; falling back to `known`.                                                          |
| `the LogStructureWhen setting is invalid: <value>`             | Unrecognized value; falling back to `never`.                                                          |
| `IBC closing after TWS/Gateway failed to display login dialog` | Login dialog did not appear within `LoginDialogDisplayTimeout` seconds.                               |

---

## 3. Error Codes

IBC exits with a numeric code that identifies the class of failure. All codes are in `ErrorCodes.java`.

| Exit code | Constant                             | Cause                                                                                                                                                                   | Corrective action                                                                                                                                                  |
| --------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `1100`    | `UNHANDLED_EXCEPTION`                | An uncaught Java exception reached the top-level handler (either `UncaughtExceptionHandler` or a caught `Throwable` in the event dispatch loop).                        | Set `IncludeStackTraceForExceptions=yes` and restart to capture the full stack trace. Check the IBC and TWS versions for compatibility.                            |
| `1101`    | `CANT_FIND_CONTROL`                  | IBC looked for a specific UI control (e.g. a button or the 2FA device list) and could not find it. Usually caused by a TWS update that changed the dialog layout.       | Enable `LogStructureScope=all` + `LogStructureWhen=open` to capture the current window structure and compare with what IBC expects.                                |
| `1102`    | `INCORRECT_NUMBER_OF_ARGS`           | The Java entry point was called with the wrong number of command-line arguments.                                                                                        | Do not invoke `ibcalpha.ibc.IbcTws` directly; use the provided shell scripts (`twsstart.sh`, `gatewaystart.sh`).                                                   |
| `1103`    | `INI_FILE_DOES_NOT_EXIST`            | The path given for `config.ini` does not exist or is not readable.                                                                                                      | Check `IBC_INI` in the start script. Default is `~/ibc/config.ini`.                                                                                                |
| `1104`    | `INVALID_TRADING_MODE`               | The `TradingMode` argument (from the script or config.ini) is neither `live` nor `paper`.                                                                               | Set `TradingMode=live` or `TradingMode=paper` in config.ini (or the `--mode` argument).                                                                            |
| `1105`    | `INVALID_SETTING_VALUE`              | A config.ini value is not one of the accepted values for that setting.                                                                                                  | Read the error message to identify the setting, then check the accepted values in config.ini's comments.                                                           |
| `1106`    | `CANT_CREATE_TWS_SETTINGS_DIR`       | IBC could not create the TWS settings directory.                                                                                                                        | Check that the path exists and is writable by the user running IBC.                                                                                                |
| `1107`    | `CANT_FIND_ENTRYPOINT`               | The TWS/Gateway main class could not be located in the classpath. This typically means the jars in `TWS_PATH` do not match the version specified by `TWS_MAJOR_VRSN`.   | Verify that `TWS_MAJOR_VRSN` in the start script matches the installed offline TWS/Gateway version. The classpath is built from `<TWS_PATH>/<version>/jars/*.jar`. |
| `1108`    | `INVALID_JTSINI_PATH`                | The resolved path for `jts.ini` is invalid.                                                                                                                             | Check `TWS_SETTINGS_PATH` (or `IbDir` in config.ini). Ensure the settings directory exists and is readable.                                                        |
| `1109`    | `IO_EXCEPTION_ON_JTSINI`             | An I/O error occurred while reading or writing `jts.ini`.                                                                                                               | Check filesystem permissions and available disk space on the settings path.                                                                                        |
| `1110`    | `INVALID_STATE`                      | IBC reached an internal state that should not occur (e.g. a method called before the session is initialized, or an unrecognized `ExistingSessionDetectedAction` value). | Check the preceding log lines for the specific context. Ensure `ExistingSessionDetectedAction` is one of `manual`, `primary`, `primaryoverride`, or `secondary`.   |
| `1111`    | `SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT` | The 2FA window was displayed but login did not complete within `SecondFactorAuthenticationExitInterval` seconds after the user acknowledged the IBKR Mobile alert.      | See section 5 for 2FA timeout remediation.                                                                                                                         |
| `1112`    | `LOGIN_DIALOG_DISPLAY_TIMED_OUT`     | The login dialog did not appear within `LoginDialogDisplayTimeout` seconds of TWS starting.                                                                             | Increase `LoginDialogDisplayTimeout` (default 60 s). Check available memory and CPU. Consider whether TWS is taking unusually long to initialize.                  |

> **Exit code 1111 modulo 256 = 87.** The shell layer (`displaybannerandlaunch.sh`) checks specifically for exit code `$((1111 % 256))` (i.e. 87) to detect a 2FA timeout and decide whether to restart IBC based on `TWOFA_TIMEOUT_ACTION`.

---

## 4. Diagnosing an Unrecognized Window After a TWS Update

When TWS is updated, dialog titles or component layouts can change so that IBC's handlers no longer match. The typical symptom is that IBC stalls at login (or at some other automated step) without clicking the expected button.

### Step-by-step procedure

**Step 1 — Enable full structure logging**

Edit `config.ini`:

```ini
LogStructureScope=all
LogStructureWhen=open
```

Also enable stack traces for the session:

```ini
IncludeStackTraceForExceptions=yes
```

**Step 2 — Restart IBC**

Restart IBC normally. Let it run until it stalls or until the window in question appears.

**Step 3 — Identify the stalling window**

In the log, look for lines of the form:

```
2026-05-29 10:32:15:042 IBC: detected dialog entitled: <some title>; event=Opened
```

If IBC is stalling, look for the last `detected ... Opened` line before activity stops. That is the window IBC does not know how to handle.

**Step 4 — Read the component tree**

Immediately after the `detected` line (if `LogStructureScope=all` and `LogStructureWhen=open`), you will see:

```
2026-05-29 10:32:15:050 IBC: Window structure for dialog entitled: <title>; event=Opened
<component tree>
```

The component tree uses indentation to show hierarchy. Each line has the format:

```
<name>(<class>){<details>}
```

For buttons: `{JButton: Login}`. For text fields: `{JTextField: }`. For labels: `{JLabel: Enter your credentials}`.

**Step 5 — Compare with existing handlers**

Find the handler Java file that is supposed to manage this window (search `src/ibcalpha/ibc/` for a class whose `recogniseWindow` method matches the old dialog title). Compare the button labels, field positions, and checkbox texts it expects with what appears in the logged structure.

**Step 6 — Determine what changed**

Common changes between TWS versions:

- Button label renamed (e.g. "Login" → "Log In")
- A new panel wrapping existing components (shifts field indices used by `findTextField(container, 0)`)
- Dialog title changed

**Step 7 — Revert logging settings**

Once the issue is understood, restore the settings to avoid excess log volume:

```ini
LogStructureScope=known
LogStructureWhen=never
IncludeStackTraceForExceptions=no
```

---

## 5. Common Errors and Solutions

### IBC does not find the Login button

**Symptom:** IBC logs `detected dialog entitled: <login title>; event=Opened` but then nothing happens. No "Click button" line appears. Eventually `CANT_FIND_CONTROL` (exit code 1101) is logged.

**Cause:** The login dialog's component layout changed in the new TWS version. IBC's handler searches for a button by its exact label text (case-insensitive match via `findButton`). If IBKR renamed the button or restructured the dialog, the search returns null and IBC cannot proceed.

**Resolution:**

1. Enable `LogStructureScope=all` + `LogStructureWhen=open`.
2. Run IBC and capture the structure dump for the login dialog.
3. Identify the actual button label in `{JButton: <label>}` entries.
4. Update the relevant handler to match the new label or structure.

---

### 2FA timeout

**Symptom:** IBC exits with code 87 (1111 mod 256) or logs `SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT` (code 1111). The log may also show a line beginning with `Will re-login at`.

**Relevant settings in config.ini:**

| Setting                                         | Default | Purpose                                                                                                                                                   |
| ----------------------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `SecondFactorAuthenticationTimeout`             | `180`   | How long (in seconds) IBC waits for the IBKR 2FA prompt itself — do not change unless IBKR changes it.                                                    |
| `SecondFactorAuthenticationExitInterval`        | `60`    | How long (in seconds) IBC waits for login to complete **after** you acknowledge the mobile alert. Increase this if your network login round-trip is slow. |
| `ReloginAfterSecondFactorAuthenticationTimeout` | `no`    | Set to `yes` to have IBC automatically restart the login sequence after a timeout (requires IBC v3.14.0+).                                                |

**In the start script:**

| Variable               | Value     | Effect                                                                                                                    |
| ---------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------- |
| `TWOFA_TIMEOUT_ACTION` | `exit`    | IBC terminates cleanly after a 2FA timeout. Use with an external watchdog (cron, systemd).                                |
| `TWOFA_TIMEOUT_ACTION` | `restart` | The script relaunches IBC automatically. Also requires `ReloginAfterSecondFactorAuthenticationTimeout=yes` in config.ini. |

---

### "Command invalid" response from the command server

**Symptom:** You connect to IBC's command server (e.g. via `telnet localhost 7462`) and send a command, but receive `Command invalid` in response.

**Cause:** The command server only recognizes the following commands (case-insensitive):

```
STOP
EXIT
ENABLEAPI
RECONNECTDATA
RECONNECTACCOUNT
RESTART
PAUSE
```

Any other string, including commands with extra whitespace, trailing newlines without carriage returns, or misspellings, is rejected with `Command invalid`.

**Resolution:**

- Verify spelling and casing of the command.
- Ensure the TCP client sends the command terminated with `\r\n` (the channel reads line by line).
- Check `CommandServerPort` is non-zero in config.ini (default is `0`, which disables the server).
- Check that the source IP is permitted by `ControlFrom` (connections from localhost are always allowed).
- Note that `RESTART` is not valid when running the FIX Gateway, and `ENABLEAPI` is not valid for the IB Gateway.

---

### Crash at startup with ClassNotFoundException (or CANT_FIND_ENTRYPOINT, exit code 1107)

**Symptom:** IBC exits immediately at startup with exit code 1107, or a `ClassNotFoundException` stack trace appears in the log naming a TWS main class.

**Cause:** The classpath built by `ibcstart.sh` does not contain the correct TWS jars. The script constructs the classpath as:

```
<TWS_PATH>/<TWS_MAJOR_VRSN>/jars/*.jar
:$<install4j_dir>/i4jruntime.jar
:<IBC_PATH>/IBC.jar
```

**Checklist:**

1. **`TWS_MAJOR_VRSN` in the start script** — must match the major version number of the _installed offline_ TWS/Gateway. Find it via TWS Help > About: `Build 10.19.1f` → major version is `1019`.
2. **Offline installer required** — IBC does not work with the auto-updating (online) TWS. You must install the standalone offline version.
3. **`TWS_PATH` must point to the installation root** — default is `~/Jts`. Do not point it at the versioned subfolder.
4. **`IBC_PATH` must contain `IBC.jar`** — verify with `ls $IBC_PATH/IBC.jar`.

The script prints the generated classpath to the log (`Classpath=...`) before launching Java. If the jars folder is empty or missing, it exits with error code `E_TWS_VERSION_NOT_INSTALLED`.

---

## 6. Log File Location

IBC's diagnostic output is managed by `displaybannerandlaunch.sh`. The behavior depends on the `LOG_PATH` variable set in the start script (`twsstart.sh` or `gatewaystart.sh`):

| `LOG_PATH` value                            | Log destination                                                                                                 |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Set to a directory path (e.g. `~/ibc/logs`) | Output goes to a file inside that directory. The file is created automatically if the directory does not exist. |
| Set to an empty string (`LOG_PATH=`)        | Output is discarded (`/dev/null`).                                                                              |
| Variable unset entirely                     | Output goes to the terminal (no redirection).                                                                   |

**Log file name format:**

```
<LOG_PATH>/ibc-<IBC_VERSION>_<APP>-<TWS_MAJOR_VRSN>_<DayOfWeek>.txt
```

Example: `~/ibc/logs/ibc-3.18.0_TWS-1019_Thursday.txt`

The file is recycled each day: if the file exists but was last modified on a previous day, it is deleted before the new session appends to it. This means there is at most one log file per day of the week — older logs from the same weekday are overwritten on the next cycle.

**Default location** (from the provided start scripts):

```
LOG_PATH=~/ibc/logs
```

Both `twsstart.sh` and `gatewaystart.sh` default to this value. You can change it in those scripts before the `PLEASE DON'T CHANGE ANYTHING BELOW THIS LINE` marker.

Both stdout and stderr are redirected to the log file (`>> "${log_file}" 2>&1`), so IBC errors (written to stderr with `!` banners) and informational messages (written to stdout) appear in the same file.
