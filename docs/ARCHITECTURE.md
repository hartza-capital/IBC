# Architecture d'IBC — Comment automatiser une application Java Swing

## Le problème fondamental

TWS (Trader Workstation) et IB Gateway sont des applications Java Swing fermées. Elles
n'exposent aucune API d'automatisation, aucun webhook, aucune interface programmatique.
La seule façon d'interagir avec elles, normalement, c'est un humain devant un écran.

IBC résout ce problème en exploitant une propriété fondamentale du runtime Java : **tout ce
qui tourne dans la même JVM partage le même espace d'événements graphiques.**

---

## 1. La même JVM — l'insight central

IBC ne lance pas TWS comme un processus fils (`ProcessBuilder`, `Runtime.exec`). Il appelle
directement le `main()` de TWS dans le **même processus Java** :

```java
// IbcTws.java, méthode startTws()
jclient.LoginFrame.main(twsArgs);

// IbcGateway.java, méthode startGateway()
ibgateway.GWClient.main(twsArgs);
```

Ces classes (`jclient.LoginFrame`, `ibgateway.GWClient`) viennent des jars TWS/Gateway,
qui sont sur le classpath au moment de la compilation (variable `IBC_BIN`). C'est pourquoi
cette variable est obligatoire pour builder IBC.

**Conséquence directe :** IBC et TWS partagent :

