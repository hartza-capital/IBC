// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2022 Richard L King (rlking@aultan.com)
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
import javax.swing.JButton;
import javax.swing.JDialog;

/**
 * Fallback window handler for the paper-trading login warning dialog, recognised by
 * its plain "Warning" title and dismissed through its acknowledgement button.
 */
public class LoginWarningDialogHandler implements WindowHandler {

    /** Title the vendor gives the paper-trading login warning dialog (i18n key {@code Warning}). */
    private static final String LOGIN_WARNING_TITLE = "Warning";

    /** Caption of the button that acknowledges the warning. */
    private static final String ACKNOWLEDGEMENT_BUTTON = "I understand and accept";

    /**
     * Determines whether to process window events for the login warning dialog.
     *
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true for the open and close events of the dialog, false otherwise
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
     * Acknowledges the warning when the operator accepts the paper-trading warning,
     * and records the close of the dialog so the Gateway initialisation barrier is
     * released.
     *
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    @Override
    public void handleWindow(Window window, int eventID) {
        if (eventID == WindowEvent.WINDOW_CLOSED) {
            SessionManager.setNonBrokerageAccountDialogClosed();
            return;
        }

        if (! Settings.settings().getBoolean("AcceptNonBrokerageAccountWarning", true)) return;

        JButton acknowledgementButton = findAcknowledgementButton(window);
        if (acknowledgementButton == null) {
            Utils.logToConsole("login warning dialog offers no '"
                    + ACKNOWLEDGEMENT_BUTTON
                    + "' button: leaving it on display");
            return;
        }

        SwingUtils.clickButton(acknowledgementButton);
    }

    /**
     * Identifies the paper-trading login warning dialog by its title.
     *
     * @param window the window to identify
     * @return true if this is a dialog titled "Warning", false otherwise
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return SwingUtils.titleEquals(window, LOGIN_WARNING_TITLE);
    }

    /**
     * Reports whether the container offers the button that acknowledges the warning.
     *
     * @param container the container to search, typically the dialog itself
     * @return the acknowledgement button, or null when there is none
     */
    static JButton findAcknowledgementButton(Container container) {
        return SwingUtils.findButton(container, ACKNOWLEDGEMENT_BUTTON);
    }
}
