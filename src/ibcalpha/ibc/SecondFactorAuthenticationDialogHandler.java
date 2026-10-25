// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2021 Richard L King (rlking@aultan.com)
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

import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.security.GeneralSecurityException;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListModel;

/**
 * Handles the Second Factor Authentication dialog, performing read-only login,
 * second factor device selection or automatic TOTP submission.
 */
public class SecondFactorAuthenticationDialogHandler implements WindowHandler {

    /** Index of the passcode text field in the 2FA dialog. */
    private static final int TOTP_CODE_FIELD_INDEX = 0;

    /**
     * Private constructor prevents external instantiation of this singleton
     * handler; use {@link #getInstance()} to obtain the shared instance.
     */
    private SecondFactorAuthenticationDialogHandler() {};

    /** Shared handler instance returned by {@link #getInstance()}. */
    static SecondFactorAuthenticationDialogHandler _secondFactorAuthenticationDialogHandler = new SecondFactorAuthenticationDialogHandler();

    /**
     * Returns the singleton instance of SecondFactorAuthenticationDialogHandler.
     *
     * @return the singleton handler instance
     */
    static SecondFactorAuthenticationDialogHandler getInstance() {
        return _secondFactorAuthenticationDialogHandler;
    }
    
    /**
     * Determines whether to process the given AWT event for this window.
     * Handles both WINDOW_OPENED and WINDOW_CLOSED events.
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID
     * @return true if eventId is WINDOW_OPENED or WINDOW_CLOSED, false otherwise
     */
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the Second Factor Authentication dialog by processing either a WINDOW_OPENED or WINDOW_CLOSED event.
     * On WINDOW_OPENED: handles read-only login, device selection, or TOTP generation.
     * On WINDOW_CLOSED: updates the login state accordingly.
     *
     * @param window the 2FA dialog
     * @param eventID the AWT event ID (WINDOW_OPENED or WINDOW_CLOSED)
     */
    @Override
    public void handleWindow(Window window, int eventID) {
        if (eventID == WindowEvent.WINDOW_OPENED) {
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                doReadonlyLogin(window);
            } else if (secondFactorDeviceSelectionRequired(window)) {
                selectSecondFactorDevice(window);
            } else {
                doValidate2FA(window);
            }
        } else if (eventID == WindowEvent.WINDOW_CLOSED) {
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }
            LoginManager.loginManager().secondFactorAuthenticationDialogClosed();
        }
    }

    /**
     * Fills the passcode field of the 2FA dialog with a freshly generated TOTP
     * code, leaving the dialog itself untouched.
     *
     * @param container the 2FA dialog, or any component tree holding its passcode field
     * @return true if the passcode field was filled, false if the code could not
     *     be generated or the field could not be found
     */
    boolean fillTotpCode(final Container container) {
        final String secret = Settings.settings().getString("Ib2FA", "");
        if (secret.length() == 0) {
            Utils.logError("You should specify the required 2FA secret using the Ib2FA setting in config.ini");
            return false;
        }

        final String totpCode;
        try {
            totpCode = Totp.generate(secret, Totp.timeStepCounter(System.currentTimeMillis()));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            Utils.logError("the Ib2FA setting does not hold a usable Base32 TOTP secret: " + e.getMessage());
            return false;
        }

        if (! SwingUtils.setTextField(container, TOTP_CODE_FIELD_INDEX, totpCode)) {
            Utils.logError("could not find the passcode field of the second factor authentication dialog");
            return false;
        }
        return true;
    }

    /**
     * Moves the login state to TWO_FA_IN_PROGRESS and submits the dialog once a generated TOTP
     * code has been entered.
     *
     * @param window the 2FA dialog
     */
    private void doValidate2FA(final Window window) {
        LoginManager.loginManager().setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);

        if (! fillTotpCode(window)) {
            Utils.logError("second factor authentication dialog left open: no passcode was submitted");
            return;
        }

        GuiDeferredExecutor.instance().execute(() -> {
            Utils.logToConsole("Second factor authentication dialog submitted");
            SwingUtils.clickButton(window, "OK");
        });
    }

    /**
     * Identifies the Second Factor Authentication dialog by window type and title.
     * Recognizes both JDialog (Gateway) and JFrame (TWS) variants.
     *
     * @param window the window to check
     * @return true if the window is a Second Factor Authentication dialog, false otherwise
     */
    @Override
    public boolean recogniseWindow(Window window) {
        // For TWS this window is a JFrame; for Gateway it is a JDialog
        if (! (window instanceof JDialog || window instanceof JFrame)) return false;

        return SwingUtils.titleContains(window, "Second Factor Authentication") ||
            SwingUtils.titleContains(window, "Security Code Card Authentication");
    }

    /**
     * Initiates a read-only login by clicking the "Enter Read Only" button in the 2FA dialog.
     *
     * @param window the 2FA dialog
     */
    private void doReadonlyLogin(Window window){
        if (SwingUtils.clickButton(window, "Enter Read Only")) {
            Utils.logToConsole("initiating read-only login.");
        } else {
            Utils.logError("could not initiate read-only login.");
        }
    }
    
    /**
     * Checks whether the 2FA dialog requires the user to select a 2FA method.
     * This occurs when multiple 2FA methods are enabled.
     *
     * @param window the 2FA dialog
     * @return true if device selection is required, false otherwise
     */
    private boolean secondFactorDeviceSelectionRequired(Window window) {
        // this area appears in the Second Factor Authentication dialog when the
        // user has enabled more than one second factor authentication method

        return (SwingUtils.findTextArea(window, "Select second factor device") != null);
    }
    
    /**
     * Selects the configured second factor device from the 2FA method selection list and submits the dialog.
     * The device name must match a value from the SecondFactorDevice configuration setting.
     *
     * @param window the 2FA dialog
     */
    private void selectSecondFactorDevice(Window window) {
        JList<?> deviceList = SwingUtils.findList(window, 0);
        if (deviceList == null) {
            Utils.exitWithError(ErrorCodes.CANT_FIND_CONTROL, "could not find second factor device list.");
            return;
        }

        String secondFactorDevice = Settings.settings().getString("SecondFactorDevice", "");
        if (secondFactorDevice.length() == 0) {
            Utils.logError("You should specify the required second factor device using the SecondFactorDevice setting in config.ini");
            return;
        }

        ListModel<?> model = deviceList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            String entry = model.getElementAt(i).toString().trim();
            if (entry.equals(secondFactorDevice)) {
                deviceList.setSelectedIndex(i);

                if (!SwingUtils.clickButton(window, "OK")) {
                    Utils.logError("could not select second factor device: OK button not found");
                }
                return;
            }
        }
        Utils.logError("could not find second factor device '" + secondFactorDevice + "' in the list");
    }
}
