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
import javax.swing.JDialog;

/**
 * Handles the Security Code Card Authentication dialog.
 */
public class SecurityCodeDialogHandler implements WindowHandler {

    /**
     * Determines whether to process the given AWT event for this window.
     *
     * Only handles WINDOW_OPENED events.
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID
     * @return true if eventId is WINDOW_OPENED, false otherwise
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
     * Handles the Security Code Card Authentication dialog by initiating a
     * read-only login if the ReadOnlyLogin configuration setting is enabled.
     *
     * @param window the Security Code Card Authentication dialog
     * @param eventID the AWT event ID
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        if (!Settings.settings().getBoolean("ReadOnlyLogin", false)) {
            return;
        }

        if (SwingUtils.clickButton(window, "Enter Read Only")) {
            Utils.logToConsole("initiating read-only login.");
        } else {
            Utils.logError("could not initiate read-only login.");
        }
    }

    /**
     * Identifies the Security Code Card Authentication dialog by checking for
     * the "Enter Read Only" button.
     *
     * @param window the window to check
     * @return true if the window is a JDialog with an "Enter Read Only" button,
     *         false otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }
        return SwingUtils.findButton(window, "Enter Read Only") != null;
    }

}
