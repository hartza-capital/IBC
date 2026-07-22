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

class AcceptIncomingConnectionDialogHandler implements WindowHandler {
    /**
     * Determines whether to process window events for the accept incoming connection dialog.
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED event, false otherwise
     */
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the accept incoming connection dialog based on the AcceptIncomingConnectionAction setting.
     * Accepts, rejects, or leaves the dialog for manual handling depending on configuration.
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    public void handleWindow(Window window, int eventID) {
        final String Accept = "accept";
        final String Reject = "reject";
        final String Manual = "manual";

        String acceptIncomingConnectionAction = Settings.settings().getString("AcceptIncomingConnectionAction", Manual);

        if (acceptIncomingConnectionAction.equalsIgnoreCase(Manual)) return;

        if (acceptIncomingConnectionAction.equalsIgnoreCase(Accept)) {
            // Try "OK", then "Yes"; short-circuiting means at most one click,
            // and the error is logged only if neither control is present.
            if (!SwingUtils.clickButton(window, "OK")
                    && !SwingUtils.clickButton(window, "Yes")) {
                Utils.logError("could not accept incoming connection because we could not find one of the controls.");
            }
        } else if (acceptIncomingConnectionAction.equalsIgnoreCase(Reject)) {
            if (!SwingUtils.clickButton(window, "No")) {
                Utils.logError("could not accept incoming connection because we could not find one of the controls.");
            }
        } else {
            Utils.logError("could not accept incoming connection because the AcceptIncomingConnectionAction setting is invalid.");
        }
    }

    /**
     * Identifies the accept incoming connection dialog by looking for a specific label.
     * @param window the window to identify
     * @return true if this is the accept incoming connection dialog, false otherwise
     */
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return (SwingUtils.findLabel(window, "Accept incoming connection") != null);
    }
}

