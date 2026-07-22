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

/**
 * Default implementation of LoginManager that obtains credentials from
 * command-line arguments, constructor arguments, or settings.
 */
public class DefaultLoginManager extends LoginManager {

    /** Index of the FIX username command-line argument. */
    private static final int FIX_USERNAME_ARG_INDEX = 1;

    /** Index of the FIX password command-line argument. */
    private static final int FIX_PASSWORD_ARG_INDEX = 2;

    /** Index of the IBAPI username in long-form command-line arguments. */
    private static final int IBAPI_USERNAME_LONG_FORM_ARG_INDEX = 3;

    /** Index of the IBAPI password in long-form command-line arguments. */
    private static final int IBAPI_PASSWORD_LONG_FORM_ARG_INDEX = 4;

    /** Index of the IBAPI username in short-form command-line arguments. */
    private static final int IBAPI_USERNAME_SHORT_FORM_ARG_INDEX = 1;

    /** Index of the IBAPI password in short-form command-line arguments. */
    private static final int IBAPI_PASSWORD_SHORT_FORM_ARG_INDEX = 2;

    /** Minimum argument count that may include FIX credentials. */
    private static final int MIN_ARGS_WITH_FIX = 3;

    /** Maximum argument count that may include FIX credentials. */
    private static final int MAX_ARGS_WITH_FIX = 6;

    /** Minimum argument count for short-form TWS credentials. */
    private static final int MIN_ARGS_SHORT_FORM = 3;

    /** Maximum argument count for short-form TWS credentials. */
    private static final int MAX_ARGS_SHORT_FORM = 4;

    /** Minimum argument count for long-form TWS credentials. */
    private static final int MIN_ARGS_LONG_FORM = 5;

    /** Maximum argument count for long-form TWS credentials. */
    private static final int MAX_ARGS_LONG_FORM = 6;

    /** Maximum argument count when no credentials are supplied. */
    private static final int MAX_ARGS_WITHOUT_CREDENTIALS = 2;

