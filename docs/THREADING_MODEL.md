# IBC Threading Model

This document describes the concurrency architecture of IBC: which threads exist, what
each one does, how they communicate, and what rules must be respected to avoid deadlocks
and race conditions.

---

## 1. Thread Topology

```
┌─────────────────────────────────────────────────────────────────────────┐
│  JVM Process                                                            │
│                                                                         │
│  ┌────────────────┐    starts TWS/Gateway process,                      │
│  │  Main Thread   │    registers AWTEventListener (TwsListener),        │
│  │  (IbcTws /     │    sets UncaughtExceptionHandler,                   │
│  │   IbcGateway)  │    submits CommandServer to MyCachedThreadPool,     │
│  │                │    then blocks in TWS's own main loop               │
│  └────────────────┘                                                     │
│                                                                         │
│  ┌────────────────────────────────────────────────────────────────┐     │
│  │  Event Dispatch Thread  (EDT)                                  │     │
│  │                                                                │     │
│  │  - Runs all Swing/AWT event processing                         │     │
│  │  - Receives AWTEvent notifications → TwsListener.              │     │
│  │    eventDispatched() is called HERE                            │     │
│  │  - Executes Runnable tasks submitted via invokeLater /         │     │
│  │    invokeAndWait (GuiDeferredExecutor, GuiSynchronousExecutor, │     │
│  │    GuiExecutor)                                                │     │
│  │  - Runs all WindowHandler.handleWindow() logic                 │     │
│  │  - Must NEVER be blocked for long                              │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │  CommandServer Thread  (MyCachedThreadPool worker)          │        │
│  │                                                             │        │
│  │  - Listens on a TCP ServerSocket (blocking accept())        │        │
│  │  - For each accepted connection, submits a                  │        │
│  │    CommandDispatcher to MyCachedThreadPool                  │        │
│  │  - Terminated by shutdown() closing the ServerSocket        │        │
│  └─────────────────────────────────────────────────────────────┘        │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │  MyCachedThreadPool  (unbounded, Executors.newCachedThreadPool)│      │
│  │                                                             │        │
│  │  Workers used for:                                          │        │
│  │  - CommandServer (the accept loop itself)                   │        │
│  │  - CommandDispatcher (one per client connection)            │        │
│  │  - StopTask / RestartTask (triggered from EDT or scheduler) │        │
│  │  - ConfigurationTask                                        │        │
│  │  - exitWithError() shutdown sequence (Utils)                │        │
│  └─────────────────────────────────────────────────────────────┘        │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────┐        │
│  │  MyScheduledExecutorService  (1 thread, newScheduledThreadPool)│     │
│  │                                                             │        │
│  │  Workers used for:                                          │        │
│  │  - Login dialog display timeout watchdog (SessionManager)   │        │
│  │  - Login retry / auto-login delay (AbstractLoginHandler)    │        │
│  │  - Login failure shutdown timer (LoginManager)              │        │
│  │  - ColdRestartTime countdown (IbcTws)                       │        │
│  │  - TooManyFailedLoginAttempts retry delay                   │        │
│  │  - TwsSettingsSaver periodic save (scheduleAtFixedRate)     │        │
│  │  - RestartTask polling loop (scheduleAtFixedRate)           │        │
│  └─────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────┘
```

### Thread interaction summary

```
  AWTEvent (system)
        │
        ▼
  TwsListener.eventDispatched()   ← runs on EDT
        │
        │  GuiDeferredExecutor (invokeLater)
        ▼
  EDT task queue  ──►  WindowHandler.handleWindow()
                              │
                  ┌───────────┴───────────┐
                  │                       │
          GuiSynchronousExecutor   MyCachedThreadPool
          (invokeAndWait — for     (StopTask, RestartTask,
           result needed now)       ConfigurationTask …)


  MyScheduledExecutorService
        │
        ├─► GuiExecutor.execute()  ──►  EDT  (if not already on it)
        └─► MyCachedThreadPool.execute(StopTask / RestartTask)
```

