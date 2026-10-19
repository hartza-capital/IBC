---
title: IBC User Guide
subtitle: Version 3.25.1 — Linux edition
---

# Important notes

**Auto-restart.** Starting with version 3.15.0, IBC allows TWS/Gateway to use the
auto-restart mechanism originally introduced in TWS 974/975. You can therefore
run TWS/Gateway all week with a single login at the start of the week, under
IBC's control. Configure this with the `AutoRestart` setting in the _Lock and
Exit_ section of the TWS/Gateway configuration dialog, or with `AutoRestartTime`
in `config.ini`.

Auto-restart can lead to confusion if you sometimes want to start TWS/Gateway
without IBC: see [How to run TWS/Gateway without IBC](#how-to-run-twsgateway-without-ibc-when-ibc-is-installed).

**Scope.** Read [Scope of this User Guide](#scope-of-this-user-guide) before
going further: this project distributes a Linux edition only.

# Introduction

## Overview of IBC

IBC enables Interactive Brokers' Trader Workstation (TWS) and IB Gateway to be
run in 'hands-free' mode, so that a user need not be present. This makes
possible the deployment of unattended automated trading systems.

IBC loads TWS or the Gateway and then 'listens' for various events (such as the
display of dialogs) that would normally require user intervention, and takes the
appropriate action on the user's behalf. For example, it automates the login by
filling the login dialog with your credentials and 'clicking' the login button.

Here are some of the things IBC does for you:

- starts TWS or the Gateway;
- logs you into TWS or the Gateway;
- clicks the YES button if the "Accept incoming connection?" dialog is
  displayed;
- allows TWS or the Gateway to restart each day without repeated
  authentication: authentication is only required the first time during the week
  that TWS or the Gateway run after 01:00 ET on Sunday;
- completes two-factor authentication, either by submitting a passcode it
  computes from a shared TOTP secret (`Ib2FA`), or by letting you acknowledge
  the alert in the IBKR Mobile app and retrying the login if it times out.

IBC also responds to commands sent to it by another program, for example to tell
TWS/Gateway to shut itself down cleanly, to restart, or to reconnect.

## Scope of this User Guide

This User Guide is intended to help you get started with IBC. It does not cover
every feature in depth.

The configuration file `config.ini` that governs IBC's behaviour contains
extensive notes on the various settings; it is the reference for anything not
explained here.

This edition covers the **Linux distribution only**: the project publishes a
single ZIP whose start scripts are Bash scripts. Windows and macOS editions are
no longer produced, the upstream project `IbcAlpha/IBC` having been retired on
1 September 2026 and its repository archived read-only.

# Getting Started

## Checklist

Here is a summary of the steps you need to perform to get IBC up and running
properly.

1. Install the **offline** edition of Trader Workstation or IB Gateway, and make
   sure it uses the English language setting. IBC does **not** work with the
   self-updating edition.

2. Install `xterm` if it is not already installed (to check, enter the command
   `xterm` in a terminal session). The start scripts open their diagnostic
   window in xterm unless you pass `-inline` as the first argument.

3. Download the IBC distribution ZIP, `IBCLinux-3.25.1.zip` (the name carries
   the version), from the releases page of the project (see
   [Where to get IBC](#where-to-get-ibc)).

4. Install IBC (see [Installing IBC](#installing-ibc)). If you already have an
   existing installation, rename its folder before installing the new version
   in case you need to revert to it later.

5. Create a folder called `ibc` in your home directory, encrypted if your
   distribution supports it, to hold your configuration (see
   [Password Security](#password-security)).

6. Copy `config.ini` from the IBC installation folder into that folder.

7. Edit `~/ibc/config.ini` with a text editor and set your credentials in
   `IbLoginId` and `IbPassword`. Set `Ib2FA` to your Base32 two-factor secret,
   or leave it empty to authenticate with a push notification or a manually
   entered code. It is advisable to use your paper-trading credentials at first,
   and for that you also need to set `TradingMode` to `paper`.

8. Check that the major version number of your TWS/Gateway installation is set
   in the start script you intend to use, `gatewaystart.sh` or `twsstart.sh`.
   To find it, first run TWS or the Gateway manually using the IBKR-provided
   icon, then click _Help > About Trader Workstation_ or _Help > About IB
   Gateway_. You will see a line similar to this:

   ```text
   Build 10.50.1f, Oct 5, 2026 4:18:53 PM
   ```

   The `TWS_MAJOR_VRSN` variable must then be set to `1050`, that is the first
   two parts of the version number with the period removed. The supplied
   scripts default to `1050`.

9. At this stage everything is set up to run IBC with its default settings,
   which start the Gateway (or TWS) and attempt to log in to your account. Run
   the relevant script from the IBC installation folder, for example
   `/opt/ibc/gatewaystart.sh`. If everything is satisfactory, shut down IBC by
   closing TWS/Gateway in the usual way.

   Information about the startup process is written to a log file, whose
   location is printed at startup (by default in `~/ibc/logs`). Include that
   file when reporting a problem with IBC.

10. Now edit `config.ini` further as needed: see
    [Configuring IBC](#configuring-ibc).

11. If you did not install TWS/Gateway and IBC in their default locations, or
    store the configuration file in the recommended location, edit the
    variables at the top of the start scripts accordingly. The scripts are
    commented to help you.

12. If you intend to run API programs that connect to TWS/Gateway, you must
    also edit the API settings in the TWS Global Configuration dialog.

13. If you want TWS/Gateway to restart every day during the week without you
    re-authenticating, set `AutoRestart` in the _Lock and Exit_ section of the
    Global Configuration dialog, or `AutoRestartTime` in `config.ini`. The only
    alternative is auto-logoff, which shuts TWS/Gateway down completely at the
    specified time, leaving you to restart it and re-authenticate.

# Prerequisites

## Java Runtime

IBC runs inside the same Java virtual machine as TWS/Gateway, so the Java
runtime it uses is the one shipped with your TWS/Gateway installation. The start
scripts pick that runtime from the installation automatically, which means you
do not have to install Java yourself.

`IBC.jar` is compiled for Java 25 (class file version 69), so the runtime must
be Java 25 or later. If your installation ships an older runtime, install Java
25 and point the `JAVA_PATH` variable in the start script at it; that variable
overrides the runtime taken from the installation.

## Interactive Brokers TWS or Gateway

Download and install the offline edition of Trader Workstation or IB Gateway
from the Interactive Brokers website. The location of the downloads page varies
from time to time and from country to country; at the time of writing it is
reachable through the _Trading_ menu, then _Platforms_, then _Download TWS_.

IBKR provides two modes of operation for TWS:

- an online, or self-updating TWS that receives updates automatically. IBC does
  not work with the self-updating TWS, so do not install that version;
- an offline or standalone TWS that never changes until you install another
  version. You must use this one with IBC.

The TWS installation contains the code for both TWS and the Gateway. The
Gateway-specific downloads on IBKR's website contain the same code but install
in a different place; either is fine. When you start IBC in Gateway mode it
loads the Gateway installation, and in TWS mode the TWS installation.

It is safest to use the 'stable' offline version rather than the 'latest' for
live trading: the latter is more likely to have bugs.

IBC needs TWS to operate in English so that it can recognise the dialogs it
interacts with. Set the language by starting TWS manually (that is, without
IBC) and selecting it on the initial login dialog; TWS remembers the setting
when you subsequently start it using IBC.

## Graphical session

IBC drives the real Swing user interface of TWS/Gateway, so it needs a display.
On a desktop that is the session you are logged into. On a headless server, run
an X server such as `Xvfb` and point `DISPLAY` at it before starting IBC.

# Where to get IBC

IBC is distributed as a ZIP file containing the compiled program and the files
listed below.

The latest version, and earlier ones, are published on the releases page of
<https://github.com/hartza-capital/IBC>. That repository is the maintained
source of IBC since the upstream project `IbcAlpha/IBC` was retired on
1 September 2026 and archived read-only.

The distribution ZIP, `IBCLinux-3.25.1.zip`, contains:

- the licence text, `COPYING.txt`;
- `IBC.jar`, the compiled Java code;
- a sample configuration file, `config.ini`;
- the start scripts `twsstart.sh` and `gatewaystart.sh`, plus the `scripts`
  sub-folder they call;
- command client scripts: `commandsend.sh`, `stop.sh`, `restart.sh`,
  `pause.sh`, `enableapi.sh`, `reconnectdata.sh`, `reconnectaccount.sh`;
- a text file called `version` containing the IBC version number;
- `README.md`, which points at the project;
- this User Guide, `userguide.pdf`.

Source code and build scripts are not part of the ZIP; they are in the
repository above.

# Installing IBC

Installing IBC is just a matter of extracting the ZIP file to wherever you want
to install it:

```bash
sudo unzip ~/Downloads/IBCLinux-3.25.1.zip -d /opt/ibc
cd /opt/ibc
sudo chmod o+x *.sh scripts/*.sh
```

`/opt/ibc` is the default location assumed by the scripts; using it minimises
customisation. If you already have a previous installation, rename its folder
(for example to `/opt/ibc.old`) so that you can refer back to your
customisations.

# Default Paths

The supplied scripts assume the defaults shown below, where `<username>` is your
operating system user name, not your IBKR login id.

| Item              | Path                        |
| ----------------- | --------------------------- |
| IBKR TWS/Gateway  | `/home/<username>/Jts`      |
| IBC program files | `/opt/ibc`                  |
| `config.ini`      | `/home/<username>/ibc`      |
| log files         | `/home/<username>/ibc/logs` |

If you store any of these items elsewhere, edit the variables at the top of the
start scripts to reflect the new locations.

# Password Security

To log in to TWS or IB Gateway, IBC needs your Interactive Brokers username and
password. Secure them carefully to prevent unauthorised use by third parties.

IBC accepts the credentials in one of two ways:

- through the configuration file `config.ini`: this is the preferred method,
  because the file can be protected by the operating system;
- as command-line parameters when IBC is started: this method is strongly
  deprecated, because the command line of a process is readable by other users
  of the machine (for example through `ps`).

## Protecting the configuration file

Store `config.ini` where other users of the machine cannot read it: within your
home directory, in its own `ibc` folder. Then restrict the permissions:

```bash
chmod 700 ~/ibc
chmod 600 ~/ibc/config.ini
```

Consider encrypting the folder as well, so that another user with administrator
privileges cannot read the contents even if they gain access to the files. On
Linux the usual options are an encrypted home directory, a LUKS volume mounted
under your home directory, or a per-directory tool such as `gocryptfs` or
`encfs`.

Note that `Ib2FA` is as sensitive as the password itself: anyone holding the
secret can generate valid passcodes.

# Configuring IBC

IBC must be supplied with a configuration file. A specimen `config.ini` is
included in the distribution ZIP; the scripts look for `~/ibc/config.ini` unless
you point `IBC_INI` somewhere else.

`config.ini` contains detailed comments on every setting, and most of them have
sensible defaults or are only needed in special situations. Here are the
settings you are most likely to change:

`IbLoginId`
: Your IBKR username.

`IbPassword`
: Your IBKR password.

`Ib2FA`
: Base32 TOTP secret (the text IBKR shows alongside the QR code), used to
complete two-factor authentication without any interaction. Leave it empty to
use a push notification or a manual code. A value that is not valid Base32
stops IBC at startup.

`SecondFactorDevice`
: When several 2FA devices are registered, the item to select in the device
list, spelled exactly as it appears there.

`TradingMode`
: Set to `paper` to use the paper-trading account, `live` otherwise; `live` is
the default if the setting is absent.

`AcceptNonBrokerageAccountWarning`
: Logging in to a paper-trading account raises a dialog confirming that it is
not a brokerage account, and TWS blocks API connections until it is accepted.
`yes` (the default) accepts it automatically; `no` leaves it to you.

`AcceptIncomingConnectionAction`
: It is safest to set this to `reject` and to list the permitted API addresses
in the TWS/Gateway configuration dialog instead.

`AutoRestartTime`
: Time at which TWS/Gateway restarts each day without re-authentication
(`<hh:mm>`).

`ClosedownAt`
: Time at which IBC shuts TWS/Gateway down tidily, with no restart: `<hh:mm>`
daily, or `<dayOfWeek hh:mm>` weekly, for example `Friday 22:00`.

`CommandServerPort`
: TCP port of the command server (7462 by convention); `0`, the default,
disables it. Must differ between instances running on the same machine.

`ExistingSessionDetectedAction`
: What to do when the same IBKR user is already logged in elsewhere: `manual`,
`primary`, `secondary` or `primaryoverride`.

`OverrideTwsApiPort`
: Forces the API listening port, whatever the value stored in TWS.

`IbDir` still exists, but using it is deprecated: auto-restart does not work
when TWS settings are redirected that way. Set `TWS_SETTINGS_PATH` in the start
script instead.

There are two ways IBC locates the configuration file:

- the simplest is to tell it in the start script, through the `IBC_INI`
  variable. This is the recommended approach (the supplied scripts do exactly
  that) and lets you give the file any name you like;
- if you do not specify a file, IBC expects `config.ini` in `~/ibc`.

# Starting IBC

The normal way to start IBC is to run one of the start scripts shipped in the
ZIP: `gatewaystart.sh` for the Gateway, `twsstart.sh` for TWS. Due to their
complexity you are strongly advised to use them rather than write your own.

Run the script from a terminal, or from any launcher that gives it a graphical
session. Three things govern how it behaves:

- without arguments, it opens an xterm window showing the startup diagnostics
  and then logs in;
- with `-inline` as the first argument, the diagnostics are printed in the
  current terminal instead, and no xterm is used. This is the mode to use from
  `cron` or `systemd`;
- `HIDE`, when set to `YES` or `TRUE`, starts the diagnostic xterm window
  iconified. It has no effect with `-inline`.

The variables at the top of the script are the only part you may need to
change:

`TWS_MAJOR_VRSN`
: Installed TWS/Gateway version, for example `1050` for build `10.50.1f`.

`IBC_INI`
: Location of `config.ini`; defaults to `~/ibc/config.ini`.

`TRADING_MODE`
: `live` or `paper`; overrides `TradingMode` in `config.ini`.

`TWOFA_TIMEOUT_ACTION`
: `exit` or `restart`: what to do when two-factor authentication times out (see
[Second Factor Authentication](#second-factor-authentication)).

`IBC_PATH`
: IBC installation folder; defaults to `/opt/ibc`.

`TWS_PATH`
: TWS/Gateway installation folder; defaults to `~/Jts`.

`TWS_SETTINGS_PATH`
: Folder where TWS/Gateway stores its settings; defaults to `TWS_PATH`.

`LOG_PATH`
: Folder for the diagnostic log; defaults to `~/ibc/logs`. Unset, the log goes
to the terminal; removed, nothing is logged.

`TWSUSERID`, `TWSPASSWORD`
: Credentials passed on the command line. Do not use them: put the credentials
in `config.ini` instead.

`FIXUSERID`, `FIXPASSWORD`
: FIX credentials, Gateway only. Deprecated for the same reason.

`JAVA_PATH`
: Java installation to run TWS/Gateway and IBC; defaults to the runtime shipped
with the installation.

`HIDE`
: `YES` or `TRUE` iconifies the diagnostic window.

The scripts refuse to start a second instance that uses the same `config.ini`
(detected by looking for a `java` process carrying that path). That is what
makes the periodic restarts described in
[Running IBC automatically](#running-ibc-automatically) safe.

# Second Factor Authentication

You can use your mobile phone or tablet running Android or iOS to provide second
factor authentication for your TWS login. To do that, install the IBKR Mobile
app on the device and register it for second factor authentication using the
button the app displays.

Once registered, every login to TWS or Gateway (including when IBC performs it)
sends an alert to your device. Acknowledging the alert completes the login.

## Automatic passcodes (TOTP)

IBC can also complete two-factor authentication with no interaction at all, by
computing the passcode itself. Set `Ib2FA` in `config.ini` to the Base32 secret
that IBKR shows next to the QR code when you register an authenticator:

```ini
Ib2FA=JBSWY3DPEHPK3PXP
```

IBC then derives the current six-digit passcode (RFC 4226/6238, HMAC-SHA1) and
submits it on the 2FA dialog.

Two caveats:

- any non-empty value is turned into a passcode and submitted, so a placeholder
  such as `changeme` produces a wrong code that spends one of the few failed
  login attempts IBKR allows before locking the account. Leave `Ib2FA` empty
  until you have the real secret;
- a value that is not valid Base32 stops IBC at startup with an
  `INVALID_SETTING_VALUE` error, rather than letting it attempt a login.

If several 2FA devices are registered, `SecondFactorDevice` selects the one to
use, spelled exactly as the device list shows it.

## When the login does not complete

If you use the IBKR Mobile app and fail to acknowledge the alert within a fixed
period (currently 3 minutes), the login cannot complete without manual
intervention at TWS/Gateway. IBC can be told to detect this timeout and
re-initiate the login, giving you another alert:

```ini
ReloginAfterSecondFactorAuthenticationTimeout=yes
```

The timeout/relogin mechanism can repeat any number of times until you
acknowledge an alert and the login succeeds.

In some circumstances the login does not complete even though you acknowledged
the alert. IBC can then shut down and restart, which repeats the login sequence
and gives you another chance to receive an alert. This is controlled by
`SecondFactorAuthenticationExitInterval`, the number of seconds IBC waits for
the login to complete after you acknowledged the alert. For the automatic
restart you must also set `TWOFA_TIMEOUT_ACTION=restart` in the start script.

If you have another mechanism that restarts IBC when it exits (for example
`cron` or `systemd`), set `TWOFA_TIMEOUT_ACTION=exit` instead, so that both
mechanisms do not react at the same time.

# Running IBC automatically

## crontab

On Linux you can use `crontab` to run `gatewaystart.sh` or `twsstart.sh`
automatically. For example, to start the Gateway at 08:00 on Mondays:

```text
0 8 * * 1 export DISPLAY=:10 && /bin/bash /opt/ibc/gatewaystart.sh -inline
```

The value of `DISPLAY` depends on how your system is configured.

Because the script exits immediately when an instance already runs with the same
`config.ini`, you can do better than a single start: attempt to start IBC
periodically and let it succeed only when it is not running.

```text
0,15,30,45 * * * 1-5 export DISPLAY=:10 && /bin/bash /opt/ibc/gatewaystart.sh -inline
```

This restarts TWS/Gateway after an unexpected shutdown and, with
`primaryoverride` as the `ExistingSessionDetectedAction`, also after a session
was terminated from the IBKR Mobile app or the Client Portal.

## systemd

A user unit does the same job with logging handled by the journal. Create
`ibc-gateway.service` in `~/.config/systemd/user`:

```ini
[Unit]
Description=IBC + IB Gateway
After=graphical-session.target

[Service]
Type=simple
Environment=DISPLAY=:0
ExecStart=/opt/ibc/gatewaystart.sh -inline
Restart=on-failure
RestartSec=60

[Install]
WantedBy=default.target
```

Then enable it and make the user manager start it at boot:

```bash
systemctl --user daemon-reload
systemctl --user enable --now ibc-gateway
loginctl enable-linger "$USER"
```

`systemctl --user stop ibc-gateway` ends the session, whereas the `stop.sh`
script lets IBC close TWS/Gateway tidily.

# Multiple IBC instances

You may want to run more than one instance of TWS or the Gateway on the same
computer, perhaps simultaneously. Common reasons:

- you want to run both your live and paper-trading accounts, especially if you
  need market data from both (IBKR only allows that when both instances run on
  the same computer);
- you have several logins for your live account;
- you trade on behalf of others, each with their own account;
- you want to trade in different regions at different times;
- you want to test a new TWS version on paper while your live account stays on
  the previous one.

TWS stores a large number of settings in a folder structure inside the
installation folder, one sub-folder per username. Some files it creates while
running are not separated by username and only one instance can hold them at a
time, so multiple instances work as long as each logs in to a different
username.

`TWS_SETTINGS_PATH` tells TWS/Gateway where to store its settings, so running
instances simultaneously means giving each of them its own settings folder.
You do not need to copy the TWS `.jar` files: all instances can load the same
installation.

For example, to keep a live and a paper instance completely separate:

- install TWS in the default location, `~/Jts`;
- create two settings folders, `~/JtsLive` and `~/JtsPaper`;
- create two configuration files, `configLive.ini` and `configPaper.ini`, with
  the matching credentials in `IbLoginId` and `IbPassword` (plus
  `TradingMode=paper` in the paper one);
- copy `gatewaystart.sh` to `gatewaystartLive.sh` and `gatewaystartPaper.sh`;
- in each copy, set `IBC_INI` to its configuration file and `TWS_SETTINGS_PATH`
  to the matching settings folder, and give the two instances different
  `LOG_PATH` folders, for example `~/ibc/LiveLogs` and `~/ibc/PaperLogs`.

Running the two scripts then starts two instances, each logged in to a different
account with its settings in its own folder. Do not let them share a log
folder: they would write to the same log file and one of them would fail.

# Using different TWS versions simultaneously

To use more than one version of TWS (for example to test a new one with your
paper account while the live account stays on the previous version), install
the versions in the normal way: TWS's installer places each one in its own
folder named after the version number.

Then follow the advice in the previous section and make sure each start script
has the `TWS_MAJOR_VRSN` of the version it is to load.

# How to run TWS/Gateway without IBC when IBC is installed

For auto-restart to work properly, the IBC start script renames the TWS/Gateway
executables by appending a `1` to the filename: `tws` becomes `tws1` and
`ibgateway` becomes `ibgateway1`. If those files have their original names when
auto-restart occurs, TWS/Gateway do restart, but without IBC, and every benefit
of IBC is lost. For that reason, do not rename them back while IBC is running.

The scripts do not restore the original names when IBC exits, which can be
confusing if you then want to start TWS/Gateway without IBC. Some options:

- rename the executables back to their original names before running them;
- run the renamed executables directly, for example
  `~/Jts/1050/ibgateway1`;
- edit the IB-supplied desktop launcher, or create a second one, to point at the
  renamed executable;
- if you have a script that starts TWS/Gateway without IBC, change it to use
  whichever of the two names currently exists;
- install a second copy of TWS/Gateway in another folder and use only that one
  without IBC, keeping the same settings through `TWS_SETTINGS_PATH`.

# Command Server

IBC incorporates a command server that lets external programs influence its
operation. To issue a command, first open a TCP connection to the port given by
`CommandServerPort` in `config.ini`; the server is not started when that setting
is `0`.

The client then sends the command as plain text and may read the socket for the
answer: a line starting with `OK` when the command was accepted, `ERROR`
otherwise, each followed by a message. A client may send several consecutive
commands, and should finish with `EXIT` (which is not necessary after `STOP`,
since that closes the socket).

By default the server accepts connections from the local machine only; list
other permitted addresses or host names in `ControlFrom`, and restrict the
listening interface with `BindAddress` on a multi-homed host. `CommandPrompt`
sets a string printed after the connection is opened and after each command,
which makes interactive use easier.

A simple way to use the command server is the `telnet` command. The distribution
ZIP contains a script per command: `stop.sh`, `restart.sh`, `pause.sh`,
`enableapi.sh`, `reconnectdata.sh`, `reconnectaccount.sh`, all of which call
`commandsend.sh`. Edit `commandsend.sh` first to set the address and port.

`STOP`
: Shuts TWS/Gateway down tidily, as the _File > Exit_ menu command would.

`RESTART`
: Restarts TWS/Gateway as if the auto-restart time had arrived. Credentials from
the current session are reused, so no second factor is needed. It cannot
bypass IBKR's requirement that the session be shut down completely at some
point on Sunday.

`PAUSE`
: Restarts TWS/Gateway like `RESTART`, but IBC then exits instead of relaunching
it. The session is kept, so starting IBC again continues it without a new
authentication; that is how a session is parked or moved between runs.

`ENABLEAPI`
: Ensures the _Enable ActiveX and Socket Clients_ checkbox is set. TWS only: the
Gateway rejects the command, its API having no such checkbox.

`RECONNECTDATA`
: Refreshes the market data connections, as Ctrl-Alt-F does. Not valid for the
FIX Gateway.

`RECONNECTACCOUNT`
: Reconnects to the IB login server, as Ctrl-Alt-R does. Not valid for the FIX
Gateway.

`EXIT`
: Closes the connection to the command server.

For TWS, `RESTART` uses the _File > Restart..._ menu command, so the restart
starts immediately. The Gateway has no such command: IBC sets the auto-restart
time in the _Lock and Exit_ section to the start of the next minute (or of the
one after, when less than 58 seconds into the current minute) and displays a
countdown overlay on the main window. That time remains in force after the
restart, so either let `AutoRestartTime` in `config.ini` override it, or send
another `RESTART` once the session is back to set it to its usual value.

# Any Questions?

The retired upstream project used the support forum at
<https://groups.io/g/ibcalpha>. For this repository, use the issue tracker at
<https://github.com/hartza-capital/IBC/issues>.

When reporting a problem, provide as much evidence as you can: the versions of
IBC (the `version` file in the installation) and of TWS/Gateway, a description
of the incorrect behaviour, and the IBC log. The log usually identifies the
source of a problem quickly.

# Changes from IBController

IBC was forked from the original IBController project and differs from it in
ways that matter when you switch:

1. The program file is called `IBC.jar`.

2. Settings file changes:

   - the file is called `config.ini`, where IBController used
     `IBController.ini`;
   - `ForceTwsApiPort` was renamed `OverrideTwsApiPort`;
   - `AcceptIncomingConnectionAction` now defaults to `manual`, so API
     connections from unknown computers must be accepted explicitly;
   - `PasswordEncrypted` and `FIXPasswordEncrypted` were removed, along with
     the password 'encryption' facility;
   - `IbControllerPort` was renamed `CommandServerPort`, and defaults to `0`,
     meaning the command server is not started;
   - `IbControlFrom` was renamed `ControlFrom`;
   - `IbBindAddress` was renamed `BindAddress`.

3. Script names on Linux:

   ```text
   IBControllerStart.sh          ->  twsstart.sh
   IBControllerGatewayStart.sh   ->  gatewaystart.sh
   ```

# Building IBC from source

Building requires a Java 25 JDK and the `IBC_BIN` variable pointing at the
`jars` folder of an installed TWS/Gateway. The Maven wrapper provides Maven
itself.

```bash
export IBC_BIN=~/Jts/1050/jars
make dist          # dist/IBCLinux-<version>.zip
make test          # JUnit 5 suite
make checkstyle    # style gate
make docker-verify # checks the ZIP as a consumer receives it
```

`CONTRIBUTING.md` describes the build in detail and `docker/README.md` the
containerised release checks.

To regenerate this guide after editing `docs/userguide.md`, run `make userguide`,
which rebuilds `resources/userguide.pdf`; it needs `pandoc` and `tectonic` on
`PATH` (`brew install pandoc tectonic`).

# Licence

IBC is licensed under the GNU General Public License v3. See `COPYING.txt` in
the distribution, or <http://www.gnu.org/licenses/gpl.html>.
