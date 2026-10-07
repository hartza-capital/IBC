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
 * Default {@link LoginManager} implementation that takes the TWS and FIX credentials from the
 * settings file, or from the command-line arguments passed to its constructor.
 */
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
     * Creates a new DefaultLoginManager that extracts credentials from command-line arguments.
     *
     * @param args the command-line arguments containing optional TWS and FIX credentials
     */
    public DefaultLoginManager(String[] args) {
        ibapiCredentialsFromArgs = getTWSUserNameAndPasswordFromArguments(args);
        fixCredentialsFromArgs = getFIXUserNameAndPasswordFromArguments(args);
        message = "will get username and password from " +
                (ibapiCredentialsFromArgs ? "args" : "settings") +
                "; FIX username and password (if required) from " +
                (fixCredentialsFromArgs ? "args" : "settings");
    }

    /**
     * Creates a new DefaultLoginManager with TWS credentials provided to the constructor.
     *
     * @param username the TWS login username
     * @param password the TWS login password
     */
    public DefaultLoginManager(String username, String password) {
        IBAPIUserName = username;
        IBAPIPassword = password;
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = false;
        message = "getting username and password from constructor";
    }

    /**
     * Creates a new DefaultLoginManager with both FIX and TWS credentials provided to the constructor.
     *
     * @param FIXUsername the FIX login username
     * @param FIXPassword the FIX login password
     * @param IBAPIUsername the TWS login username
     * @param IBAPIPassword the TWS login password
     */
    public DefaultLoginManager(String FIXUsername, String FIXPassword, String IBAPIUsername, String IBAPIPassword) {
        this.FIXUserName = FIXUsername;
        this.FIXPassword = FIXPassword;
        this.IBAPIUserName = IBAPIUsername;
        this.IBAPIPassword = IBAPIPassword;
        ibapiCredentialsFromArgs = true;
        fixCredentialsFromArgs = true;
        message = "getting username and password from constructor (including FIX)";
    }

    private final String message;

    private volatile AbstractLoginHandler loginHandler = null;

    private final boolean ibapiCredentialsFromArgs;
    private final boolean fixCredentialsFromArgs;

    /**
     * IBAPI username - can either be supplied from the .ini file or as args[1]
     * NB: if IBAPI username is supplied in args[1], then the password must
     * be in args[2]. If IBAPI username is supplied in .ini, then the password must
     * also be in .ini.
     */
    private volatile String IBAPIUserName;

    /**
     * IBAPI password - can either be supplied from the .ini file or as args[2]
     */
    private volatile String IBAPIPassword;

    /**
     * FIX username - can either be supplied from the .ini file or as args[1]
     * NB: if username is supplied in args[1], then the password must
     * be in args[2], and the IBAPI username and password may be in 
     * args[3] and args[4]. If username is supplied in .ini, then the password must
     * also be in .ini.
     */
    private volatile String FIXUserName;

    /**
     * FIX password - can either be supplied from the .ini file or as args[2]
     */
    private volatile String FIXPassword;


    /**
     * Logs a diagnostic message describing the source of login credentials.
     */
    @Override
    public void logDiagnosticMessage(){
        Utils.logToConsole("using default login manager: " + message);
    }

    /**
     * Returns the FIX login password from either constructor arguments or settings.
     *
     * @return the FIX password
     */
    @Override
    public String FIXPassword() {
        if (fixCredentialsFromArgs) return FIXPassword;
        return getFIXPasswordFromSettings();
    }

    /**
     * Returns the FIX login username from either constructor arguments or settings.
     *
     * @return the FIX username
     */
    @Override
    public String FIXUserName() {
        if (fixCredentialsFromArgs) return FIXUserName;
        return getFIXUserNameFromSettings();
    }

    /**
     * Returns the TWS login password from either constructor arguments or settings.
     *
     * @return the TWS password
     */
    @Override
    public String IBAPIPassword() {
        if (ibapiCredentialsFromArgs) return IBAPIPassword;
        return getTWSPasswordFromSettings();
    }

    /**
     * Returns the TWS login username from either constructor arguments or settings.
     *
     * @return the TWS username
     */
    @Override
    public String IBAPIUserName() {
        if (ibapiCredentialsFromArgs) return IBAPIUserName;
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
    public void setLoginFrame(JFrame window) {
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
    public void setLoginHandler(AbstractLoginHandler handler) {
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
     * Attempts to extract FIX username and password from command-line arguments.
     *
     * @param args the command-line arguments
     * @return true if FIX credentials were found and extracted; false otherwise
     */
    private boolean getFIXUserNameAndPasswordFromArguments(String[] args) {
        if (args.length >= 3 && args.length <= 6) {
            FIXUserName = args[1];
            FIXPassword = args[2];
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
     * Attempts to extract TWS username and password from command-line arguments.
     *
     * @param args the command-line arguments
     * @return true if TWS credentials were found and extracted; false otherwise
     */
    private boolean getTWSUserNameAndPasswordFromArguments(String[] args) {
        if (args.length == 5 || args.length == 6) {
            IBAPIUserName = args[3];
            IBAPIPassword = args[4];
            return true;

        }
        if (args.length == 3 || args.length == 4) {
            IBAPIUserName = args[1];
            IBAPIPassword = args[2];
            return true;
        }
        if (args.length <= 2) {
            return false;
        }
        Utils.logError("Incorrect number of arguments passed. quitting...");
        Utils.logRawToConsole("Number of arguments = " +args.length);
        for (int i = 0; i < args.length; i++) {
            Utils.logRawToConsole("arg[" + i + "]=" + args[i]);
        }
        Utils.exitWithError(ErrorCodes.INCORRECT_NUMBER_OF_ARGS);
        return false;
    }

}
