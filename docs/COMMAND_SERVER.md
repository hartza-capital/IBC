# IBC Command Server — Technical Reference

## 1. Architecture

The Command Server is a simple TCP server that allows external processes to send control commands to a running IBC instance. The pipeline for each connection is:

```text
CommandServer (TCP accept loop)
    └── CommandChannel  (per-connection I/O wrapper)
            └── CommandDispatcher  (command routing, runs in thread pool)
                    ├── StopTask
                    ├── RestartTask
                    ├── ConfigurationTask → EnableApiTask
                    └── direct key-event dispatch (RECONNECTDATA, RECONNECTACCOUNT)
```

### CommandServer

`CommandServer` implements `Runnable` and is started in a dedicated thread named `"CommandServer"`. On startup it reads `CommandServerPort` from `config.ini`. If the value is `0` it logs a message and returns immediately — the server is not started. Otherwise it opens a `ServerSocket` on the configured port (with a backlog of 5) and enters a blocking `accept()` loop.

Each accepted connection is checked against the access-control list. If the client is permitted, a new `CommandDispatcher` is submitted to `MyCachedThreadPool` (a cached thread pool), wrapping a freshly constructed `CommandChannel`. This gives every connection its own thread and its own I/O channel.

`CommandServer` keeps a static singleton reference accessible via `CommandServer.commandServer()`, used by `StopTask` to shut the server down as part of the stop sequence.

### CommandChannel

`CommandChannel` wraps a `Socket` and exposes typed read/write methods to the dispatcher. On construction it immediately sends the banner `INFO IBC Command Server` to the client (subject to the `SuppressInfoMessages` setting).

Reading is line-oriented: `getCommand()` calls `BufferedReader.readLine()` and skips blank lines, re-issuing the prompt for each one. The channel is closed on any `IOException` or `SocketException`.

### CommandDispatcher

`CommandDispatcher` implements `Runnable` and runs in the cached thread pool. It loops on `mChannel.getCommand()` until the connection is closed (returns `null`) or the client sends `EXIT`. After each command (other than `EXIT`) it calls `mChannel.writePrompt()`.

Command matching is case-insensitive (`equalsIgnoreCase`).

---

## 2. TCP Protocol

### Connection

Connect to `CommandServerPort` on `BindAddress` (or all interfaces if not configured). The server sends an optional banner immediately after the TCP handshake:

```text
INFO IBC Command Server\r\n
```

This line is suppressed when `SuppressInfoMessages=true` (the default).

### Command format

Each command is a single line of plain text terminated by `\n` or `\r\n`. Commands are case-insensitive. Blank lines are silently ignored and the prompt is re-issued.

### Response format

Every response line is terminated by `\r\n` (Java `BufferedWriter.newLine()`).

| Prefix  | Meaning                                       | Example                          |
| ------- | --------------------------------------------- | -------------------------------- |
| `OK`    | Command accepted and completed (or initiated) | `OK Shutting down: STOP command` |
| `ERROR` | Command rejected or failed                    | `ERROR Command invalid`          |
| `INFO`  | Informational message (suppressed by default) | `INFO Closing IBC`               |

### Optional prompt

After each command response (except `EXIT`), the server writes the value of `CommandPrompt` from `config.ini` without a trailing newline. This is empty by default, so no prompt is sent. When configured (e.g., `CommandPrompt=IBC>`), the session behaves like a simple REPL.

### Session lifecycle

```text
[client connects]
  <-- INFO IBC Command Server\r\n     (if SuppressInfoMessages=false)

  --> STOP\n
  <-- OK Shutting down: STOP command\r\n
  [connection closed by server]

  -- OR --

  --> EXIT\n
  <-- OK Goodbye\r\n
  [connection closed by server]
```

---

## 3. Available Commands

### STOP

**Handler:** `CommandDispatcher.handleStopCommand()` → `StopTask.run()`

`StopTask` is constructed with `forceColdRestart=false` and `reason="STOP command"`. It uses a `SwitchLock` to prevent concurrent execution; if a stop is already in progress it responds `ERROR STOP already in progress` and closes the channel.

Execution path:

1. Responds `OK Shutting down: STOP command` and closes the command channel.
2. If the user is not logged in: calls `CommandServer.shutdown()` then `Runtime.getRuntime().halt(0)` — immediate process termination.
3. If the user is logged in: invokes the TWS/Gateway menu item `File > Exit` (TWS) or `File > Close` (Gateway) via `Utils.invokeMenuItem()`. This triggers TWS's own graceful shutdown sequence.

