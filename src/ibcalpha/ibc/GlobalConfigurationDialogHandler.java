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
 * Window handler for the Global Configuration dialog, registering it when it opens and clearing it
 * when it closes.
 */
class GlobalConfigurationDialogHandler implements WindowHandler {
    /**
     * Determines whether this handler should act on the given window event.
     * Acts on both WINDOW_OPENED and WINDOW_CLOSED events.
     *
     * @param window the AWT window associated with the event.
     * @param eventId the AWT event ID.
     * @return true if this handler should process the event; false otherwise.
     */
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the Global Configuration dialog lifecycle.
     * Registers the dialog when opened and clears it when closed.
     *
     * @param window the Global Configuration dialog.
     * @param eventId the AWT event ID (WINDOW_OPENED or WINDOW_CLOSED).
     */
    @Override
    public void handleWindow(final Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                ConfigDialogManager.configDialogManager().setConfigDialog((JDialog) window);
                break;
            case WindowEvent.WINDOW_CLOSED:
                ConfigDialogManager.configDialogManager().clearConfigDialog();
                break;
            default:
                // other window events carry no configuration state change
                break;
        }
    }

    /**
     * Identifies whether the window is the Global Configuration dialog.
     *
     * @param window the window to identify.
     * @return true if this is the Global Configuration dialog; false otherwise.
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return (SwingUtils.titleContains(window, "Configuration"));
    }
}
