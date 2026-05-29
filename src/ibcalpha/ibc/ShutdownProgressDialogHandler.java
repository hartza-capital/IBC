// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2021 Richard L King (rlking@aultan.com)
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

public class ShutdownProgressDialogHandler implements WindowHandler {
    /**
     * Determines whether to process window events for the shutdown progress dialog.
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
     * Handles the shutdown progress dialog by shutting down executor services and the command server.
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    public void handleWindow(Window window, int eventID) {
        MyCachedThreadPool.getInstance().shutdownNow();
        MyScheduledExecutorService.getInstance().shutdownNow();
        CommandServer.commandServer().shutdown();
    }

    /**
     * Identifies the shutdown progress dialog by checking the window title.
     * @param window the window to identify
     * @return true if this is the shutdown progress dialog, false otherwise
     */
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return (SwingUtils.titleContains(window, "Shutdown progress"));
    }

  
}
