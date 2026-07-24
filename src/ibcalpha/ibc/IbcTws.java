// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author stevek
 *
 * This is our way of automating the Interactive Brokers' TWS and Gateway
 * applications so they do not require human interaction. IBC is a class whose
 * main starts up the TWS or Gateway application, and which monitors the
 * application for certain events, such as the login dialog, after which it can
 * automatically respond to these events. Upon seeing the login dialog, it fills
 * out the username and pwd and presses the button. Upon seeing the "allow
 * incoming connection dialog it presses the yes button.
 *
 * This code is based original code by Ken Geis (ken_geis@telocity.com).
 *
 * Amendment history:
 *   Date     Author           Change # Description
 *   -------- -------------------- -------- -----------
 *   20040316 Richard King         1        Handle new .ini file entry
 *                                          IbAutoClosedown.
 *   20040316 Richard King         2        Handle "Accept incoming connection"
 *                                          dialog with changed title.
 *   20040802 Steven Kearns        3        Incorporated Richard King's changes
 *                                          into my latest version.
 *   20060203 Richard King         4        Changed the string used for
 *                                          recognising the "Newer Version"
 *                                          dialog.
 *   20060203 Richard King         5        Changed the way that the Exit
 *                                          Session Setting dialog handling
 *                                          works. It now sets the autologoff
 *                                          time to 5 minutes before the current
 *                                          time.
 *  20060404 Richard King          6        Changed to cater for the new Login
 *                                          dialog title in TWS 857 (was "New
 *                                          Login", now "Login").
 *  20060404 Richard King          7        Added an option to specify a day
 *                                          of the week and a time when
 *                                          IBController should tidily close
 *                                          TWS and shut down.
 *  20060404 Richard King          8        Fixed an unimportant bug in the
 *                                          handling of the TWS Exit Session
 *                                          dialogue. Where the time configured
 *                                          in TWS for autologoff was within
 *                                          five minutes after midnight,
 *                                          IBController would set it to just
 *                                          before midday instead of just before
 *                                          midnight.
 *  20070205 Richard King          9        Fixed another unimportant bug in
 *                                          the TWS Exit Session dialog handler.
 *  20070205 Richard King          10       Now caters for the variant of the
 *                                          TWS 'Newer version' dialog that has
 *                                          Yes and No buttons instead of an Ok
 *                                          button.
 *  20070226 Richard King          11       Fixed a bug in the implementation
 *                                          of the WAITINIT command.
 *  20070226 Richard King          12       Fixed a bug in ConfigureApiTask.
 *  20070226 Richard King          13       Username and password can now be
 *                                          supplied as args[1] and args[2]
 *                                          rather than via the .ini file.
 *                                          Password must not be encrypted.
 *  20070305 Richard King          14       Removed the check for null title in
 *                                          'newer version' dialogs because
 *                                          Linux TWSs sometimes display this
 *                                          dialog with a non-null title.
 *  20070305 Richard King          15       For the Linux versions of the
 *                                          'newer version' dialog, click 'No'
 *                                          instead of 'Yes'.
 *  20070305 Richard King          16       Added private static final long
 *                                          serialVersionUID to
 *                                          ScriptProperties.java to avoid a
 *                                          warning given by some compilers.
 *  20070903 Richard King          17       Removed the test for !wasOpen when
 *                                          checking for the Exit Session
 *                                          Setting dialog.
 *                                 18       Added a PasswordEncypted option. If
 *                                          set to 'no', the password in the
 *                                          .ini file is treated as not
 *                                          encrypted. The default is 'yes'.
 *                                 19       Added timestamps to system.out
 *                                          writes.
 *  20071213 Richard King          20       Now recognises the German Exit
 *                                          Session dialogue.
 *  20100201 Richard King          21       The code has been completely
 *                                          refactored to improve readability
 *                                          and maintainability. Some redundant
 *                                          code has been removed.
 *                                 22       Added a MinimizeMainWindow option.
 *                                          Setting this to 'yes' causes the TWS
 *                                          main window to be minimised when TWS
 *                                          is started. The default is 'no'.
 *                                 23       Added an AllowBlindTrading option.
 *                                          Setting this to 'yes' causes the
 *                                          warning message output by TWS to be
 *                                          dismissed (by clicking the 'Yes'
 *                                          button) when attempting to place an
 *                                          order for a contract for which the
 *                                          user has no market data
 *                                          subscription. The default is 'no'.
 *                                 24       Added a StoreSettingsOnServer
 *                                          option. Setting this to 'yes' sets
 *                                          the corresponding option in the TWS
 *                                          login dialog, resulting in TWS
 *                                          settings being stored on IB's
 *                                          servers. The default is 'no'.
 *                                 25       The WAITINIT command to
 *                                          IBController Server has been changed
 *                                          to ENABLEAPI.
 *  20100204 Richard King          26       Changed so that if username and/or
 *                                          password are not supplied in either
 *                                          the commnd line args or the config
 *                                          file, then TWS prompts for them
 *                                          rather than IBController shutting
 *                                          down with an error.
 *  20100317 Richard King          27       Changed to handle the new Tip of
 *                                          the Day dialog in TWS 903 which has
 *                                          a different title.
 *  20100322 Richard King          28       Login processing has been
 *                                          modified to enable and click the
 *                                          Login button after a brief delay,
 *                                          because TWS 903 no longer enables
 *                                          the Login button when the username
 *                                          and password fields are filled in
 *                                          programatically.
 *  20100329 Richard King          29       Fixed a bug where the ENABLEAPI
 *                                          command was not handled correctly if
 *                                          issued before TWS's main window was
 *                                          loaded.
 *  20100331 Richard King          30       Fixed a bug where streams of error
 *                                          messages were written to System.err
 *                                          if an CommandServer client
 *                                          disconnected by resetting the
 *                                          connection (as opposed to using the
 *                                          Exit command).
 *  20100419 Richard King          31       TWS 903.7 has changed the 'Newer
 *                                          version' notification from a
 *                                          JDialog to a JFrame. The
 *                                          NewerVersionFrameHandler class has
 *                                          been added to handle this.
 *  20100419 Richard King          32       Improved Change # 28 to keep
 *                                          periodically clicking the Login
 *                                          button after enabling it, until it
 *                                          becomes disabled. This overcomes the
 *                                          problem that the necessary delay
 *                                          time in the original approach was
 *                                          unknown, and seemed to vary from
 *                                          machine to machine and from time to
 *                                          time.
 *  20100420 Richard King          33       Support for the IB Gateway added.
 *                                          Only the IB API mode of the gateway
 *                                          is supported, not the FIX mode.
 *  20100420 Richard King          34       Fixed a bug where the tidy shutdown
 *                                          time was not accurately observed.
 *  20100521 Richard King          35       Changed the Gateway login handler
 *                                          to click the login button
 *                                          repeatedly until the login dialog
 *                                          becomes invisible: some users found
 *                                          that the first click had no effect
 *  20100728 Richard King          36       Addressed some threading and
 *                                          synchronization issues. Thanks to
 *                                          Brent Boyer for pointing them out.
 *                                 37       Commands to CommandServer now
 *                                          produce the following responses:
 *                                              OK info
 *                                              ERROR info
 *                                              INFO info
 *                                          Where 'info' is a text string. OK
 *                                          and ERROR are final responses. INFO
 *                                          is an intermediate response that may
 *                                          be sent to provide information about
 *                                          the command's progress. INFO's may
 *                                          be suppressed using the
 *                                          SuppressInfoMessages option (see
 *                                          change 39).
 *                                 38       Added a CommandPrompt option. The
 *                                          specified string is output by the
 *                                          server when the connection to
 *                                          CommandServer is first opened and
 *                                          after the completion of each
 *                                          command. If no string is specified,
 *                                          no prompt is issued.
 *                                 39       Added a SuppressInfoMessages option.
 *                                          If set to 'yes', only final
 *                                          responses from CommandServer are
 *                                          sent; intermediate info messages
 *                                          are suppressed. Default is 'yes'.
 *  20100816 Richard King          40       Added handlers for Password and
 *                                          NSE Compliance dialogs (Indian TWS).
 *                                          By default, Password Notice is not
 *                                          dismissed, but NSE Compliance is.
 *                                          Overridable via new settings:
 *                                          DismissPasswordExpiryWarning and
 *                                          DismissNSEComplianceNotice.
 *  20100902 Richard King          41       Changed MainWindowFrameHandler to
 *                                          recognise the new main window title
 *                                          introduced in TWS 907.
 *  20100905 Shane Cusson          42       Added GPL licensing, added to
 *                                          SourceForge.net
 *  20110623 Richard King          43       Changed some window handlers to
 *                                          reflect changes in titles etc in TWS
 *                                          918.6.
 *                                 44       Enhanced the ENABLEAPI command
 *                                          implementation to cater for the fact
 *                                          that the Configure top-level menu
 *                                          was removed in TWS 909. API
 *                                          configuration can now only be done
 *                                          via the Edit > Global
 *                                          Configuration... menu.
 *                                 45       Added an AutoConfirmOrders option.
 *                                          If set to yes, then when orders are
 *                                          placed using the BookTrader in TWS,
 *                                          the confirmation dialog is
 *                                          automatically handled, thereby
 *                                          effectively restoring the one-click
 *                                          trading that was removed in TWS 906.
 *                                          The default is 'no', requiring the
 *                                          user to manually confirm each trade.
 *                                 46       Fixed the NewerVersionDialogHandler:
 *                                          the text to be searched for (in
 *                                          current TWS versions) is contained
 *                                          in a JOptionPane, not a JLabel.
 *  20131218 Richard King          47       Modified
 *                                          AcceptIncomingConnection handler
 *                                          to skip title bar check (varies by
 *                                          TWS version; not needed).
 *                                 48       Added an
 *                                          AcceptIncomingConnectionAction
 *                                          setting. If set to 'accept',
 *                                          IBController automatically accepts
 *                                          the incoming connection request. If
 *                                          set to 'reject', IBController
 *                                          automatically rejects the incoming
 *                                          connection request. If set to
 *                                          'manual', IBController does nothing
 *                                          and the user must decide whether to
 *                                          accept or reject the incoming
 *                                          connection request. The default is
 *                                          'accept'.
 *                                 49       Improved Exit Session Setting
 *                                          dialog handling. In TWS 942, caption
 *                                          shown only on first display. Dialog
 *                                          instance is stored and reused to
 *                                          detect subsequent displays.
 *                                 50       Added a ShowAllTrades setting.
 *                                          If yes: Trades log shown with 'All'
 *                                          checkbox for all executions. User
 *                                          cannot change checkboxes or close
 *                                          log. If no, no interaction with
 *                                          Trades log. Default is no.
 *                                 51       Added RECONNECTACCOUNT and
 *                                          RECONNECTDATA commands.
 *                                          RECONNECTACCOUNT causes TWS to
 *                                          disconnect from the IB account
 *                                          server and then reconnect (the same
 *                                          as the user pressing Ctrl-Alt-R).
 *                                          RECONNECTDATA causes TWS to
 *                                          disconnect from all market data
 *                                          farms and then reconnect (the same
 *                                          as the user pressing Ctrl-Alt-F).
 *                                          Thanks to Cheung Kwok Fai for
 *                                          suggesting this and supplying the
 *                                          relevant code edits.
 *                                 52       Added an
 *                                          ExistingSessionDetectedAction
 *                                          setting. When TWS logs on it checks
 *                                          to see whether the account is
 *                                          already logged in. If so it displays
 *                                          a dialog: this setting instructs TWS
 *                                          how to proceed. If set to 'primary',
 *                                          TWS ends the other session and
 *                                          continues with the new session. If
 *                                          set to 'secondary', TWS exits
 *                                          (other session unaffected). 'manual'
 *                                          means user must handle. Default is
 *                                          'manual'.
 *                                 53       Change # 45 removed (not correctly
 *                                          implemented). TWS now allows users
 *                                          to suppress order confirmations,
 *                                          and legal restrictions on one-click
 *                                          trading via BookTrader have been
 *                                          lifted.
 *                                 54       Added a LogToConsole setting. If
 *                                          set to 'yes', all logging output
 *                                          from IBController is to the console
 *                                          and may be directed into a file
 *                                          using the normal > or >>
 *                                          command line redirection operators.
 *                                          If set to 'no', output from
 *                                          IBController that is logged after it
 *                                          has loaded TWS appears in the TWS
 *                                          logfile. The default is 'no'.
 *  20140228 Richard King          55       Added FIX CTCI gateway support.
 *                                          New settings: FIX (yes/no; default
 *                                          no), FIXLoginId, FIXPassword,
 *                                          FIXPasswordEncrypted (default yes).
 *                                          For market data via gateway, also
 *                                          use IbLoginId and IbPassword.
 *                                          Credentials via args 2-3 (FIX),
 *                                          4-5 (market data).
 *
 * With the move to Github, the value of recording details of amendments here
 * is questionable, and this practice has therefore been discontinued.
 *
 */