    /**
     * Creates a new DefaultLoginManager that obtains credentials from
     * settings.
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
     * @param args the command-line arguments containing optional TWS and
     *             FIX credentials
     */
    public DefaultLoginManager(final String[] args) {
        ibapiCredentialsFromArgs =
                getTWSUserNameAndPasswordFromArguments(args);
        fixCredentialsFromArgs =
                getFIXUserNameAndPasswordFromArguments(args);
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
    public DefaultLoginManager(final String username,
                               final String password) {
        ibapiUserName = username;
        ibapiPassword = toCharArray(password);
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = false;
        message = "getting username and password from constructor";
    }

    /**
     * Creates a new DefaultLoginManager with both FIX and TWS credentials
     * provided to the constructor.
     *
     * @param fixUsername the FIX login username
     * @param fixPasswordValue the FIX login password
     * @param ibapiUsername the TWS login username
     * @param ibapiPasswordValue the TWS login password
     */
    public DefaultLoginManager(final String fixUsername,
                               final String fixPasswordValue,
                               final String ibapiUsername,
                               final String ibapiPasswordValue) {
        this.fixUserName = fixUsername;
        this.fixPassword = toCharArray(fixPasswordValue);
        this.ibapiUserName = ibapiUsername;
        this.ibapiPassword = toCharArray(ibapiPasswordValue);
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = true;
        message = "getting username and password from constructor "
                + "(including FIX)";
    }

    /** Diagnostic message describing the credential source. */
    private final String message;

    /** The current login handler, if one has been set. */
    private volatile AbstractLoginHandler loginHandler = null;

    /** Whether the IBAPI credentials were supplied as arguments. */
    private final boolean ibapiCredentialsFromArgs;

    /** Whether the FIX credentials were supplied as arguments. */
    private final boolean fixCredentialsFromArgs;

    /**
     * IBAPI username - can either be supplied from the .ini file or as args[1].
     * NB: if IBAPI username is supplied in args[1], then the password must
     * be in args[2]. If IBAPI username is supplied in .ini, then the password
     * must also be in .ini.
     */
    private volatile String ibapiUserName;

    /**
     * IBAPI password - can either be supplied from the .ini file or as args[2].
     */
    private volatile char[] ibapiPassword;

    /**
     * FIX username - can either be supplied from the .ini file or as args[1].
     * NB: if username is supplied in args[1], then the password must
     * be in args[2], and the IBAPI username and password may be in
     * args[3] and args[4]. If username is supplied in .ini, then the password
     * must also be in .ini.
     */
    private volatile String fixUserName;

    /**
     * FIX password - can either be supplied from the .ini file or as args[2].
     */
    private volatile char[] fixPassword;


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
     * When the password comes from constructor arguments, a fresh clone is
     * returned so the caller can zero it without affecting the retained value
     * needed for retries.
     *
     * @return the FIX password; callers should zero the array after use
     */
    @Override
    public char[] FIXPassword() {
        if (fixCredentialsFromArgs) {
            return fixPassword.clone();
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
    public String FIXUserName() {
        if (fixCredentialsFromArgs) {
            return fixUserName;
        }
        return getFIXUserNameFromSettings();
    }

    /**
     * Returns the TWS login password from either constructor arguments or
     * settings.
     * When the password comes from constructor arguments, a fresh clone is
     * returned so the caller can zero it without affecting the retained value
     * needed for retries.
     *
     * @return the TWS password; callers should zero the array after use
     */
    @Override
    public char[] IBAPIPassword() {
        if (ibapiCredentialsFromArgs) {
            return ibapiPassword.clone();
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
    public String IBAPIUserName() {
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
     * @return a fresh char array containing the FIX password from settings
     */
    private static char[] getFIXPasswordFromSettings() {
        return Settings.settings().getString("FIXPassword", "").toCharArray();
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
            fixUserName = args[FIX_USERNAME_ARG_INDEX];
            fixPassword = toCharArray(args[FIX_PASSWORD_ARG_INDEX]);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Retrieves the TWS login password from the settings configuration.
     *
     * @return a fresh char array containing the TWS password from settings
     */
    private static char[] getTWSPasswordFromSettings() {
        return Settings.settings().getString("IbPassword", "").toCharArray();
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
        if (args.length >= MIN_ARGS_LONG_FORM
                && args.length <= MAX_ARGS_LONG_FORM) {
            ibapiUserName = args[IBAPI_USERNAME_LONG_FORM_ARG_INDEX];
            ibapiPassword =
                    toCharArray(args[IBAPI_PASSWORD_LONG_FORM_ARG_INDEX]);
            return true;

        }
        if (args.length >= MIN_ARGS_SHORT_FORM
                && args.length <= MAX_ARGS_SHORT_FORM) {
            ibapiUserName = args[IBAPI_USERNAME_SHORT_FORM_ARG_INDEX];
            ibapiPassword =
                    toCharArray(args[IBAPI_PASSWORD_SHORT_FORM_ARG_INDEX]);
            return true;
        }
        if (args.length <= MAX_ARGS_WITHOUT_CREDENTIALS) {
            return false;
        }
        Utils.logError(
                "Incorrect number of arguments passed. quitting...");
        Utils.logRawToConsole("Number of arguments = " + args.length);
        for (int i = 0; i < args.length; i++) {
            Utils.logRawToConsole("arg[" + i + "]=" + args[i]);
        }
        Utils.exitWithError(ErrorCodes.INCORRECT_NUMBER_OF_ARGS);
        return false;
    }

    /**
     * Returns a char array copy of a String password, or an empty array if the
     * input is null.
     *
     * @param password the password as a String; may be null
     * @return a char array containing the password characters, or an empty
     *         array if password is null
     */
    private static char[] toCharArray(final String password) {
        return password == null ? new char[0] : password.toCharArray();
    }

}
