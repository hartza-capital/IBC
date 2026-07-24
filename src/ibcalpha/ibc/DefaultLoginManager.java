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

import javax.swing.JFrame;

public class DefaultLoginManager extends LoginManager {

    /**
     * Creates a new DefaultLoginManager that obtains credentials from settings.
     */
    public DefaultLoginManager() {
        /* don't actually get the credentials yet because the settings
         * provider might be changed
         */
        ibapiCredentialsFromArgs = false;
        fixCredentialsFromArgs = false;
        message = "will get username and password from settings";
    }

    /**
     * Creates a new DefaultLoginManager that extracts credentials from
     * command-line arguments.
     *
     * @param args the command-line arguments containing optional TWS and FIX
     *            credentials
     */
    public DefaultLoginManager(final String[] args) {
        ibapiCredentialsFromArgs = getTWSUserNameAndPasswordFromArguments(args);
        fixCredentialsFromArgs = getFIXUserNameAndPasswordFromArguments(args);
        message = "will get username and password from "
                + (ibapiCredentialsFromArgs ? "args" : "settings")
                + "; FIX username and password (if required) from "
                + (fixCredentialsFromArgs ? "args" : "settings");
    }

    /**
     * Creates a new DefaultLoginManager with TWS credentials provided to the
     * constructor.
     *
     * @param username the TWS login username
     * @param password the TWS login password
     */
    public DefaultLoginManager(final String username, final String password) {
        ibapiUserName = username;
        ibapiPassword = password;
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = false;
        message = "getting username and password from constructor";
    }

    /**
     * Creates a new DefaultLoginManager with both FIX and TWS credentials
     * provided to the constructor.
     *
     * @param fixUsername the FIX login username
     * @param fixPasswordArg the FIX login password
     * @param ibapiUsername the TWS login username
     * @param ibapiPasswordArg the TWS login password
     */
    public DefaultLoginManager(final String fixUsername,
            final String fixPasswordArg, final String ibapiUsername,
            final String ibapiPasswordArg) {
        this.fixUserName = fixUsername;
        this.fixPassword = fixPasswordArg;
        this.ibapiUserName = ibapiUsername;
        this.ibapiPassword = ibapiPasswordArg;
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = true;
        message = "getting username and password from constructor "
                + "(including FIX)";
    }

    private static final int MIN_ARGS_WITH_FIX = 3;
    private static final int MAX_ARGS_WITH_FIX = 6;
    private static final int MIN_ARGS_IBAPI_ONLY_LONG = 5;
    private static final int MAX_ARGS_IBAPI_ONLY_LONG = 6;
    private static final int MIN_ARGS_IBAPI_ONLY_SHORT = 3;
    private static final int MAX_ARGS_IBAPI_ONLY_SHORT = 4;
    private static final int MAX_ARGS_NO_CREDS = 2;
    private static final int FIX_USERNAME_INDEX = 1;
    private static final int FIX_PASSWORD_INDEX = 2;
    private static final int IBAPI_USERNAME_LONG_INDEX = 3;
    private static final int IBAPI_PASSWORD_LONG_INDEX = 4;

    private final String message;

    private volatile AbstractLoginHandler loginHandler = null;

    private final boolean ibapiCredentialsFromArgs;
    private final boolean fixCredentialsFromArgs;

    /**
     * IBAPI username - can either be supplied from the .ini file or as args[1]
     * NB: if IBAPI username is supplied in args[1], then the password must
     * be in args[2]. If IBAPI username is supplied in .ini, then the password
     * must also be in .ini.
     */
    private volatile String ibapiUserName;

    /**
     * IBAPI password - can either be supplied from the .ini file or as args[2].
     */
    private volatile String ibapiPassword;

    /**
     * FIX username - can either be supplied from the .ini file or as args[1]
     * NB: if username is supplied in args[1], then the password must
     * be in args[2], and the IBAPI username and password may be in
     * args[3] and args[4]. If username is supplied in .ini, then the password
     * must also be in .ini.
     */
    private volatile String fixUserName;

    /**
     * FIX password - can either be supplied from the .ini file or as args[2].
     */
    private volatile String fixPassword;


    /**
     * Logs a diagnostic message describing the source of login credentials.
     */
    @Override
    public void logDiagnosticMessage() {
        Utils.logToConsole("using default login manager: " + message);
    }

    /**
     * Returns the FIX login password from either constructor arguments or
     * settings.
     *
     * @return the FIX password
     */
    @Override
    public String fixPassword() {
        if (fixCredentialsFromArgs) {
            return fixPassword;
        }
        return getFIXPasswordFromSettings();
    }

