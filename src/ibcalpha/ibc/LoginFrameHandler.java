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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class LoginFrameHandler extends AbstractLoginHandler {

    /**
     * Identifies the TWS/Gateway Login frame by checking window type, title,
     * and presence of login button. Recognizes "New Login" or "Login" frames
     * that have a "Log In" or "Paper Log In" button, or any login frame during
     * an auto-restart session.
     *
     * @param window the window to check
     * @return true if the window is the TWS/Gateway login frame, false
     *         otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JFrame)) {
            return false;
        }

        // we check for the presence of the Login button because TWS displays a
        // different (information-only) dialog, also entitled Login, when it's
        // trying to reconnect. (Not sure if this is still true)
        // Also when doing autorestart there is no login button.
        return ((SwingUtils.titleEquals(window, "New Login")
                || SwingUtils.titleEquals(window, "Login"))
                && (SwingUtils.findButton(window, "Log In") != null
                || SwingUtils.findButton(window, "Paper Log In") != null
                || SessionManager.isRestart()));
    }

    private boolean listeningForUsernameChange;

    /**
     * Initializes the login frame by setting the trading mode and attaching a
     * DocumentListener to monitor and sync the "Use/store settings on server"
     * checkbox when the username changes.
     *
     * @param window the login frame
     * @param eventID the AWT event ID
     * @return true if initialization succeeds, false if the dialog is invalid
     * @throws IbcException if the username field cannot be found
     */
    @Override
    protected boolean initialise(final Window window, final int eventID)
            throws IbcException {
        if (!setTradingMode(window)) {
            return false;
        }

        // because TWS/Gateway modify the jts.ini file before this point
        JtsIniManager.reload();

        final JTextField userName = SwingUtils.findTextField(window, 0);
        if (userName == null) {
            throw new IbcException("Username field");
        }

        if (!listeningForUsernameChange) {
            listeningForUsernameChange = true;

            // Add a DocumentListener to the username field, which will set the
            // "Use/store settings on server" checkbox as required. This is
            // necessary because when a valid username is entered, TWS sets the
            // checkbox according to the latest saved settings for that user,
            // which may not be what is now required by the
            // StoreSettingsOnServer setting.
            userName.getDocument().addDocumentListener(new DocumentListener() {
                /**
                 * Invoked when text is inserted into the username field.
                 * Triggers synchronization of the "Use/store settings on
                 * server" checkbox.
                 *
                 * @param de the document event
                 */
                @Override
                public void insertUpdate(final DocumentEvent de) {
                    setStoreSettingsOnServerCheckbox();
                }

                /**
                 * Invoked when text is removed from the username field.
                 * Currently a no-op; synchronization is only triggered on
                 * insert or change.
                 *
                 * @param de the document event
                 */
                @Override
                public void removeUpdate(final DocumentEvent de) {
                    //setStoreSettingsOnServerCheckbox();
                }

                /**
                 * Invoked when the username field is otherwise updated
                 * (attribute change). Triggers synchronization of the
                 * "Use/store settings on server" checkbox.
                 *
                 * @param de the document event
                 */
                @Override
                public void changedUpdate(final DocumentEvent de) {
                    setStoreSettingsOnServerCheckbox();
                }

                /**
                 * Synchronizes the "Use/store settings on server" checkbox
                 * based on the StoreSettingsOnServer configuration. Deferred
                 * to avoid being overwritten by TWS's own setting logic.
                 */
                private void setStoreSettingsOnServerCheckbox() {
                    String storeSettingsOnServerSetting = Settings.settings()
                            .getString("StoreSettingsOnServer", "");
                    if (storeSettingsOnServerSetting.length() != 0) {
                        final String storeSettingsOnServerCheckbox =
                                "Use/store settings on server";

                        // we defer setting the checkbox: if we do it inline,
                        // TWS's setting overwrites it
                        GuiDeferredExecutor.instance().execute(() -> {
                            boolean storeSettingsOnServer = Settings.settings()
                                    .getBoolean("StoreSettingsOnServer", false);
                            if (!SwingUtils.setCheckBoxSelected(
                                    window,
                                    storeSettingsOnServerCheckbox,
                                    storeSettingsOnServer)) {
                                // this is expected when autorestarting, so we
                                // just log the fact
                                Utils.logToConsole(
                                        "could not find control: "
                                        + storeSettingsOnServerCheckbox
                                        + ": this is expected when restarting");
                            } else {
                                String msg = "Use/store settings on server "
                                        + "selected: " + storeSettingsOnServer;
                                Utils.logToConsole(msg);
                            }
                        });
                    }
                }

            });
        }
        return true;
    }

    /**
     * Pre-login validation that ensures both username and password are
     * provided before login. Requests focus on the missing credential field if
     * either is empty.
     *
     * @param window the login frame
     * @param eventID the AWT event ID
     * @return true if both credentials are present, false if a credential
     *         field needs user input
     * @throws IbcException if a credential cannot be set
     */
    @Override
    protected boolean preLogin(final Window window, final int eventID)
            throws IbcException {
        if (LoginManager.loginManager().ibapiUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().ibapiPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else {
            return true;
        }
        return false;
    }

    /**
     * Populates the username and password fields in the login frame.
     *
     * @param window the login frame
     * @param eventID the AWT event ID
     * @return true if both fields are set successfully
     * @throws IbcException if either username or password field cannot be
     *         found or updated
     */
    @Override
    protected boolean setFields(final Window window, final int eventID)
            throws IbcException {
        Utils.logToConsole("Setting user name");
        setCredential(window, "IBAPI user name", 0,
                LoginManager.loginManager().ibapiUserName());
        Utils.logToConsole("Setting password");
        setCredential(window, "IBAPI password", 1,
                LoginManager.loginManager().ibapiPassword());
        return true;
    }

    /**
     * Checks whether the username field is disabled or not present in the
     * login frame.
     *
     * @param window the login frame
     * @return true if the username field is not found or is disabled, false if
     *         it is enabled
     */
    @Override
    protected boolean isUserIdDisabledOrAbsent(final Window window) {
        JTextField userID = SwingUtils.findTextField(window, 0);
        if (userID == null) {
            return true;
        }
        return !userID.isEnabled();
    }

    /**
     * Checks whether the password field is disabled or not present in the
     * login frame.
     *
     * @param window the login frame
     * @return true if the password field is not found or is disabled, false if
     *         it is enabled
     */
    @Override
    protected boolean isPasswordDisabledOrAbsent(final Window window) {
        JTextField password = SwingUtils.findTextField(window, 1);
        if (password == null) {
            return true;
        }
        return !password.isEnabled();
    }

}