- La même heap JVM
- Le même `Toolkit` AWT (gestionnaire d'événements graphiques)
- La même EDT (Event Dispatch Thread)
- Le même `SecurityManager` (ou son absence)

IBC peut donc interagir avec les fenêtres de TWS exactement comme TWS lui-même le ferait.

---

## 2. Le hook d'événements AWT

Une fois dans la même JVM, IBC installe un listener global sur le `Toolkit` AWT — le
gestionnaire central de tous les événements graphiques de la JVM :

```java
// IbcTws.java, méthode createToolkitListener()
Toolkit.getDefaultToolkit().addAWTEventListener(
    new TwsListener(createWindowHandlers()),
    AWTEvent.WINDOW_EVENT_MASK
);
```

`addAWTEventListener` est une API publique et officielle de Java (`java.awt.Toolkit`).
Elle permet d'intercepter **tous** les événements fenêtre de **toutes** les fenêtres
de la JVM, sans distinction d'origine. Quand TWS ouvre sa fenêtre de login, IBC le sait.

Le masque `AWTEvent.WINDOW_EVENT_MASK` filtre sur les événements de type fenêtre :
`WINDOW_OPENED`, `WINDOW_ACTIVATED`, `WINDOW_CLOSED`, `WINDOW_CLOSING`, etc.

---

## 3. Le TwsListener — dispatcher central

`TwsListener` implémente `AWTEventListener`. Sa méthode `eventDispatched` reçoit chaque
événement fenêtre :

```java
// TwsListener.java
@Override
public void eventDispatched(AWTEvent event) {
    final int eventID = event.getID();
    final Window window = ((WindowEvent) event).getWindow();

    GuiDeferredExecutor.instance().execute(() -> {
        for (WindowHandler wh : windowHandlers) {
            if (wh.recogniseWindow(window)) {
                if (wh.filterEvent(window, eventID)) {
                    wh.handleWindow(window, eventID);
                }
                return;
            }
        }
    });
}
```

Points importants :

1. **`eventDispatched` est appelé sur l'EDT** — le thread Swing principal. Le traitement
   est immédiatement délégué via `GuiDeferredExecutor` (un `SwingUtilities.invokeLater`)
   pour éviter de bloquer l'EDT pendant la reconnaissance et l'interaction.

2. **La liste des handlers est parcourue dans l'ordre** — le premier `recogniseWindow()`
   qui retourne `true` gagne. L'ordre dans `createWindowHandlers()` est donc significatif
   (voir la note dans le code : `SecondFactorAuthenticationDialogHandler` doit précéder
   `SecurityCodeDialogHandler` car ils partagent un bouton identique).

3. **Deux étapes séparées** : `recogniseWindow` (est-ce que c'est la bonne fenêtre ?) et
   `filterEvent` (est-ce le bon événement pour agir ?). Un handler peut reconnaître une
   fenêtre sans agir à chaque événement.

---

## 4. Le modèle WindowHandler

Tous les handlers implémentent l'interface `WindowHandler` :

```java
interface WindowHandler {
    boolean recogniseWindow(Window window);  // identifier la fenêtre
    boolean filterEvent(Window window, int eventId);  // filtrer les événements
    void handleWindow(Window window, int eventID);  // agir
}
```

### Comment `recogniseWindow` fonctionne

TWS n'expose pas ses classes internes. IBC identifie les fenêtres par leur structure
visible : titre et composants présents. Par exemple, pour la fenêtre de login TWS :

```java
// LoginFrameHandler.java (simplifié)
@Override
public boolean recogniseWindow(Window window) {
    if (!(window instanceof JFrame)) return false;
    return SwingUtils.titleContains(window, "Login")
        && SwingUtils.findButton(window, "Login") != null;
}
```

La combinaison titre + présence de composants spécifiques identifie la fenêtre de façon
robuste même quand le titre seul ne suffit pas.

### Comment `handleWindow` agit

Une fois la fenêtre identifiée, IBC parcourt son arbre de composants Swing et interagit
directement avec eux :

```java
// SwingUtils.java — cliquer sur un bouton par son texte
static boolean clickButton(final Window window, final String buttonText) {
    final JButton button = findButton(window, buttonText);
    if (button == null) return false;
    button.doClick();  // équivalent à un clic humain
    return true;
}

// Remplir un champ texte
static JTextField findTextField(Container container, int index) { ... }
```

`doClick()` est une méthode standard de `JButton` qui déclenche les mêmes `ActionEvent`
qu'un vrai clic souris — pour TWS, c'est indiscernable d'une interaction humaine.

---

## 5. La traversée de l'arbre de composants

`ComponentIterator` effectue un parcours en profondeur (DFS) de l'arbre de composants
Swing. Chaque `Container` contient des `Component[]` accessibles via `getComponents()`.

```java
// ComponentIterator.java — parcours DFS avec stack explicite
private class NodeState {
    NodeState(Component component) {
        synchronized (component.getTreeLock()) {  // verrou Swing obligatoire
            if (component instanceof Container)
                subComponents = ((Container)component).getComponents();
        }
    }
}
```

Note critique : `component.getTreeLock()` est le verrou interne de Swing pour la
modification de l'arbre de composants. Le prendre en lecture est la façon correcte
d'itérer sans risque de `ConcurrentModificationException` pendant qu'une autre partie
du code modifie l'UI.

---

## 6. Le modèle de threads

```text
Main thread
    └── IbcTws.main()
            ├── startCommandServer()     → thread "CommandServer" (TCP)
            ├── createToolkitListener()  → enregistre TwsListener sur Toolkit
            └── jclient.LoginFrame.main() → démarre TWS → crée l'EDT

EDT (Event Dispatch Thread)  ← thread unique Swing
    ├── Reçoit tous les AWTEvent de TWS
    ├── TwsListener.eventDispatched()  ← appelé ici
    │       └── GuiDeferredExecutor.execute(handler)
    │               └── SwingUtilities.invokeLater(handler)  ← reposte sur l'EDT
    └── Handler s'exécute ici : doClick(), setText(), etc.

Thread "CommandServer"
    └── Accepte connexions TCP
            └── CommandDispatcher → GuiSynchronousExecutor
                    └── SwingUtilities.invokeAndWait()  ← bloque jusqu'à exécution sur EDT
```

**Pourquoi `invokeLater` dans `eventDispatched` ?**

`eventDispatched` est appelé pendant le dispatch d'un événement AWT — potentiellement
au milieu d'un traitement TWS. Exécuter un handler directement ici bloquerait l'EDT
et pourrait causer des deadlocks. `invokeLater` place le handler dans la queue de l'EDT
pour exécution **après** que l'événement courant est traité.

**`GuiSynchronousExecutor` vs `GuiDeferredExecutor` :**

| Classe                   | Méthode Swing   | Usage                                            |
| ------------------------ | --------------- | ------------------------------------------------ |
| `GuiDeferredExecutor`    | `invokeLater`   | Handlers de fenêtres (asynchrone, ne bloque pas) |
| `GuiSynchronousExecutor` | `invokeAndWait` | Commandes TCP (doit attendre le résultat)        |

---

## 7. La séquence de démarrage complète

```text
IbcTws.main(args)
    │
    ├─ 1. setupDefaultEnvironment()
    │       ├─ SessionManager.initialise()
    │       ├─ Settings.initialise(new DefaultSettings(args))  ← lit config.ini
    │       ├─ LoginManager.initialise(new DefaultLoginManager(args))
    │       └─ TradingModeManager.initialise()
    │
    ├─ 2. load()
    │       ├─ startCommandServer()           → thread TCP démarre
    │       ├─ startShutdownTimerIfRequired() → schedule StopTask si ClosedownAt configuré
    │       ├─ createToolkitListener()        → TwsListener installé sur Toolkit
    │       ├─ startSavingTwsSettingsAutomatically()
    │       └─ startTwsOrGateway()
    │               ├─ JtsIniManager.initialise()  ← prépare jts.ini
    │               └─ jclient.LoginFrame.main()   ← TWS démarre, crée ses fenêtres
    │
    └─ À partir de là, TWS tourne et IBC réagit aux événements
            │
            ├─ TWS ouvre LoginFrame
            │       → TwsListener reçoit WINDOW_OPENED
            │       → LoginFrameHandler.recogniseWindow() → true
            │       → LoginFrameHandler.handleWindow() → remplit username/password, clique Login
            │
            ├─ TWS affiche dialog 2FA
            │       → SecondFactorAuthenticationDialogHandler
            │       → génère code TOTP (classes internes Totp/Base32) ou attend IBKR Mobile
            │
            └─ TWS ouvre MainWindow
                    → MainWindowFrameHandler détecte la connexion établie
                    → Configure*Task s'exécutent (port API, master client ID, etc.)
```

---

## 8. Pourquoi cette approche fonctionne uniquement avec TWS

| Application                | Technologie UI | Possible d'automatiser comme IBC ?                  |
| -------------------------- | -------------- | --------------------------------------------------- |
| TWS / IB Gateway           | Java Swing     | Oui — même JVM, AWTEventListener global             |
| Application native Windows | Win32/GDI      | Non — nécessite FindWindow/SendMessage (WinAPI)     |
| Application Electron       | Chromium/JS    | Non — nécessiterait DevTools Protocol ou Playwright |
| Autre app Java Swing       | Java Swing     | Oui — même technique applicable                     |

L'ouverture totale du runtime Java (pas de sandbox entre bibliothèques du même processus,
arbre Swing entièrement accessible, Toolkit global) est ce qui rend IBC possible sans
aucun patch de TWS ni accès au code source de TWS.

---

## 9. La robustesse face aux mises à jour de TWS

Le point fragile d'IBC est la reconnaissance des fenêtres dans `recogniseWindow()` :
si Interactive Brokers renomme un bouton ou change le titre d'un dialog, le handler
cesse de fonctionner.

Les stratégies de robustesse dans le code :

- **Ne pas dépendre uniquement du titre** : combiner titre + présence de composants
  spécifiques (`findButton("Login") != null`)
- **`LogStructureScope` / `LogStructureWhen`** : paramètres de debug dans `config.ini`
  qui font logguer la structure complète de l'arbre Swing de chaque fenêtre — permet
  d'identifier comment reconnaître une nouvelle fenêtre après une mise à jour TWS
- **`GatewayDialogHandler`** : handler générique de dernier recours pour les dialogs
  Gateway inconnus, en bas de la liste des handlers

La liste `createWindowHandlers()` dans `IbcTws.java` est le registre exhaustif de tout
ce qu'IBC sait gérer. Toute nouvelle fenêtre TWS nécessite d'y ajouter un handler.

---

## 10. Configuration Tasks

### Why programmatic configuration is needed

After login TWS is running with whatever settings were last saved to disk. For unattended
operation, IBC must ensure that the API socket is enabled, the port matches what trading
clients expect, and optional precautions (bond warnings, volatility warnings, etc.) are
in the correct state. TWS offers no CLI flags or config-file overrides for these settings:
the only interface is the **Global Configuration** dialog, which is normally opened by a
human from the menu. IBC opens and drives this dialog programmatically after every login.

### The ConfigDialogManager abstraction

`ConfigDialogManager` is a singleton that acts as a shared handle to the Global
Configuration dialog. Any code that needs to modify TWS settings calls
`ConfigDialogManager.configDialogManager().getConfigDialog()` and receives the live
`JDialog` reference, blocking until the dialog is ready.

The default implementation is `DefaultConfigDialogManager`, which coordinates concurrent
callers:

- It maintains a **usage counter** (`usageCount`). Each caller to `getConfigDialog()`
  increments it; each call to `releaseConfigDialog()` decrements it.
- When the counter reaches zero and the dialog was **not** opened by the user, the manager
  automatically clicks the **OK** button to close the dialog, then iconizes the main window
  if required.
- If a caller changes the API port while the "Enable ActiveX and Socket Clients" checkbox
  is already checked, TWS will show a confirmation prompt. The manager tracks this
  expectation via `setApiConfigChangeConfirmationExpected()` so the confirmation handler
  can dismiss it correctly.

### GetConfigDialogTask — opening the dialog

`GetConfigDialogTask` implements `Callable<JDialog>` and runs on a dedicated single-thread
executor. Its `call()` method:

1. Retrieves the main TWS/Gateway window via `MainWindowManager`.
2. Calls `SessionManager.awaitReady()` — blocks until the splash screen has disappeared
   and all initialisation is complete (critical for Gateway, where menus are unresponsive
   during startup even after the window appears).
3. Invokes the appropriate menu item to open the dialog:
   - Gateway: `Configure > Settings`
   - TWS Classic layout: `Edit > Global Configuration...`
   - TWS Mosaic layout: `File > Global Configuration...`
4. Waits on a `ReentrantLock` / `Condition` pair until `setConfigDialog()` is called by
   the AWT event handler that detects the dialog opening (`WINDOW_OPENED` event).

```java
// GetConfigDialogTask.java — simplified flow
if (SessionManager.isGateway()) {
    Utils.invokeMenuItem(mainForm, new String[] {"Configure", "Settings"});
} else if (!Utils.invokeMenuItem(mainForm, new String[] {"Edit", "Global Configuration..."})) {
    Utils.invokeMenuItem(mainForm, new String[] {"File", "Global Configuration..."});
}
// then blocks until the dialog's WINDOW_OPENED event signals the condition
```

### Configure\*Task — the actual settings changes

Each `ConfigurationAction` implementation receives the dialog reference via `initialise()`
and then performs its specific settings changes in `run()`. The tasks that run after a
normal login are:

| Task                          | What it does                                                                                                                                                                                                      |
| ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ConfigureTwsApiPortTask`     | Navigates to `API > Settings`, finds the "Socket port" text field, and sets it to the configured port number. Sets `apiConfigChangeConfirmationExpected` if the API was already enabled and the port is changing. |
| `ConfigureApiPrecautionsTask` | Navigates to `API > Precautions`, finds each precaution checkbox by its label text, and sets it to `yes` / `no` / leaves it unchanged (empty string).                                                             |
| `EnableApiTask`               | Finds the "Enable ActiveX and Socket Clients" checkbox; if not already checked, clicks it and immediately closes the dialog. Used by the `ENABLEAPI` TCP command rather than the startup sequence.                |

Navigation inside the dialog (e.g. `Utils.selectApiSettings()`, `Utils.selectConfigSection()`)
works by clicking on tree nodes in the left-panel navigation tree of the Global Configuration
dialog — the same way a human would expand "API" and then click "Settings".

### Execution sequence after login

```text
MainWindowFrameHandler.handleWindow(WINDOW_OPENED)
    └─ SessionManager.setMainWindow()          ← signals awaitReady()
            └─ triggers Configure*Task chain
                    ├─ DefaultConfigDialogManager.getConfigDialog()
                    │       ├─ submits GetConfigDialogTask on executor thread
                    │       │       └─ invokes menu → dialog opens → WINDOW_OPENED event
                    │       │               └─ ConfigDialogHandler.setConfigDialog() → signals condition
                    │       └─ returns JDialog to each waiting task
                    ├─ ConfigureTwsApiPortTask.run()
                    ├─ ConfigureApiPrecautionsTask.run()
                    └─ releaseConfigDialog() × N
                            └─ usageCount == 0 → click OK → close dialog → iconize
```

---

## 11. Session Lifecycle

### Session state flags

`SessionManager` holds the key flags that shape IBC's behaviour throughout a session:

| Flag        | Source                                                                | Effect                                                                                                                                                                                           |
| ----------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `isGateway` | Set at startup via `SessionManager.initialise(true/false)`            | Selects the correct menu paths (`Configure > Settings` vs `Edit/File > Global Configuration`), controls splash-screen handling, and changes the shutdown menu (`File > Close` vs `File > Exit`). |
| `isFIX`     | Read from `config.ini` key `FIX=true/false` at `startSession()`       | Determines whether IBC is controlling a FIX session (Order Management System mode) rather than a standard TWS/Gateway session.                                                                   |
| `isRestart` | Detected from the JVM system property `-Drestart` at `startSession()` | When set, IBC knows TWS will re-establish the previous session automatically without showing a login dialog. No login automation is attempted.                                                   |

### The `awaitReady()` barrier

Before any code tries to access menus or configuration, it must call
`SessionManager.awaitReady()`. This call blocks on a `ReentrantLock` / `Condition` until
`_InitialisationCompleted` is set to `true`.

For **TWS**, initialisation completes as soon as `MainWindowFrameHandler` sees the main
window open (`setMainWindow()` signals the condition directly).

For **Gateway**, two independent events must both arrive before the condition is signalled:

1. The splash screen (`"Starting application..."` frame) closes — `setSplashScreenClosed()`.
2. The NonBrokerageAccount dialog closes (if shown) — `setNonBrokerageAccountDialogClosed()`.

This two-gate design prevents `GetConfigDialogTask` from invoking the `Configure > Settings`
menu before Gateway has finished loading, which would cause a logged exception inside
Gateway even though the menu item is already visible.

### Daily auto-restart

TWS and Gateway perform a mandatory daily restart at a configurable time (typically around
23:45 ET). This restart is entirely managed by TWS itself: it closes the current session
and relaunches the application. IBC does not orchestrate this restart — it simply stays
resident in the JVM and waits for TWS to go through its normal window sequence again.

From IBC's perspective, the auto-restart looks identical to the initial startup:

```text
TWS daily restart
    ├─ TWS closes its windows (IBC detects WINDOW_CLOSING / WINDOW_CLOSED events)
    ├─ TWS reopens the login dialog (if credentials have expired) or skips directly to main window
    │       → LoginFrameHandler handles credentials entry if needed
    │       → 2FA dialog may reappear → SecondFactorAuthenticationDialogHandler
    └─ TWS reopens main window
            → MainWindowFrameHandler triggers Configure*Task chain again
```

If the restart is a **cold restart** (forced via IBC's `STOP` command with
`ForceColdRestart=yes`), IBC creates a `COLDRESTART<sessionid>` flag file in the
`jtsConfigDir` before exiting. The external wrapper script reads this file to decide
whether to re-launch IBC from scratch rather than waiting for TWS's internal restart.

### Login timeout watchdog

At `startSession()`, if `isRestart` is false, IBC schedules a watchdog that fires after
`LoginDialogDisplayTimeout` seconds (default 60). If the login dialog has not been shown
by then (`loginState == LOGGED_OUT`), IBC exits with error code
`LOGIN_DIALOG_DISPLAY_TIMED_OUT`. This prevents IBC from hanging indefinitely if TWS
fails to start.

---

## 12. Edge Cases

### NonBrokerageAccount dialog (paper trading warning)

When a paper-trading account is used with IB Gateway, Gateway displays a dialog with the
label `"This is not a brokerage account"` immediately after login. This dialog must be
dismissed before the Gateway menus become responsive.

`NonBrokerageAccountDialogHandler` handles two events:

- `WINDOW_OPENED`: if `AcceptNonBrokerageAccountWarning=true` (the default), clicks
  `"I understand and accept"` to dismiss the dialog automatically. Also schedules an
  iconize of the main window.
- `WINDOW_CLOSED`: calls `SessionManager.setNonBrokerageAccountDialogClosed()`, which
  contributes to the two-gate `awaitReady()` barrier described above.

If `AcceptNonBrokerageAccountWarning=false`, the handler does nothing on `WINDOW_OPENED`
and the dialog stays open, blocking `GetConfigDialogTask` until a human dismisses it. This
is intentional for users who want to confirm the paper-trading warning manually.

The recognition is purely label-based (no title check, since Gateway does not set a title
on this dialog):

```java
// NonBrokerageAccountDialogHandler.java
public boolean recogniseWindow(Window window) {
    if (!(window instanceof JDialog)) return false;
    return (SwingUtils.findLabel(window, "This is not a brokerage account") != null);
}
```

### ExitConfirmation dialog

When IBC sends a STOP command, `StopTask.stop()` invokes the `File > Exit` menu item on
TWS (or `File > Close` on Gateway). TWS responds by showing an "Are you sure you want to
exit?" confirmation dialog. Without IBC intercepting it, the dialog would block the
shutdown indefinitely.

`ExitConfirmationDialogHandler` handles this dialog, but only when a shutdown is actually
in progress:

```java
// ExitConfirmationDialogHandler.java
public void handleWindow(Window window, int eventID) {
    if (!StopTask.shutdownInProgress()) return;  // user opened the dialog manually — ignore
    SwingUtils.clickButton(window, "Yes");
}
```

The `StopTask.shutdownInProgress()` guard is critical: the same dialog can appear if the
user clicks the window close button manually. In that case IBC must not auto-confirm the
exit (the user may have clicked it by accident). IBC only auto-confirms when it was IBC
itself that triggered the shutdown via `StopTask`.

Recognition is label-based:

```java
SwingUtils.findLabel(window, "Are you sure you want to exit?") != null
```

### Dialogs without a title (untitled)

Some TWS/Gateway dialogs appear without a window title. This makes recognition via
`SwingUtils.titleContains()` impossible, and forces IBC to identify them exclusively by
the presence of specific labels or buttons inside the dialog.

When a new untitled dialog appears and no existing handler matches it, it will be silently
ignored by `TwsListener` (the handler loop exits with no match). To diagnose what the
dialog contains, use the `LogStructureScope` and `LogStructureWhen` settings in
`config.ini`:

```text
# config.ini — log the full Swing component tree of every untitled dialog
LogStructureScope=untitled
LogStructureWhen=open
```

`LogStructureScope=untitled` restricts logging to windows whose title is empty or blank.
The output lists every `Component` in the tree with its class, text/label, and position,
giving enough information to write a new `recogniseWindow()` implementation.

Common untitled dialogs in practice:

- The NonBrokerageAccount paper-trading warning (Gateway only)
- Occasional API connection warning dialogs that vary by TWS version

Any handler for an untitled dialog must rely on a distinctive label or button text, and
that text should be checked across multiple TWS versions to confirm stability.
