// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2020 Richard L King (rlking@aultan.com)
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

public class LoginFailedDialogHandler implements WindowHandler  {
    final String DIALOG_TITLE = "Login failed";

    /**
     * Determines whether to process the given AWT event for this window.
     * Only handles WINDOW_OPENED events.
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID
     * @return true if eventId is WINDOW_OPENED, false otherwise
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
     * Handles the Login Failed dialog by logging the failure and initiating a cold restart.
     * Clicks the OK button to dismiss the dialog before stopping.
     *
     * @param window the Login Failed dialog
     * @param eventID the AWT event ID
     */
    @Override
    public void handleWindow(Window window, int eventID) {
        Utils.logToConsole("Login failed");
        Utils.logToConsole("Cold restart in progress");
        // stop tidily and do a cold restart
        MyCachedThreadPool.getInstance().execute(new StopTask(null, true, "Cold restart after Login Failed dialog encountered"));

        if (! SwingUtils.clickButton(window, "OK")) {
            Utils.logError("could not dismiss Login Failed dialog because we could not find the OK button");
        }
    }

    /**
     * Identifies the Login Failed dialog by checking the window type and title.
     *
     * @param window the window to check
     * @return true if the window is a JDialog with "Login failed" in its title, false otherwise
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return (SwingUtils.titleContains(window, DIALOG_TITLE));
    }

}
