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

class ApiChangeConfirmationDialogHandler implements WindowHandler {

    /**
     * Determines whether to process window events for the API change
     * confirmation dialog. Processes WINDOW_OPENED events only if an API config
     * change confirmation is expected.
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED event and an API change
     *         confirmation is pending, false otherwise
     */
    @Override
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                if (ConfigDialogManager.configDialogManager()
                        .getApiConfigChangeConfirmationExpected()) {
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Handles the API change confirmation dialog by confirming the socket port
     * change. Clicks the Yes button to apply the new API socket port setting.
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        ConfigDialogManager.configDialogManager()
                .setApiConfigChangeConfirmationHandled();
        if (!SwingUtils.clickButton(window, "Yes")) {
            Utils.logError(
                    "can't apply new API socket port: 'Yes' button not found");
        }
    }

    /**
     * Identifies the API change confirmation dialog by checking for a specific
     * label.
     * @param window the window to identify
     * @return true if this is the API change confirmation dialog, false
     *         otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return SwingUtils.findLabel(window, "apply the new socket port setting")
                != null;
    }

}