public final class IbcTws {

    private static final int DAYS_IN_WEEK = 7;
    private static final int MAX_COMMAND_LINE_ARGS = 6;

    /**
     * Private constructor prevents instantiation of this utility class.
     */
    private IbcTws() { }

    /**
     * Starts the TWS automation agent.
     * Sets up exception handling, validates arguments, initializes the
     * environment, and loads TWS.
     *
     * @param args command-line arguments (optional: config file,
     *             credentials, trading mode)
     * @throws Exception if an error occurs during TWS initialization
     */
    public static void main(final String[] args) throws Exception {
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread.setDefaultUncaughtExceptionHandler(
                new ibcalpha.ibc.UncaughtExceptionHandler());
        }
        checkArguments(args);
        setupDefaultEnvironment(args, false);
        load();
    }

    static void setupDefaultEnvironment(final String[] args,
            final boolean isGateway) throws Exception {
        SessionManager.initialise(isGateway);
        Settings.initialise(new DefaultSettings(args));
        LoginManager.initialise(new DefaultLoginManager(args));
        MainWindowManager.initialise(new DefaultMainWindowManager());
        TradingModeManager.initialise(new DefaultTradingModeManager(args));
    }

    /**
     * Validates command-line arguments for IBC.
     * Allowable parameter combinations:
     *
     * 1. No parameters
     *
     * 2. <iniFile> [<tradingMode>]
     *
     * 3. <iniFile> <apiUserName> <apiPassword> [<tradingMode>]
     *
     * 4. <iniFile> <fixUserName> <fixPassword> <apiUserName> <apiPassword>
     * [<tradingMode>]
     *
     * where:
     *      <iniFile>       ::= NULL | path-and-filename-of-.ini-file
     *      <tradingMode>   ::= blank | LIVETRADING | PAPERTRADING
     *      <apiUserName>   ::= blank | username-for-TWS
     *      <apiPassword>   ::= blank | password-for-TWS
     *      <fixUserName>   ::= blank | username-for-FIX-CTCI-Gateway
     *      <fixPassword>   ::= blank | password-for-FIX-CTCI-Gateway
     *
     * @param args the arguments to validate
     */
    static void checkArguments(final String[] args) {
        if (args.length > MAX_COMMAND_LINE_ARGS) {
            Utils.logError("Incorrect number of arguments passed. quitting...");
            Utils.logRawToConsole("Number of arguments = " + args.length);
            for (String arg : args) {
                Utils.logRawToConsole(arg);
            }
            Utils.exitWithError(ErrorCodes.INCORRECT_NUMBER_OF_ARGS);
        }
    }

    /**
     * Initializes and starts the TWS/Gateway automation system.
     * Sets up the Toolkit listener, command server, shutdown scheduling,
     * and auto-save of settings. Catches and gracefully handles illegal
     * state exceptions during initialization.
     */
    public static void load() {
        try {
            printVersionInfo();

            printProperties();

            Settings.settings().logDiagnosticMessage();
            LoginManager.loginManager().logDiagnosticMessage();
            MainWindowManager.mainWindowManager().logDiagnosticMessage();
            TradingModeManager.tradingModeManager().logDiagnosticMessage();
            ConfigDialogManager.configDialogManager().logDiagnosticMessage();

            startCommandServer();

            startShutdownTimerIfRequired();

            createToolkitListener();

            startSavingTwsSettingsAutomatically();

            startTwsOrGateway();
        } catch (IllegalStateException e) {
            if (e.getMessage().equalsIgnoreCase("Shutdown in progress")) {
                // an exception with this message can occur if a STOP command is
                // processed by IBC while TWS/Gateway is still in early stages
                // of initialisation
                Utils.exitWithoutError();
            }
        }
    }

    /**
     * Prints IBC version information to the console.
     */
    public static void printVersionInfo() {
        Utils.logToConsole("version: " + IbcVersionInfo.IBC_VERSION);
    }

    /**
     * Registers the Toolkit AWTEventListener to intercept window events
     * and dispatch to handlers.
     */
    private static void createToolkitListener() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            new TwsListener(createWindowHandlers()),
            AWTEvent.WINDOW_EVENT_MASK);
    }

    /**
     * Creates the list of window handlers in priority order.
     * Order matters: handlers that match similar window patterns are ordered
     * to check specific handlers first. For example,
     * SecondFactorAuthenticationDialogHandler must come before
     * SecurityCodeDialogHandler.
     *
     * @return a list of WindowHandler instances ready to be registered
     *         with the Toolkit listener
     */
    private static List<WindowHandler> createWindowHandlers() {
        List<WindowHandler> windowHandlers = new ArrayList<>();

        windowHandlers.add(new AcceptIncomingConnectionDialogHandler());
        windowHandlers.add(new BlindTradingWarningDialogHandler());
        windowHandlers.add(new LoginFrameHandler());
        windowHandlers.add(new GatewayLoginFrameHandler());
        windowHandlers.add(new MainWindowFrameHandler());
        windowHandlers.add(new GatewayMainWindowFrameHandler());
        windowHandlers.add(new NewerVersionDialogHandler());
        windowHandlers.add(new NewerVersionFrameHandler());
        windowHandlers.add(new NotCurrentlyAvailableDialogHandler());
        windowHandlers.add(new TipOfTheDayDialogHandler());
        windowHandlers.add(new NSEComplianceFrameHandler());
        windowHandlers.add(new PasswordExpiryWarningFrameHandler());
        windowHandlers.add(new GlobalConfigurationDialogHandler());
        windowHandlers.add(new TradesFrameHandler());
        windowHandlers.add(new ExistingSessionDetectedDialogHandler());
        windowHandlers.add(new ApiChangeConfirmationDialogHandler());
        windowHandlers.add(new SplashFrameHandler());

        // this line must come before SecurityCodeDialogHandler
        // because both contain an "Enter Read Only" button
        windowHandlers.add(
            SecondFactorAuthenticationDialogHandler.getInstance());
        windowHandlers.add(new SecurityCodeDialogHandler());

        windowHandlers.add(new ReloginDialogHandler());
        windowHandlers.add(new NonBrokerageAccountDialogHandler());
        windowHandlers.add(new ExitConfirmationDialogHandler());
        windowHandlers.add(new TradingLoginHandoffDialogHandler());
        windowHandlers.add(new LoginFailedDialogHandler());
        windowHandlers.add(
            new TooManyFailedLoginAttemptsDialogHandler());
        windowHandlers.add(new ShutdownProgressDialogHandler());
        windowHandlers.add(
            new BidAskLastSizeDisplayUpdateDialogHandler());
        windowHandlers.add(new LoginErrorDialogHandler());
        windowHandlers.add(
            new CryptoOrderConfirmationDialogHandler());
        windowHandlers.add(
            new RestartConfirmationDialogHandler());
        windowHandlers.add(
            new ResetOrderIdConfirmationDialogHandler());
        windowHandlers.add(
            new ReconnectDataOrAccountConfirmationDialogHandler());
        windowHandlers.add(new GatewayDialogHandler());
        return windowHandlers;
    }

    /**
     * Calculates the next scheduled cold restart time based on the
     * ColdRestartTime setting. Parses a 24-hour HH:mm format and schedules
     * for the next occurrence (Sunday by default).
     *
     * @return the next scheduled cold restart time, or null if
     *         ColdRestartTime is not configured
     */
    private static Date getColdRestartTime() {
        String coldRestartTimeSetting =
            Settings.settings().getString("ColdRestartTime", "");
        if (coldRestartTimeSetting.length() == 0) {
            return null;
        }

        int shutdownDayOfWeek = Calendar.SUNDAY;
        int shutdownHour;
        int shutdownMinute;
        Calendar cal = Calendar.getInstance();
        try {
            try {
                cal.setTime(
                    (new SimpleDateFormat("HH:mm"))
                        .parse(coldRestartTimeSetting));
            } catch (ParseException e) {
                throw e;
            }
            shutdownHour = cal.get(Calendar.HOUR_OF_DAY);
            shutdownMinute = cal.get(Calendar.MINUTE);
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.set(Calendar.HOUR_OF_DAY, shutdownHour);
            cal.set(Calendar.MINUTE, shutdownMinute);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH,
                    (shutdownDayOfWeek + DAYS_IN_WEEK
                            - cal.get(Calendar.DAY_OF_WEEK)) % DAYS_IN_WEEK);
            if (!cal.getTime().after(new Date())) {
                cal.add(Calendar.DAY_OF_MONTH, DAYS_IN_WEEK);
            }
        } catch (ParseException e) {
            Utils.exitWithError(ErrorCodes.INVALID_SETTING_VALUE,
                    "Invalid ColdRestartTime setting: '"
                            + coldRestartTimeSetting
                            + "'; format should be: <hh:mm>   eg 13:00");
        }
        return cal.getTime();
    }

    /**
     * Calculates the next scheduled shutdown time based on the
     * ClosedownAt setting. Parses either a 24-hour time (daily) or a
     * day-of-week and 24-hour time (weekly).
     *
     * @return the next scheduled shutdown time, or null if ClosedownAt
     *         is not configured
     */
    private static Date getShutdownTime() {
        String shutdownTimeSetting =
            Settings.settings().getString("ClosedownAt", "");
        if (shutdownTimeSetting.length() == 0) {
            return null;
        } else {
            int shutdownDayOfWeek;
            int shutdownHour;
            int shutdownMinute;
            Calendar cal = Calendar.getInstance();
            try {
                boolean dailyShutdown = false;
                try {
                    cal.setTime(
                        (new SimpleDateFormat("E HH:mm"))
                            .parse(shutdownTimeSetting));
                    dailyShutdown = false;
                } catch (ParseException e) {
                    try {
                        String today =
                            (new SimpleDateFormat("E"))
                                .format(cal.getTime());
                        cal.setTime(
                            (new SimpleDateFormat("E HH:mm"))
                                .parse(today + " " + shutdownTimeSetting));
                        dailyShutdown = true;
                    } catch (ParseException x) {
                        throw x;
                    }
                }
                shutdownDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                shutdownHour = cal.get(Calendar.HOUR_OF_DAY);
                shutdownMinute = cal.get(Calendar.MINUTE);
                cal.setTimeInMillis(System.currentTimeMillis());
                cal.set(Calendar.HOUR_OF_DAY, shutdownHour);
                cal.set(Calendar.MINUTE, shutdownMinute);
                cal.set(Calendar.SECOND, 0);
                cal.add(Calendar.DAY_OF_MONTH,
                        (shutdownDayOfWeek + DAYS_IN_WEEK
                                - cal.get(Calendar.DAY_OF_WEEK))
                                % DAYS_IN_WEEK);
                if (!cal.getTime().after(new Date())) {
                    if (dailyShutdown) {
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                    } else {
                        cal.add(Calendar.DAY_OF_MONTH, DAYS_IN_WEEK);
                    }
                }
            } catch (ParseException e) {
                Utils.exitWithError(ErrorCodes.INVALID_SETTING_VALUE,
                        "Invalid ClosedownAt setting: '"
                                + shutdownTimeSetting
                                + "'; format should be: "
                                + "<[day ]hh:mm>   eg 22:00 or Friday 22:00");
            }
            return cal.getTime();
        }
    }

    /**
     * Constructs the full path to the TWS jts.ini configuration file.
     *
     * @return the path to jts.ini in the TWS settings directory
     */
    private static String getJtsIniFilePath() {
        return getTWSSettingsDirectory() + File.separatorChar
            + "jts.ini";
    }

    /**
     * Gets or creates the TWS settings directory from the IbDir
     * configuration setting. Defaults to the current working directory if
     * not configured.
     *
     * @return the path to the TWS settings directory
     */
    private static String getTWSSettingsDirectory() {
        String path = Settings.settings()
            .getString("IbDir", System.getProperty("user.dir"));
        try {
            Files.createDirectories(Paths.get(path));
        } catch (FileAlreadyExistsException ex) {
            Utils.exitWithError(ErrorCodes.CANT_CREATE_TWS_SETTINGS_DIR,
                    "Failed to create TWS settings directory at: " + path
                            + "; a file of that name already exists");
        } catch (IOException ex) {
            Utils.exitWithException(
                    ErrorCodes.CANT_CREATE_TWS_SETTINGS_DIR, ex);
        }
        return path;
    }

    /**
     * Logs all system properties to the console.
     * Masks credential arguments in sun.java.command to prevent logging
     * sensitive data.
     */
    private static void printProperties() {
        Properties p = System.getProperties();
        Enumeration<Object> i = p.keys();
        Utils.logRawToConsole("System Properties");
        Utils.logRawToConsole(
            "----------------------------------------------");
        while (i.hasMoreElements()) {
            String props = (String) i.nextElement();
            String vals = (String) p.get(props);
            if (props.equals("sun.java.command")) {
                //hide credentials
                String[] args = vals.split(" ");
                for (int j = 2; j < args.length - 1; j++) {
                    args[j] = "***";
                }
                vals = String.join(" ", args);
            }
            Utils.logRawToConsole(props + " = " + vals);
        }
        Utils.logRawToConsole(
            "----------------------------------------------");
    }

    /**
     * Starts the IB Gateway application with the configured TWS settings
     * directory. Exits with error if the Gateway entry point cannot be
     * found.
     */
    private static void startGateway() {
        String[] twsArgs = new String[1];
        twsArgs[0] = getTWSSettingsDirectory();
        try {
            Utils.logToConsole("Starting Gateway");
            ibgateway.GWClient.main(twsArgs);
        } catch (Throwable t) {
            Utils.logException(t);
            Utils.exitWithError(ErrorCodes.CANT_FIND_ENTRYPOINT);
        }
    }

    /**
     * Starts the TCP command server in a background thread.
     * The command server listens for remote control commands if enabled.
     */
    private static void startCommandServer() {
        MyCachedThreadPool.getInstance().execute(new CommandServer());
    }

    private static boolean isColdRestart = false;

    /**
     * Schedules a shutdown or cold restart timer based on configuration
     * settings. If both ClosedownAt and ColdRestartTime are configured, the
     * earliest time is used.
     */
    private static void startShutdownTimerIfRequired() {
        Date shutdownTime = getShutdownTime();
        Date coldRestartTime = getColdRestartTime();
        if (shutdownTime == null && coldRestartTime == null) {
            return;
        }
        if (shutdownTime == null && coldRestartTime != null) {
            isColdRestart = true;
            shutdownTime = coldRestartTime;
        } else if (shutdownTime != null && coldRestartTime != null) {
            if (coldRestartTime.before(shutdownTime)) {
                isColdRestart = true;
                shutdownTime = coldRestartTime;
            }
        }
        long delay = shutdownTime.getTime() - System.currentTimeMillis();
        String appName = SessionManager.isGateway() ? "Gateway" : "TWS";
        String actionName = isColdRestart ? "cold restarted" : "shut down";
        String timeStr =
            (new SimpleDateFormat("yyyy/MM/dd HH:mm"))
                .format(shutdownTime);
        Utils.logToConsole(appName + " will be " + actionName + " at "
                + timeStr);
        MyScheduledExecutorService.getInstance().schedule(() -> {
            MyCachedThreadPool.getInstance()
                .execute(new StopTask(null, isColdRestart,
                        "ColdRestartTime setting"));
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Starts the TWS application with the configured TWS settings
     * directory. Opens the Trades log window if ShowAllTrades is enabled.
     * Exits with error if the TWS entry point cannot be found.
     */
    private static void startTws() {
        if (Settings.settings().getBoolean("ShowAllTrades", false)) {
            Utils.showTradesLogWindow();
        }
        String[] twsArgs = new String[1];
        twsArgs[0] = getTWSSettingsDirectory();
        try {
            Utils.logToConsole("Starting TWS");
            jclient.LoginFrame.main(twsArgs);
        } catch (Throwable t) {
            Utils.logException(t);
            Utils.exitWithError(ErrorCodes.CANT_FIND_ENTRYPOINT);
        }
    }

    /**
     * Orchestrates the startup of TWS or Gateway based on session
     * configuration. Initializes the JTS INI manager and applies all
     * required configuration tasks (API port, master client ID, auto
     * logoff, precautions, etc.).
     */
    private static void startTwsOrGateway() {
        Utils.logToConsole("TWS Settings directory is: "
            + getTWSSettingsDirectory());
        SessionManager.startSession();
        JtsIniManager.initialise(getJtsIniFilePath());
        if (SessionManager.isGateway()) {
            startGateway();
        } else {
            startTws();
        }

        configureResetOrderIdsAtStart();
        configureApiPort();
        configureMasterClientID();
        configureReadOnlyApi();
        configureSendMarketDataInLotsForUSstocks();
        configureAutoLogoffOrRestart();
        configureApiPrecautions();
    }

    /**
     * Applies the ResetOrderIdsAtStart configuration if enabled.
     * Ignored for FIX mode (only applies to IB API).
     */
    private static void configureResetOrderIdsAtStart() {
        String configName = "ResetOrderIdsAtStart";
        boolean resetOrderIds =
            Settings.settings().getBoolean(configName, false);
        if (resetOrderIds) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(
                new ConfigureResetOrderIdsTask(resetOrderIds)))
                .executeAsync();
        }
    }

    /**
     * Applies the OverrideTwsApiPort configuration if set.
     * Overrides the API port number configured in TWS settings.
     * Ignored for FIX mode (only applies to IB API).
     */
    private static void configureApiPort() {
        String configName = "OverrideTwsApiPort";
        int portNumber = Settings.settings().getInt(configName, 0);
        if (portNumber != 0) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(
                new ConfigureTwsApiPortTask(portNumber)))
                .executeAsync();
        }
    }

    /**
     * Applies the OverrideTwsMasterClientID configuration if set.
     * Overrides the master client ID configured in TWS settings.
     * Ignored for FIX mode (only applies to IB API).
     */
    private static void configureMasterClientID() {
        String configName = "OverrideTwsMasterClientID";
        String masterClientID =
            Settings.settings().getString(configName, "");
        if (!masterClientID.equals("")) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(
                new ConfigureTwsMasterClientIDTask(masterClientID)))
                .executeAsync();
        }
    }

    /**
     * Applies the AutoRestartTime or AutoLogoffTime configuration if set.
     * AutoRestartTime takes precedence over AutoLogoffTime if both are
     * configured. Ignored for FIX mode (only applies to IB API).
     */
    private static void configureAutoLogoffOrRestart() {
        String configName = "AutoLogoffTime Or AutoRestartTime";
        String autoLogoffTime =
            Settings.settings().getString("AutoLogoffTime", "");
        String autoRestartTime =
            Settings.settings().getString("AutoRestartTime", "");
        if (autoRestartTime.length() != 0
                || autoLogoffTime.length() != 0) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
        }
        if (autoRestartTime.length() != 0) {
            (new ConfigurationTask(
                new ConfigureAutoLogoffOrRestartTimeTask(
                    "Auto restart", autoRestartTime)))
                .executeAsync();
            if (autoLogoffTime.length() != 0) {
                Utils.logToConsole(
                    "AutoLogoffTime is ignored because "
                            + "AutoRestartTime is also set");
            }
        } else if (autoLogoffTime.length() != 0) {
            (new ConfigurationTask(
                new ConfigureAutoLogoffOrRestartTimeTask(
                    "Auto logoff", autoLogoffTime)))
                .executeAsync();
        }
    }

    /**
     * Applies the ReadOnlyApi configuration if set.
     * Configures whether the API connection is read-only or allows order
     * submission. Ignored for FIX mode (only applies to IB API).
     */
    private static void configureReadOnlyApi() {
        String configName = "ReadOnlyApi";
        if (!Settings.settings().getString(configName, "").equals("")) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(
                new ConfigureReadOnlyApiTask(
                    Settings.settings().getBoolean(configName, true))))
                .executeAsync();
        }
    }

    /**
     * Applies the SendMarketDataInLotsForUSstocks configuration if set.
     * Configures whether US stock market data is sent in shares or lots.
     * Ignored for FIX mode (only applies to IB API).
     */
    private static void configureSendMarketDataInLotsForUSstocks() {
        String configName = "SendMarketDataInLotsForUSstocks";
        String sendMarketDataInLots =
            Settings.settings().getString(configName, "");
        if (!sendMarketDataInLots.equals("")) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(
                new ConfigureSendMarketDataInLotsForUSstocksTask(
                    Settings.settings().getBoolean(configName, true))))
                .executeAsync();
        }
    }

    /**
     * Applies API precaution configurations if any are set.
     * Configures bypassing of various order and risk warnings and
     * confirmations. Ignored for FIX mode (only applies to IB API).
     */
    private static void configureApiPrecautions() {
        String configName = "ApiPrecautions";

        final Map<String, String> bypassSettingsByCheckboxText =
                new LinkedHashMap<>();
        bypassSettingsByCheckboxText.put(
                "Bypass Order Precautions for API Orders",
                Settings.settings().getString("BypassOrderPrecautions", ""));
        bypassSettingsByCheckboxText.put(
                "Bypass Bond warning for API Orders.",
                Settings.settings().getString("BypassBondWarning", ""));
        bypassSettingsByCheckboxText.put(
                "Bypass negative yield to worst confirmation for "
                        + "API Orders.",
                Settings.settings()
                        .getString(
                                "BypassNegativeYieldToWorstConfirmation",
                                ""));
        bypassSettingsByCheckboxText.put(
                "Bypass Called Bond warning for API Orders",
                Settings.settings().getString("BypassCalledBondWarning", ""));
        bypassSettingsByCheckboxText.put(
                "Bypass \"same action pair trade\" warning for API "
                        + "orders.",
                Settings.settings()
                        .getString("BypassSameActionPairTradeWarning", ""));
        bypassSettingsByCheckboxText.put(
                "Bypass price-based volatility risk warning for API "
                        + "Orders.",
                Settings.settings()
                        .getString(
                                "BypassPriceBasedVolatilityRiskWarning", ""));
        bypassSettingsByCheckboxText.put(
                "Bypass US Stocks market data in shares warning for API "
                        + "Orders",
                Settings.settings()
                        .getString(
                                "BypassUSStocksMarketDataInSharesWarning",
                                ""));
        bypassSettingsByCheckboxText.put(
                "Bypass Redirect Order warning for Stock API Orders",
                Settings.settings().getString("BypassRedirectOrderWarning",
                        ""));
        bypassSettingsByCheckboxText.put(
                "Bypass No Overfill Protection precaution for "
                        + "destinations where implied natively.",
                Settings.settings()
                        .getString(
                                "BypassNoOverfillProtectionPrecaution", ""));

        boolean anyPrecautionConfigured = bypassSettingsByCheckboxText
                .values()
                .stream()
                .anyMatch(settingValue -> !settingValue.equals(""));
        if (anyPrecautionConfigured) {
            if (SessionManager.isFIX()) {
                Utils.logToConsole(configName + " - ignored for FIX");
                return;
            }
            (new ConfigurationTask(new ConfigureApiPrecautionsTask(
                    bypassSettingsByCheckboxText)))
                .executeAsync();
        }
    }

    /**
     * Starts the automatic saving of TWS settings to persist
     * configuration changes.
     */
    private static void startSavingTwsSettingsAutomatically() {
        TwsSettingsSaver.getInstance().initialise();
    }

}