---

## 2. GuiDeferredExecutor vs GuiSynchronousExecutor

### GuiDeferredExecutor — fire-and-forget (`invokeLater`)

```java
// GuiDeferredExecutor.java
public void execute(Runnable task) {
    SwingUtilities.invokeLater(task);
}
```

- Posts `task` to the EDT event queue and **returns immediately**.
- The calling thread never waits for the task to complete.
- Zero risk of deadlock.
- Used when the caller does not need a result from the GUI operation and can
  continue working (or exit) before the task runs.

**Where it is used:** `TwsListener.eventDispatched()` wraps every window-event
handler in a `GuiDeferredExecutor` call. This is critical: `eventDispatched()`
itself is called on the EDT, but submitting a _new_ `invokeLater` task from
within the EDT defers the heavier handler logic to the _next_ pass of the event
loop, keeping the listener lean and non-blocking.

### GuiSynchronousExecutor — blocking (`invokeAndWait`)

```java
// GuiSynchronousExecutor.java
public void execute(Runnable task) {
    if (SwingUtilities.isEventDispatchThread()) {
        task.run();                          // already on EDT: run directly
    } else {
        SwingUtilities.invokeAndWait(task);  // off-EDT: block until done
    }
}
```

- If called from a non-EDT thread, **blocks the caller** until `task` finishes
  on the EDT.
- If called from the EDT itself, runs `task` synchronously in-place (no queue
  entry, no wait needed).
- Use when the calling thread needs the GUI operation to complete before it can
  proceed — for example, reading a Swing component's current value.

### GuiExecutor — conditional defer

```java
// GuiExecutor.java
public void execute(Runnable task) {
    if (SwingUtilities.isEventDispatchThread()) {
        task.run();
    } else {
        SwingUtilities.invokeLater(task);
    }
}
```

A third variant: runs inline on EDT, defers via `invokeLater` from off-EDT.
Unlike `GuiSynchronousExecutor`, it never blocks the caller.

### Deadlock risk with `invokeAndWait`

`SwingUtilities.invokeAndWait()` **must never be called from the EDT itself**.
`GuiSynchronousExecutor` guards against this with the `isEventDispatchThread()`
check. However, a deadlock can still occur if:

1. A background thread holds a lock L and calls `GuiSynchronousExecutor.execute()`.
2. The submitted task, running on the EDT, tries to acquire L.

Result: the background thread waits for the EDT; the EDT waits for L; both
block forever.

**Rule:** do not hold application-level locks while calling
`GuiSynchronousExecutor.execute()`.

### Choosing the right executor

| Situation                                    | Executor to use                                       |
| -------------------------------------------- | ----------------------------------------------------- |
| Reacting to an AWTEvent (already on EDT)     | `GuiDeferredExecutor` — defers to next EDT cycle      |
| Background thread needs GUI result           | `GuiSynchronousExecutor` — blocks until EDT runs task |
| Background thread fires-and-forgets to GUI   | `GuiExecutor` or `GuiDeferredExecutor`                |
| Code may run on or off EDT, no return needed | `GuiExecutor`                                         |

---

## 3. SessionManager: Synchronization with Lock / Condition

### Problem context

For Interactive Brokers Gateway, the main application window (`JFrame`) is
loaded very early during startup, but its menus remain unresponsive until an
internal initialization completes. That completion is signalled by two
independent observable events:

1. The splash screen (a frame titled `"Starting application..."`) closes.
2. A non-brokerage account dialog (if any) closes.

IBC must not attempt to drive the Gateway menus before _both_ events have
occurred. Any thread that needs to interact with the Gateway configuration must
therefore wait for this combined signal.

### Synchronization structure

```
static final Lock      lock        = new ReentrantLock();
static final Condition initialised = lock.newCondition();

static volatile boolean _InitialisationCompleted      = false;
static volatile boolean splashScreenClosed            = false;
static volatile boolean nonBrokerageAccountDialogClosed = false;
```

