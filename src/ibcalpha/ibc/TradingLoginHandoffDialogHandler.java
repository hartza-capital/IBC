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
 * After the 'Existing Session Detected' dialog is handled (whether by the user
 * or by IBC) such that this session is continued, AND the user is enrolled in
 * IB's 2 Factor Authentication scheme, then the 'Second Factor Authentication'
 * dialog is displayed (IBC does not handle this dialog). Once the user has
 * completed the 2FA, the 'Trading Login Handoff' dialog is displayed to
 * confirm the user's intention to continue the session or hand over to the
 * other session (ie the user can potentially change their mind).
 *
 * As far as IBC is concerned, it must simply do what is specified in the
 * ExistingSessionDetectedAction setting. However if this setting is not
 * 'manual', then IBC must have elected to continue this session when handling
 * the ExistingSessionDetectedAction (otherwise this dialog would not have been
 * displayed). So the only relevant cases here are:
 *
 *  ExistingSessionDetectedAction=manual (nothing for IBC to do)
 *  ExistingSessionDetectedAction=primary (IBC must again continue this session)
 *
 * If ExistingSessionDetectedAction is set to 'primaryoverride' or 'secondary'
 * then something unexpected has happened, and we crash out.
 *
 */
public class TradingLoginHandoffDialogHandler implements WindowHandler {
    /** Substring matched against the window title to recognise this dialog. */
    private static final String DIALOG_TITLE = "Trading Login Handoff";

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
     * Handles the Trading Login Handoff dialog based on the
     * ExistingSessionDetectedAction setting. Routes to disconnecting the
     * other session, exiting with error, or manual handling based on
     * configuration.
     *
     * @param window the Trading Login Handoff dialog.
     * @param eventID the AWT event ID.
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        final String messageStub = DIALOG_TITLE + " dialog; ";
        final String infoMessage = messageStub + "{}";
        final String errorMessage = messageStub + "error: {}";
        final String couldNotHandleMessage = messageStub
                + "could not handle because {}";

        String setting = Settings.settings()
                .getString("ExistingSessionDetectedAction", "manual");
        if (setting.equalsIgnoreCase("primary")) {
            Utils.logToConsole(String.format(infoMessage,
                    "end the other session and continue this one"));
            if (!SwingUtils.clickButton(window, "Disconnect Other Session")) {
                Utils.logError(String.format(couldNotHandleMessage,
                        "the 'Disconnect Other Session' button wasn't found."));
            }
        } else if (setting.equalsIgnoreCase("primaryoverride")) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    String.format(errorMessage,
                            "ExistingSessionDetectedAction=primaryoverride"));
        } else if (setting.equalsIgnoreCase("secondary")) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    String.format(errorMessage,
                            "ExistingSessionDetectedAction=secondary"));
        } else if (setting.equalsIgnoreCase("manual")) {
            Utils.logToConsole(String.format(infoMessage,
                    "user must choose whether to continue with this session"));
            // nothing to do
        } else {
            Utils.logError(String.format(couldNotHandleMessage,
                    "the 'ExistingSessionDetectedAction' setting is invalid."));
        }
    }

    /**
     * Identifies the Trading Login Handoff dialog by its title.
     *
     * @param window the window to identify.
     * @return true if this is the Trading Login Handoff dialog; false
     *     otherwise.
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return (SwingUtils.titleContains(window, DIALOG_TITLE));
    }
}
