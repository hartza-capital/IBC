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
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Base class for handlers that automate a TWS login frame, providing the
 * shared login flow and delegating frame-specific steps to subclasses.
 */
public abstract class AbstractLoginHandler implements WindowHandler {

    /**
     * Determines whether to process the given AWT event for this window.
     * Accepts WINDOW_OPENED events based on the current login state.
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID (e.g., WindowEvent.WINDOW_OPENED)
     * @return true if this event should be handled, false otherwise
     */
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                Utils.logToConsole("Login dialog WINDOW_OPENED: LoginState is " + LoginManager.loginManager().getLoginState().toString());
                switch (LoginManager.loginManager().getLoginState()) {
                    case LOGGED_IN:
                        return false;
                    case LOGIN_FAILED:
                        return true;
                    case LOGGING_IN:
                        return false;
                    case TWO_FA_IN_PROGRESS:
                        return false;
                    default:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * Handles the login dialog by establishing the login handler, frame reference, and initiating login.
     * Registers itself as the login handler if not already set and delegates to initiateLogin().
     *
     * @param window the login dialog window
     * @param eventID the AWT event ID (e.g., WindowEvent.WINDOW_OPENED)
     */
    @Override
    public final void handleWindow(Window window, int eventID) {
        if (LoginManager.loginManager().getLoginHandler() == null) LoginManager.loginManager().setLoginHandler(this);
        LoginManager.loginManager().setLoginFrame((JFrame) window);
        switch (LoginManager.loginManager().getLoginState()){
            case LOGGED_OUT:
                if (! SessionManager.isRestart()) {
                    initiateLogin(window);
                } else {

                    // allow automatic relogin to ptoceed

                }
        }
    }

    /**
     * Identifies whether a window is a login frame that this handler should
     * process.
     *
     * @param window the window to test
     * @return true if this is a recognized login frame, false otherwise
     */
    @Override
    public abstract boolean recogniseWindow(Window window);
    
    private static int loginAttemptNumber = 0;

    /**
     * Returns the number of login attempts made so far.
     *
     * @return the count of login attempts
     */
    int currentLoginAttemptNumber() {
        return loginAttemptNumber;
    }
    
    /**
     * Starts a login attempt by setting the login state to awaiting credentials,
     * initialising the frame, filling the credential fields and submitting the login.
     *
     * @param window the login frame
     */
    void initiateLogin(Window window) {
        LoginManager.loginManager().setLoginState(LoginManager.LoginState.AWAITING_CREDENTIALS);
        try {
            if (!initialise(window, WindowEvent.WINDOW_OPENED)) return;
            if (!setFields(window, WindowEvent.WINDOW_OPENED)) return;
            if (!preLogin(window, WindowEvent.WINDOW_OPENED)) return;

            Utils.logToConsole("Login attempt: " + ++loginAttemptNumber);
            doLogin(window);
        } catch (IbcException e) {
            Utils.exitWithError(ErrorCodes.CANT_FIND_CONTROL, "could not login: could not find control: " + e.getMessage());
        }
    }

    /**
     * Initiates the login process by setting login state to LOGGING_IN and clicking the Login button.
     * For live trading in TWS 1016+, polls for the Login frame to transform into a 2FA dialog.
     *
     * @param window the login frame
     * @throws IbcException if the login button cannot be found
     */
    private void doLogin(final Window window) throws IbcException {

        // this JLabel is only present for the 1016+ versions
        final JLabel initialTitleLabel = SwingUtils.findLabel(window, "LOGIN");
        
        GuiDeferredExecutor.instance().execute(() -> {
            final JButton loginButton = findLoginButton(window);
            LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGING_IN);
            SwingUtils.clickButton(loginButton);
        });
        
        String tradingMode = TradingModeManager.tradingModeManager().getTradingMode();
        if (tradingMode.equalsIgnoreCase(TradingModeManager.TRADING_MODE_PAPER)) {
            // paper trading mode doesn't use Second Factor Authentication, so nothing
            // to do here
        } else if (initialTitleLabel != null) {
            // Starting with TWS 1016, there is no longer a separate Second Factor
            // Authentication dialog. Instead, TWS replaces the Login frame's controls
            // with the controls that used to be in the 2FA dialog (so the Login frame
            // effectively becomes the 2FA frame). This doesn't generate any events
            // that IBC normally handles, so it goes undetected, and thus IBC doesn't
            // know when to process the Second Factor Authentication dialog. 
            //
            // To avoid this problem, we make a periodic check that the JLabel that
            // initially contained "LOGIN" has changed to "SECOND FACTOR AUTHENTICATION":
            // when this happens, we can pass the window to the SecondFactorAuthenticationDialogHandler
            // to be actioned.

            Utils.logToConsole("Waiting for Login frame to become SecondFactorAuthenticationDialog");
            MyScheduledExecutorService.getInstance().schedule(
                    () -> {
                        checkChangeToSecondFactorAuthenticationDialog(window);
                    }, 
                    200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Periodically checks if the Login frame label has changed from "LOGIN" to "SECOND FACTOR AUTHENTICATION".
     * When detected, passes the window to SecondFactorAuthenticationDialogHandler for processing.
     * Reschedules itself if the transition has not yet occurred.
     *
     * @param window the login frame being monitored
     */
    private void checkChangeToSecondFactorAuthenticationDialog(Window window) {
        JLabel currentTitleLabel = SwingUtils.findLabel(window, "SECOND FACTOR AUTHENTICATION");
        if (currentTitleLabel != null) {
            // the login frame has now become the 2FA dialog, so invoke the 
            // handler for that as if it had just been opened
            Utils.logToConsole("Login frame has now become SecondFactorAuthenticationDialog");
            TwsListener.logWindow(window, WindowEvent.WINDOW_OPENED);
            TwsListener.logWindowStructure(window, WindowEvent.WINDOW_OPENED, true);
            SecondFactorAuthenticationDialogHandler.getInstance().handleWindow(window, WindowEvent.WINDOW_OPENED);
        } else {
            MyScheduledExecutorService.getInstance().schedule(
                    () -> {
                        checkChangeToSecondFactorAuthenticationDialog(window);
                    }, 
                    200, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Initializes login dialog state and validates the frame for login
     * processing.
     *
     * @param window the login frame
     * @param eventID the AWT event ID that triggered this method
     * @return true if initialization was successful, false if the dialog is
     *     invalid
     * @throws IbcException if a required control cannot be found
     */
    protected abstract boolean initialise(final Window window, int eventID) throws IbcException;

    /**
     * Performs pre-login configuration and validation before attempting login.
     *
     * @param window the login frame
     * @param eventID the AWT event ID that triggered this method
     * @return true if pre-login checks passed and login should proceed, false
     *     otherwise
     * @throws IbcException if a required control cannot be found or updated
     */
    protected abstract boolean preLogin(final Window window, int eventID) throws IbcException;

    /**
     * Populates credential fields in the login frame with username and
     * password.
     *
     * @param window the login frame
     * @param eventID the AWT event ID that triggered this method
     * @return true if fields were successfully set, false if credentials are
     *     missing or unavailable
     * @throws IbcException if a required control cannot be found or updated
     */
    protected abstract boolean setFields(Window window, int eventID) throws IbcException;
    
    /**
     * Determines whether the username field is disabled or absent in the
     * login frame.
     *
     * @param window the login frame
     * @return true if the username field is not available for input, false
     *     otherwise
     */
    protected abstract boolean isUserIdDisabledOrAbsent(Window window);

    /**
     * Determines whether the password field is disabled or absent in the
     * login frame.
     *
     * @param window the login frame
     * @return true if the password field is not available for input, false
     *     otherwise
     */
    protected abstract boolean isPasswordDisabledOrAbsent(Window window);

    /**
     * Locates the login button in the login frame by searching for common button text variations.
     * Tries "Login", "Log In", and "Paper Log In" in order.
     *
     * @param window the login frame
     * @return the login button, or null if not found
     */
    private JButton findLoginButton(final Window window) {
        JButton b = SwingUtils.findButton(window, "Login");
        if (b == null) b = SwingUtils.findButton(window, "Log In");
        if (b == null) b = SwingUtils.findButton(window, "Paper Log In");
        return b;
    }

    /**
     * Directs focus to a credential field that is missing or empty.
     * Used to prompt the user or external process to provide the required credential.
     *
     * @param window the login frame
     * @param credentialIndex the field index (0 for username, 1 for password)
     */
    protected final void setMissingCredential(final Window window, final int credentialIndex) {
        SwingUtils.findTextField(window, credentialIndex).requestFocus();
    }

    /**
     * Sets a credential value in the login frame by updating the specified text field.
     *
     * @param window the login frame
     * @param credentialName the name of the credential (for error messages)
     * @param credentialIndex the field index (0 for username, 1 for password)
     * @param value the credential value to set
     * @throws IbcException if the text field cannot be found or updated
     */
    protected final void setCredential(final Window window,
                                            final String credentialName,
                                            final int credentialIndex,
                                            final String value) throws IbcException {
        if (! SwingUtils.setTextField(window, credentialIndex, value)) throw new IbcException(credentialName);
    }

    /**
     * Sets the trading mode (Live Trading or Paper Trading) in the login frame.
     * Handles both toggle button (TWS 974+) and combo box (earlier versions) UI patterns.
     *
     * @param window the login frame
     * @return true if trading mode was set successfully, false if the dialog is invalid and requires cold restart
     */
    protected final boolean setTradingMode(final Window window) {
        String tradingMode = TradingModeManager.tradingModeManager().getTradingMode();

        if (SwingUtils.findToggleButton(window, "Live Trading") != null && 
                SwingUtils.findToggleButton(window, "Paper Trading") != null) {
            // TWS 974 onwards uses toggle buttons rather than a combo box
            Utils.logToConsole("Setting Trading mode = " + tradingMode);
            if (tradingMode.equalsIgnoreCase(TradingModeManager.TRADING_MODE_LIVE)) {
                SwingUtils.findToggleButton(window, "Live Trading").doClick();
            } else {
                SwingUtils.findToggleButton(window, "Paper Trading").doClick();
            }
            return true;
        } else {
            // the dialog appears to have been deconstructed, stop tidily
            // and do a cold restart
            
            Utils.logToConsole("Login dialog has been invalidated - initiate cold restart");
            MyCachedThreadPool.getInstance().execute(new StopTask(null, true, "Login Error dialog encountered"));
            return false;
        }
    }
    
}