### `awaitReady()` — the waiter

```java
static void awaitReady() {
    lock.lock();
    try {
        while (!_InitialisationCompleted) {
            initialised.await();   // releases lock, suspends thread
        }
    } finally {
        lock.unlock();
    }
}
```

Called by any thread (typically a background worker initiated from a
`WindowHandler`) that needs the Gateway to be fully initialized before
proceeding. The `while (!condition)` loop is mandatory: `await()` can return
spuriously, so the predicate must be re-evaluated on every wakeup.

### `setSplashScreenClosed()` and `setNonBrokerageAccountDialogClosed()` — the signalers

```java
static void setSplashScreenClosed() {
    if (!SessionManager.isGateway()) return;
    lock.lock();
    try {
        splashScreenClosed = true;
        if (nonBrokerageAccountDialogClosed) {
            _InitialisationCompleted = true;
            initialised.signal();
        }
    } finally { lock.unlock(); }
}

static void setNonBrokerageAccountDialogClosed() {
    if (!SessionManager.isGateway()) return;
    lock.lock();
    try {
        nonBrokerageAccountDialogClosed = true;
        if (splashScreenClosed) {
            _InitialisationCompleted = true;
            initialised.signal();
        }
    } finally { lock.unlock(); }
}
```

These are called from EDT-based `WindowHandler` implementations when the
corresponding windows are detected closing. The signal is only sent when
**both** flags are true — whichever event arrives last triggers the wakeup.

### `setMainWindow()` — TWS (non-Gateway) path

```java
static void setMainWindow(JFrame window) {
    lock.lock();
    try {
        _InitialisationCompleted = true;
        initialised.signal();
    } finally {
        lock.unlock();
        MainWindowManager.mainWindowManager().setMainWindow(window);
    }
}
```

For TWS (as opposed to Gateway), there is no splash screen ceremony. A single
event — the detection of the main window — is enough to release all waiters.

### Why `volatile` fields in addition to the lock?

`_InitialisationCompleted`, `splashScreenClosed`, and
`nonBrokerageAccountDialogClosed` are declared `volatile`. Since all writes
happen inside `lock.lock()` / `lock.unlock()`, the lock alone would be
sufficient for visibility between threads. The `volatile` annotation provides a
belt-and-suspenders guarantee in case the condition variable path is bypassed or
the field is read without the lock in a future change.

### Sequence diagram (Gateway startup)

```
EDT                          Background Thread         ScheduledExecutor
 │                                │                          │
 │  WindowHandler detects         │                          │
 │  splash screen close           │                          │
 │  setSplashScreenClosed()       │                          │
 │  (nonBrokerageClosed=false     │                          │
 │   → no signal yet)             │                          │
 │                                │  awaitReady()            │
 │                                │  lock → await() →        │
 │                                │  suspended               │
 │  WindowHandler detects         │                          │
 │  non-brokerage dialog close    │                          │
 │  setNonBrokerageAccountDialog  │                          │
 │  Closed()                      │                          │
 │  (splashClosed=true            │                          │
 │   → _InitialisationCompleted   │                          │
 │   → signal())                  │                          │
 │                                │  wakes up, re-checks     │
 │                                │  _InitialisationCompleted│
 │                                │  = true → proceeds       │
```

---

## 4. SwitchLock: Idempotent Execution Guard

### Definition

```java
class SwitchLock {
    private boolean mIsSet;

    synchronized void  clear()  { mIsSet = false; }
    synchronized boolean set()  { if (mIsSet) return false; mIsSet = true; return true; }
    synchronized boolean query() { return mIsSet; }
}
```

`SwitchLock` is a simple boolean mutex with compare-and-set semantics. `set()`
atomically checks and sets the flag: it returns `true` on the first call (the
caller "wins" and may proceed) and `false` on any subsequent call (a concurrent
runner is rejected).

### Usage pattern

