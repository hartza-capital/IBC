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

import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;

public class RestartConfirmationDialogHandler implements WindowHandler {
    /**
     * Determines whether to process window events for the restart
     * confirmation dialog.
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED event, false otherwise
     */
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the restart confirmation dialog by confirming the restart.
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    public void handleWindow(final Window window, final int eventID) {
        if (!SwingUtils.clickButton(window, "Yes")) {
            Utils.logError("could not ignore shutdown confirmation "
                    + "dialog because we could not find one of the "
                    + "controls.");
        }
    }

    /**
     * Identifies the restart confirmation dialog by checking for a
     * specific label.
     * @param window the window to identify
     * @return true if this is the restart confirmation dialog, false
     * otherwise
     */
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return (SwingUtils.findLabel(window,
                "Are you sure you would like to restart "
                + "the application") != null);
    }
}
