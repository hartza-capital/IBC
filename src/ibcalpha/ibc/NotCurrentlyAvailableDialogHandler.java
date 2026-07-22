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
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;

class NotCurrentlyAvailableDialogHandler implements WindowHandler {

    /**
     * Determines whether to process window events for the "not currently
     * available" dialog.
     *
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED or WINDOW_ACTIVATED event;
     *     false otherwise
     */
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_ACTIVATED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the "not currently available" dialog by closing it and the login
     * frame.
     *
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    public void handleWindow(final Window window, final int eventID) {
        if (!SwingUtils.clickButton(window, "OK")) {
            Utils.logError("The system is not currently available.");
            return;
        }

        if (LoginManager.loginManager().getLoginFrame() != null) {
            final JButton loginButton = SwingUtils.findButton(
                    LoginManager.loginManager().getLoginFrame(), "Login");
            loginButton.requestFocus();
            final KeyEvent keyEvent = new KeyEvent(
                    loginButton,
                    KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(),
                    KeyEvent.ALT_DOWN_MASK,
                    KeyEvent.VK_F4,
                    KeyEvent.CHAR_UNDEFINED);
            loginButton.dispatchEvent(keyEvent);
        }
    }

    /**
     * Identifies the "not currently available" dialog by checking for a login
     * window with specific text.
     *
     * @param window the window to identify
     * @return true if this is the "not currently available" dialog;
     *     false otherwise
     */
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return SwingUtils.titleContains(window, "Login")
                && SwingUtils.findLabel(window, "not currently available")
                        != null;
    }
}