`SwitchLock` is used as a **per-class static field** in `StopTask` and
`RestartTask` to guarantee that only one instance of each task runs at a time,
regardless of how many threads trigger it concurrently:

```java
// StopTask.java and RestartTask.java
private static final SwitchLock _Running = new SwitchLock();

@Override
public void run() {
    if (!_Running.set()) {
        Utils.logToConsole("STOP already in progress");
        writeNack("STOP already in progress");
        mChannel.close();
        return;                  // reject: another instance is running
    }
    // ... only one invocation reaches this point ...
}
```

The lock is _never cleared_ after being set. This is intentional: once a stop
or restart sequence has started, no second attempt should ever be allowed.
`clear()` exists in the API but is not called in the production paths for
`StopTask` / `RestartTask`.

### Why not `AtomicBoolean`?

`SwitchLock` predates the common use of `AtomicBoolean.compareAndSet()` in this
codebase, and it wraps the three operations (check, set, clear) in a single
named abstraction, which improves readability at call sites.

---

## 5. MyCachedThreadPool vs MyScheduledExecutorService

Both are JVM-wide singletons, created once at class-load time and shared across
the entire IBC process.

### MyCachedThreadPool

```java
private static final ExecutorService instance = Executors.newCachedThreadPool();
```

- **Thread count:** unbounded. Idle threads are reused; new threads are created
  on demand if none are free; idle threads are terminated after 60 seconds.
- **Task type:** short-lived or I/O-bound tasks where latency matters more
  than limiting concurrency.

| Usage site                 | Task submitted                              |
| -------------------------- | ------------------------------------------- |
| `IbcTws`                   | `CommandServer` (the accept loop)           |
| `CommandServer`            | `CommandDispatcher` (per client connection) |
| `AbstractLoginHandler`     | `StopTask` on login error                   |
| `ConfigurationTask`        | `ConfigTaskRunner`                          |
| `GatewayDialogHandler`     | `StopTask` on connection failure            |
| `LoginErrorDialogHandler`  | `StopTask`                                  |
| `LoginFailedDialogHandler` | `StopTask`                                  |
| `Utils.exitWithError()`    | Anonymous shutdown runnable                 |

### MyScheduledExecutorService

```java
private static final ScheduledExecutorService instance = Executors.newScheduledThreadPool(1);
```

- **Thread count:** exactly 1 thread. Tasks run sequentially; a long-running
  task will delay subsequent scheduled tasks.
- **Task type:** time-delayed or periodic triggers. Tasks are typically short
  (they submit other work to the EDT or to `MyCachedThreadPool` rather than
  doing heavy work themselves).

| Usage site                                | Schedule type             | Purpose                                       |
| ----------------------------------------- | ------------------------- | --------------------------------------------- |
| `SessionManager.startSession()`           | `schedule` (one-shot)     | Exit if login dialog not shown within timeout |
| `AbstractLoginHandler`                    | `schedule` (one-shot, ×2) | Re-attempt login after a delay                |
| `LoginManager`                            | `schedule` (one-shot, ×2) | Auto-restart after login failure window       |
| `IbcTws`                                  | `schedule` (one-shot)     | ColdRestartTime countdown                     |
| `TooManyFailedLoginAttemptsDialogHandler` | `schedule` (one-shot)     | Retry delay                                   |
| `TwsSettingsSaver`                        | `scheduleAtFixedRate`     | Periodic settings file save                   |
| `RestartTask`                             | `scheduleAtFixedRate`     | Poll TWS readiness during restart             |

### Shutdown

`ShutdownProgressDialogHandler` calls `shutdownNow()` on **both** executors
when the TWS/Gateway shutdown progress dialog appears, ensuring no scheduled or
cached-pool tasks continue running after the application begins exiting.

---

## 6. UncaughtExceptionHandler

### Registration

Both entry points register the handler at startup, but only if no handler has
already been installed (guards against double-registration when IBC is loaded
into an already-running JVM):