### RESTART

**Handler:** `CommandDispatcher.handleRestartCommand()` → `RestartTask.run()`

Not available for FIX Gateway (`ERROR RESTART is not valid for the FIX Gateway`).

Uses a `SwitchLock` to prevent concurrent execution.

Execution path:

1. Attempts `File > Restart...` via `Utils.invokeMenuItem()`. If that menu item exists (newer TWS versions), responds `OK Restart in progress` and closes the channel.
2. If the menu item is absent (older TWS versions): computes a restart time of `now + 1 minute` (rounded to the next full minute, avoiding the last 2 seconds of the current minute), then calls `ConfigurationTask(ConfigureAutoLogoffOrRestartTimeTask(...)).executeAsync()` to set the auto-restart time in TWS configuration. Responds `OK Restarting at HH:MM AM/PM` and closes the channel.
3. If the graphics environment supports window translucency, overlays a countdown timer on the main TWS window.

### ENABLEAPI

**Handler:** `CommandDispatcher.handleEnableAPICommand()` → `ConfigurationTask(new EnableApiTask(mChannel)).execute()`

Not available for IB Gateway (`ERROR ENABLEAPI is not valid for the IB Gateway`).

Execution path:

1. `ConfigurationTask.execute()` runs `ConfigTaskRunner` synchronously on the current (dispatcher) thread.
2. `ConfigTaskRunner` blocks until the TWS configuration dialog is available via `ConfigDialogManager.getConfigDialog()`.
3. `EnableApiTask` is wrapped in a `FutureTask` and submitted to `GuiExecutor` (which uses `SwingUtilities.invokeLater`), then `.get()` blocks the dispatcher thread until the EDT task completes.
4. Inside `EnableApiTask.run()` (on the EDT): navigates to the API settings panel, locates the "Enable ActiveX and Socket Clients" checkbox. If unchecked, clicks it and clicks OK, then responds `OK configured`. If already checked, responds `OK already configured`. On failure, responds `ERROR <reason>`.

### RECONNECTDATA

**Handler:** `CommandDispatcher.handleReconnectDataCommand()`

Not available for FIX Gateway (`ERROR RECONNECTDATA is not valid for the FIX Gateway`).

Directly synthesizes and dispatches three `KeyEvent` objects (KEY_PRESSED, KEY_TYPED, KEY_RELEASED) for the key `F` with modifiers `SHORTCUT | ALT` to the TWS main window (obtained with a 1 ms timeout). This simulates the keyboard shortcut for "Reconnect Data". Responds `OK` (empty body). The key events are dispatched on the dispatcher thread via `JFrame.dispatchEvent()`, not on the EDT.

### RECONNECTACCOUNT

**Handler:** `CommandDispatcher.handleReconnectAccountCommand()`

Not available for FIX Gateway (`ERROR RECONNECTACCOUNT is not valid for the FIX Gateway`).

Identical mechanism to RECONNECTDATA but dispatches the key `R` with the same modifiers, simulating the "Reconnect Account" shortcut. Uses `MainWindowManager.getMainWindow()` without a timeout (waits indefinitely). Responds `OK` (empty body).

### EXIT

**Handler:** inline in `CommandDispatcher.run()`

Responds `OK Goodbye` and breaks the command loop. The channel is then closed by `CommandDispatcher.run()`. The IBC process is not affected.

### Unknown command

Responds `ERROR Command invalid` and logs the unrecognized string. The loop continues — the connection stays open.

---

## 4. Network Security

### ControlFrom

After accepting a TCP connection, `CommandServer.getClient()` reads the `ControlFrom` setting (a comma-separated list of IP addresses or hostnames, default empty) and calls `isPermittedClient()`.

A client is permitted if **any** of the following is true:

1. The client's IP address equals the server socket's own local address (same host, non-loopback binding).
2. The client's IP address equals `InetAddress.getLoopbackAddress()` (i.e., `127.0.0.1` / `::1`). This means loopback connections are **always** allowed regardless of `ControlFrom`.
3. The client's IP address or hostname exactly matches one of the comma-separated entries in `ControlFrom`.

If the client is denied, the connection is closed immediately and a log message is written: `CommandServer denied access to: <address>`. No data is sent to the rejected client.

### BindAddress

