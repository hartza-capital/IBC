// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2025 Richard L King (rlking@aultan.com)
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
import javax.swing.JDialog;

public class GatewayDialogHandler implements WindowHandler {

    /**
     * Determines whether this handler should act on the given window event.
     *
     * @param window the AWT window associated with the event.
     * @param eventId the AWT event ID (e.g., WINDOW_OPENED).
     * @return true if this handler should process the event; false otherwise.
     */
    @Override
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Performs the automation action for a recognized Gateway dialog.
     * Handles "Connection to server failed" dialogs by initiating a
     * cold restart; other dialogs are logged but left for user
     * interaction.
     *
     * @param window the Gateway dialog window.
     * @param eventID the AWT event ID.
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        String text = SwingUtils.getLabelTexts(window);
        // since this is a generic dialog, we always log the text
        Utils.logToConsole(text);
        // For other instances of this dialog, leave it on display
        // for the user to handle (e.g., setting Trusted IP addresses).
        if (text.startsWith("Connection to server failed")) {
            Utils.logToConsole("Cold restart in progress");
            // stop tidily and do a cold restart
            MyCachedThreadPool.getInstance()
                .execute(new StopTask(null, true,
                    "Cold restart after Connection to server failed"));

            if (!SwingUtils.clickButton(window, "OK")) {
                Utils.logError(
                    "could not dismiss Login Error dialog because we "
                        + "could not find the OK button");
            }
        }
    }

    /**
     * Identifies whether the window is a Gateway dialog.
     * Checks if the window is a JDialog with "Gateway" in its title.
     *
     * @param window the window to identify.
     * @return true if this is a recognized Gateway dialog; false otherwise.
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return SwingUtils.titleContains(window, "Gateway");
    }
}