```java
// IbcTws.java and IbcGateway.java
if (Thread.getDefaultUncaughtExceptionHandler() == null) {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
}
```

### Behaviour

```java
public final class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable t) {
        Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, t);
    }
}
```

`Utils.exitWithException()` does three things in sequence:

1. **Logs** the exception to stderr (with a prominent visual separator). If the
   `IncludeStackTraceForExceptions` setting is `true`, prints the full stack
   trace; otherwise prints only the message.
2. Logs the exit code.
3. Calls `System.exit(ErrorCodes.UNHANDLED_EXCEPTION)`, terminating the entire
   JVM immediately.

This means **any unhandled exception on any thread — including EDT, scheduler
threads, and pool workers — results in a hard JVM exit**. There is no partial
recovery path.

### In-line exception handling in TwsListener

`TwsListener.eventDispatched()` explicitly catches `Throwable` at two levels
and forwards to `Utils.exitWithException()` directly, bypassing the
`UncaughtExceptionHandler`. This is because Swing silently swallows exceptions
thrown from `AWTEventListener` implementations; the explicit catch ensures no
exception is lost.

```java
GuiDeferredExecutor.instance().execute(() -> {
    try {
        // ... handler logic ...
    } catch (Throwable e) {
        Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
    }
});
```

---

## 7. Threading Rules

### Rule 1 — Never block the EDT

The EDT must remain free to process events at all times. Any operation that
could block (network I/O, file I/O, `Lock.lock()` with contention,
`Thread.sleep()`, `invokeAndWait()` from another EDT task) must be offloaded to
`MyCachedThreadPool` or `MyScheduledExecutorService`.

**Violation symptoms:** TWS/Gateway UI freezes; dialogs do not repaint; IBC
window handlers time out.

### Rule 2 — Never access Swing components from a non-EDT thread

All reads and writes of Swing component state (text, selection, visibility,
enabled state) must happen on the EDT. Use one of:

| Need                              | Mechanism                                         |
| --------------------------------- | ------------------------------------------------- |
| Fire-and-forget (no return value) | `GuiDeferredExecutor.instance().execute(task)`    |
| Need result before continuing     | `GuiSynchronousExecutor.instance().execute(task)` |
| Safe from either thread           | `GuiExecutor.instance().execute(task)`            |

**Violation symptoms:** intermittent `NullPointerException`, wrong component
state, invisible dialogs, race conditions that only appear under load.

### Rule 3 — Never call `invokeAndWait` while holding an application lock

If a background thread holds a lock and calls `GuiSynchronousExecutor.execute()`
(which calls `invokeAndWait`), and the EDT task attempts to acquire the same
lock, a deadlock results. Neither thread can proceed.

**Safe pattern:**

```java
// Release the lock before dispatching to EDT synchronously
lock.unlock();
GuiSynchronousExecutor.instance().execute(() -> { /* GUI work */ });
lock.lock();
```

### Rule 4 — Do not call `awaitReady()` from the EDT

`SessionManager.awaitReady()` blocks the calling thread indefinitely until
Gateway initialization is complete. Calling it from the EDT would freeze the
entire UI, and since the completion signal is itself delivered via the EDT
(through `WindowHandler` events), the call would deadlock.

### Rule 5 — Use `SwitchLock` for idempotent tasks

Any task that must not run concurrently with itself (stop, restart) must guard
its entry point with a `SwitchLock`. Call `set()` as the very first statement;
return immediately if it returns `false`.

### Rule 6 — Keep ScheduledExecutor tasks short

`MyScheduledExecutorService` has exactly one thread. A long-running scheduled
task blocks all subsequent scheduled tasks. If a scheduled callback needs to do
non-trivial work, it should submit that work to `MyCachedThreadPool` and return
quickly.

```java
// Correct pattern
MyScheduledExecutorService.getInstance().schedule(() -> {
    MyCachedThreadPool.getInstance().execute(new StopTask(...));
}, delay, TimeUnit.SECONDS);
```
