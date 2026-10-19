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

import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

/**
 * Window handler for the IB Gateway login frame, selecting the gateway mode and supplying the
 * configured credentials.
 */
final class GatewayLoginFrameHandler extends AbstractLoginHandler {

    /**
     * Identifies whether the window is the IB Gateway login frame.
     * Checks for Gateway title variants and the presence of a login button.
     *
     * @param window the window to identify.
     * @return true if this is the Gateway login frame; false otherwise.
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame)) return false;

        return ((SwingUtils.titleContains(window, "IBKR Gateway") ||
                    SwingUtils.titleContains(window, "IB Gateway") ||
                    SwingUtils.titleContains(window, "Interactive Brokers Gateway")) &&
               (SwingUtils.findButton(window, "Login") != null ||
                SwingUtils.findButton(window, "Log In") != null ||          // TWS 974+
                SwingUtils.findButton(window, "Paper Log In") != null));    // TWS 974+
    }

    /**
     * Initializes the Gateway login frame by selecting the gateway mode (FIX or IB API).
     * Also sets the trading mode if the Trading Mode label is present.
     *
     * @param window the Gateway login frame.
     * @param eventID the AWT event ID.
     * @return true if initialization succeeded; false otherwise.
     * @throws IbcException if gateway mode selection fails.
     */
    @Override
    protected final boolean initialise(final Window window, int eventID) throws IbcException {
        selectGatewayMode(window);
        if (SwingUtils.findLabel(window, "Trading Mode") != null)  {
            setTradingMode(window);
        }
        return true;
    }

    /**
     * Performs pre-login validation and preparation by checking which credentials are missing.
     * Routes to FIX or IB API credential checking based on session mode.
     *
     * @param window the Gateway login frame.
     * @param eventID the AWT event ID.
     * @return true if all required credentials are present; false if any are missing.
     * @throws IbcException if credential validation fails.
     */
    @Override
    protected final boolean preLogin(final Window window, int eventID) throws IbcException {
        boolean result;
        if (SessionManager.isFIX()) {
            result = setMissingFIXCredentials(window);
        } else {
            result = setMissingIBAPICredentials(window);
        }
        return result;
    }

    /**
     * Validates FIX mode credentials and prompts for missing ones.
     * If IB API credentials are partially provided, validates those as well.
     *
     * @param window the Gateway login frame.
     * @return true if all required FIX and optional IB API credentials are present; false otherwise.
     */
    private boolean setMissingFIXCredentials(Window window) {
        boolean result = false;
        if (LoginManager.loginManager().FIXUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().FIXPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else if (LoginManager.loginManager().IBAPIUserName().length() != 0 || LoginManager.loginManager().IBAPIPassword().length() != 0) {
            if (LoginManager.loginManager().IBAPIUserName().length() == 0) {
                setMissingCredential(window, 3);
            } else if (LoginManager.loginManager().IBAPIPassword().length() == 0) {
                setMissingCredential(window, 4);
            } else {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * Validates IB API mode credentials and prompts for missing ones.
     *
     * @param window the Gateway login frame.
     * @return true if all required IB API credentials are present; false otherwise.
     */
    private boolean setMissingIBAPICredentials(Window window) {
        boolean result = false;
        if (LoginManager.loginManager().IBAPIUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().IBAPIPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else {
            result = true;
        }
        return result;
    }

    /**
     * Fills in the login frame fields with credentials based on the current session mode.
     * For FIX mode, sets FIX and IB API credentials; for IB API mode, sets only IB API credentials.
     *
     * @param window the Gateway login frame.
     * @param eventID the AWT event ID.
     * @return true if all credential fields were successfully populated.
     * @throws IbcException if credential setting fails.
     */
    @Override
    protected final boolean setFields(Window window, int eventID) throws IbcException {
        if (SessionManager.isFIX()) {
            Utils.logToConsole("Setting FIX user name");
            setCredential(window, "FIX user name", 0, LoginManager.loginManager().FIXUserName());
            Utils.logToConsole("Setting FIX password");
            setCredential(window, "FIX password", 1, LoginManager.loginManager().FIXPassword());
            Utils.logToConsole("Setting API user name");
            setCredential(window, "IBAPI user name", 2, LoginManager.loginManager().IBAPIUserName());
            Utils.logToConsole("Setting API password");
            setCredential(window, "IBAPI password", 3, LoginManager.loginManager().IBAPIPassword());
        } else {
            Utils.logToConsole("Setting user name");
            setCredential(window, "IBAPI user name", 0, LoginManager.loginManager().IBAPIUserName());
            Utils.logToConsole("Setting password");
            setCredential(window, "IBAPI password", 1, LoginManager.loginManager().IBAPIPassword());
        }
        return true;
    }

    /**
     * Selects the appropriate Gateway mode (FIX or IB API) based on session configuration.
     *
     * @param window the Gateway login frame.
     * @throws IbcException if the gateway mode selector button cannot be found.
     */
    private void selectGatewayMode(Window window) throws IbcException {
        if (SessionManager.isFIX()) {
            switchToFIX(window);
        } else {
            switchToIBAPI(window);
        }
    }

    /**
     * Switches the Gateway login frame to FIX CTCI mode by clicking the FIX CTCI toggle button.
     *
     * @param window the Gateway login frame.
     * @throws IbcException if the FIX CTCI selector button cannot be found.
     */
    private void switchToFIX(Window window) throws IbcException {
        JToggleButton button = SwingUtils.findToggleButton(window, "FIX CTCI");
        if (button == null) throw new IbcException("FIX CTCI selector");

        if (! button.isSelected()) {
            Utils.logToConsole("Clicking FIX CTCI selector");
            button.doClick();
        }
    }

    /**
     * Switches the Gateway login frame to IB API mode by clicking the IB API toggle button.
     * Looks for the "IB API" toggle button.
     *
     * @param window the Gateway login frame.
     * @throws IbcException if the IB API selector button cannot be found.
     */
    private void switchToIBAPI(Window window) throws IbcException {
        JToggleButton button = SwingUtils.findToggleButton(window, "IB API");
        if (button == null) throw new IbcException("IB API selector");

        if (! button.isSelected()) {
            Utils.logToConsole("Clicking IB API selector");
            button.doClick();
        }
    }

    /**
     * Checks whether the user ID field is disabled or absent from the Gateway login frame.
     * Field index depends on session mode (FIX vs IB API).
     *
     * @param window the Gateway login frame.
     * @return true if the user ID field is absent or disabled; false if enabled.
     */
    @Override
    protected boolean isUserIdDisabledOrAbsent(Window window) {
        int index = SessionManager.isFIX() ? 2 : 0;
        JTextField userID = SwingUtils.findTextField(window, index);
        if (userID == null) return true;
        return ! userID.isEnabled();
    }

    /**
     * Checks whether the password field is disabled or absent from the Gateway login frame.
     * Field index depends on session mode (FIX vs IB API).
     *
     * @param window the Gateway login frame.
     * @return true if the password field is absent or disabled; false if enabled.
     */
    @Override
    protected boolean isPasswordDisabledOrAbsent(Window window) {
        int index = SessionManager.isFIX() ? 3 : 1;
        JTextField password = SwingUtils.findTextField(window, index);
        if (password == null) return true;
        return ! password.isEnabled();
    }

}