    /**
     * Returns the FIX login username from either constructor arguments or
     * settings.
     *
     * @return the FIX username
     */
    @Override
    public String fixUserName() {
        if (fixCredentialsFromArgs) {
            return fixUserName;
        }
        return getFIXUserNameFromSettings();
    }

    /**
     * Returns the TWS login password from either constructor arguments or
     * settings.
     *
     * @return the TWS password
     */
    @Override
    public String ibapiPassword() {
        if (ibapiCredentialsFromArgs) {
            return ibapiPassword;
        }
        return getTWSPasswordFromSettings();
    }

    /**
     * Returns the TWS login username from either constructor arguments or
     * settings.
     *
     * @return the TWS username
     */
    @Override
    public String ibapiUserName() {
        if (ibapiCredentialsFromArgs) {
            return ibapiUserName;
        }
        return getTWSUserNameFromSettings();
    }

    /**
     * Returns the login dialog frame.
     *
     * @return the login frame
     */
    @Override
    public JFrame getLoginFrame() {
        return super.getLoginFrame();
    }

    /**
     * Sets the login dialog frame.
     *
     * @param window the login frame
     */
    @Override
    public void setLoginFrame(final JFrame window) {
        super.setLoginFrame(window);
    }

    /**
     * Returns the current login handler.
     *
     * @return the login handler instance
     */
    @Override
    public AbstractLoginHandler getLoginHandler() {
        return loginHandler;
    }

    /**
     * Sets the login handler to be used for login automation.
     *
     * @param handler the login handler instance
     */
    @Override
    public void setLoginHandler(final AbstractLoginHandler handler) {
        loginHandler = handler;
    }

    /**
     * Retrieves the FIX login password from the settings configuration.
     *
     * @return the FIX password from settings
     */
    private static String getFIXPasswordFromSettings() {
        String password = Settings.settings().getString("FIXPassword", "");
        return password;
    }

    /**
     * Retrieves the FIX login username from the settings configuration.
     *
     * @return the FIX username from settings
     */
    private static String getFIXUserNameFromSettings() {
        return Settings.settings().getString("FIXLoginId", "");
    }

    /**
     * Attempts to extract FIX username and password from command-line
     * arguments.
     *
     * @param args the command-line arguments
     * @return true if FIX credentials were found and extracted; false
     *         otherwise
     */
    private boolean getFIXUserNameAndPasswordFromArguments(
            final String[] args) {
        if (args.length >= MIN_ARGS_WITH_FIX
                && args.length <= MAX_ARGS_WITH_FIX) {
            fixUserName = args[FIX_USERNAME_INDEX];
            fixPassword = args[FIX_PASSWORD_INDEX];
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves the TWS login password from the settings configuration.
     *
     * @return the TWS password from settings
     */
    private static String getTWSPasswordFromSettings() {
        String password = Settings.settings().getString("IbPassword", "");
        return password;
    }

    /**
     * Retrieves the TWS login username from the settings configuration.
     *
     * @return the TWS username from settings
     */
    private static String getTWSUserNameFromSettings() {
        return Settings.settings().getString("IbLoginId", "");
    }

    /**
     * Attempts to extract TWS username and password from command-line
     * arguments.
     *
     * @param args the command-line arguments
     * @return true if TWS credentials were found and extracted; false
     *         otherwise
     */
    private boolean getTWSUserNameAndPasswordFromArguments(
            final String[] args) {
        if (args.length == MIN_ARGS_IBAPI_ONLY_LONG
                || args.length == MAX_ARGS_IBAPI_ONLY_LONG) {
            ibapiUserName = args[IBAPI_USERNAME_LONG_INDEX];
            ibapiPassword = args[IBAPI_PASSWORD_LONG_INDEX];
            return true;

        }
        if (args.length == MIN_ARGS_IBAPI_ONLY_SHORT
                || args.length == MAX_ARGS_IBAPI_ONLY_SHORT) {
            ibapiUserName = args[FIX_USERNAME_INDEX];
            ibapiPassword = args[FIX_PASSWORD_INDEX];
            return true;
        }
        if (args.length <= MAX_ARGS_NO_CREDS) {
            return false;
        }
        Utils.logError("Incorrect number of arguments passed. quitting...");
        Utils.logRawToConsole("Number of arguments = " + args.length);
        for (int i = 0; i < args.length; i++) {
            Utils.logRawToConsole("arg[" + i + "]=" + args[i]);
        }
        Utils.exitWithError(ErrorCodes.INCORRECT_NUMBER_OF_ARGS);
        return false;
    }

}