If `BindAddress` is set in `config.ini`, the server socket binds exclusively to that address. This restricts which network interface accepts connections at the OS level. If `BindAddress` is empty (default), the server listens on all interfaces and logs the enumerated addresses at startup.

### Backlog

The server socket backlog is hardcoded to `5` pending connections.

---

## 5. Threading Model

### Why RECONNECTDATA and RECONNECTACCOUNT dispatch key events directly

These two commands synthesize `KeyEvent` objects and call `JFrame.dispatchEvent()` directly on the `CommandDispatcher` thread (a pool thread). They do not involve `GuiSynchronousExecutor` or `GuiExecutor`. The Swing `dispatchEvent` path on a non-EDT thread is technically unsafe, but in practice TWS processes these key events via its own event queue.

### Why ENABLEAPI uses GuiExecutor (invokeLater + FutureTask.get())

`EnableApiTask` interacts with Swing components (checkboxes, buttons) in the live TWS configuration dialog. Swing requires that all component mutations happen on the Event Dispatch Thread (EDT). `ConfigurationTask.ConfigTaskRunner` therefore:

1. Runs on a pool thread (the `CommandDispatcher` thread, since `execute()` is called synchronously).
2. Wraps `EnableApiTask` in a `FutureTask`.
3. Submits it via `GuiExecutor.execute()`, which calls `SwingUtilities.invokeLater()` if not already on the EDT.
4. Calls `FutureTask.get()`, which blocks the pool thread until the EDT has finished executing the task.

This makes the command synchronous from the client's perspective: the `OK` or `ERROR` response is only sent after the checkbox has been clicked and the dialog has been dismissed.

### GuiSynchronousExecutor vs GuiExecutor

`GuiSynchronousExecutor` uses `SwingUtilities.invokeAndWait()` — it blocks the calling thread until the EDT has run the task. `GuiExecutor` uses `SwingUtilities.invokeLater()` — it enqueues the task and returns immediately. In `ConfigurationTask`, the synchronous blocking is achieved by combining `invokeLater` with `FutureTask.get()`, which is functionally equivalent to `invokeAndWait`. `GuiSynchronousExecutor` itself is not used inside `ConfigurationTask`; it is a separate utility available for other uses where direct `invokeAndWait` semantics are needed without the `FutureTask` wrapper.

The key constraint is that `invokeAndWait` (and the equivalent pattern used here) **must not be called from the EDT itself**, as it would deadlock. `GuiSynchronousExecutor.execute()` guards against this by checking `SwingUtilities.isEventDispatchThread()` and running the task inline if already on the EDT.

### STOP and RESTART run on the dispatcher thread

`StopTask.run()` and `RestartTask.run()` are called directly (`// run on the current thread` comment in the source). They do not submit work to the EDT. `Utils.invokeMenuItem()` internally handles the EDT dispatch of the menu click.

---

## 6. Command-Line Usage

### commandsend.sh

`resources/commandsend.sh` is the general-purpose command sender. Configure `server_address` and `command_server_port` at the top of the script, then:

```bash
# Send any valid command
./commandsend.sh STOP
./commandsend.sh RESTART
./commandsend.sh ENABLEAPI
./commandsend.sh RECONNECTDATA
./commandsend.sh RECONNECTACCOUNT
```

Internally the script pipes to `telnet`:

```bash
(echo "$1"; sleep 1; echo "EXIT"; echo "quit") | telnet "$server_address" $command_server_port
```

The `sleep 1` gives the server time to process the command before `EXIT` is sent. The trailing `echo "quit"` exits `telnet` if the server has already closed the connection.

### stop.sh and restart.sh

Thin wrappers that delegate to `commandsend.sh`:

```bash
# Graceful TWS/Gateway shutdown
./stop.sh

# Restart TWS (not valid for FIX Gateway)
./restart.sh
```

### Direct telnet session

For interactive use or debugging:

```bash
telnet 127.0.0.1 7462
```

Expected session (with `SuppressInfoMessages=false`):

```text
INFO IBC Command Server
ENABLEAPI
OK configured
EXIT
OK Goodbye
Connection closed.
```

### Sending a single command with netcat

```bash
printf 'STOP\nEXIT\n' | nc 127.0.0.1 7462
```

---

## 7. Disabling the Command Server

Set `CommandServerPort=0` in `config.ini` (or omit the setting entirely, as `0` is the default). On startup, `CommandServer.run()` reads this value and logs:

```text
CommandServer is not started because the port is not configured
```

The thread exits immediately and no socket is opened.
