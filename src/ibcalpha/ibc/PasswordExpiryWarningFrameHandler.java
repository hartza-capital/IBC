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
import javax.swing.JFrame;

public class PasswordExpiryWarningFrameHandler  implements WindowHandler {
    /**
     * Determines whether to process window events for the password expiry warning frame.
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED or WINDOW_ACTIVATED event, false otherwise
     */
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_ACTIVATED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the password expiry warning frame by dismissing it if DismissPasswordExpiryWarning is enabled.
     * @param window the frame window
     * @param eventID the AWT window event ID
     */
    public void handleWindow(Window window, int eventID) {
        if (! Settings.settings().getBoolean("DismissPasswordExpiryWarning", false)) return;
        if (! SwingUtils.clickButton(window, "OK")) {
            Utils.logError("could not dismiss Password Expiry Warning because we could not find one of the controls.");
        }
    }

    /**
     * Identifies the password expiry warning frame by checking the window title.
     * @param window the window to identify
     * @return true if this is the password expiry warning frame, false otherwise
     */
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame)) return false;

        return (SwingUtils.titleContains(window, "Password Notice"));
    }
}
