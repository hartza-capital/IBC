// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2019 Richard L King (rlking@aultan.com)
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
 * Handles the Trading Login Handoff dialog, which confirms whether to continue
 * this session or hand over to the other one.
 */
public class TradingLoginHandoffDialogHandler implements WindowHandler {
    /** Title text that identifies the Trading Login Handoff dialog. */
    final String DIALOG_TITLE = "Trading Login Handoff";

    /**
     * Determines whether this handler should act on the given window event.
     *
     * @param window the AWT window associated with the event.
     * @param eventId the AWT event ID (e.g., WINDOW_OPENED).
     * @return true if this handler should process the event; false otherwise.
     */
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the Trading Login Handoff dialog based on the ExistingSessionDetectedAction setting.
     * Routes to disconnecting the other session, exiting with error, or manual handling based on configuration.
     *
     * @param window the Trading Login Handoff dialog.
     * @param eventID the AWT event ID.
     */
    @Override
    public void handleWindow(Window window, int eventID) {
        final String MESSAGE_STUB = DIALOG_TITLE + " dialog; ";
        final String INFO_MESSAGE = MESSAGE_STUB + "{}";
        final String ERROR_MESSAGE = MESSAGE_STUB + "error: {}";
        final String COULD_NOT_HANDLE_MESSAGE = MESSAGE_STUB + "could not handle because {}";

        String setting = Settings.settings().getString("ExistingSessionDetectedAction", "manual");
        if (setting.equalsIgnoreCase("primary")) {
            Utils.logToConsole(String.format(INFO_MESSAGE, "end the other session and continue this one"));
            if (!SwingUtils.clickButton(window, "Disconnect Other Session"))  {
                Utils.logError(String.format(COULD_NOT_HANDLE_MESSAGE, "the 'Disconnect Other Session' button wasn't found."));
            }
        } else if (setting.equalsIgnoreCase("primaryoverride")) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE, String.format(ERROR_MESSAGE, "ExistingSessionDetectedAction=primaryoverride"));
        } else if (setting.equalsIgnoreCase("secondary")) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE, String.format(ERROR_MESSAGE, "ExistingSessionDetectedAction=secondary"));
        } else if (setting.equalsIgnoreCase("manual")) {
            Utils.logToConsole(String.format(INFO_MESSAGE, "user must choose whether to continue with this session"));
            // nothing to do
        } else {
            Utils.logError(String.format(COULD_NOT_HANDLE_MESSAGE, "the 'ExistingSessionDetectedAction' setting is invalid."));
        }
    }

    /**
     * Identifies the Trading Login Handoff dialog by its title.
     *
     * @param window the window to identify.
     * @return true if this is the Trading Login Handoff dialog; false otherwise.
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return (SwingUtils.titleContains(window, DIALOG_TITLE));
    }
}
