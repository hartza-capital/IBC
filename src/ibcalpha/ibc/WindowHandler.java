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

/**
 * Defines the contract for GUI automation handlers that respond to Swing window events.
 * Implementations identify specific dialogs/frames and automate their interaction in response
 * to AWT events.
 */
interface WindowHandler {
    /**
     * Determines whether the handler should process the given AWT event for this window.
     * Implementations use this to filter events before expensive window recognition or
     * automation occurs.
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID (e.g. WindowEvent.WINDOW_OPENED)
     * @return true if the handler should process this event, false to ignore it
     */
    boolean filterEvent(Window window, int eventId);

    /**
     * Performs the automation appropriate for this window in response to the given event.
     * Called only after {@link #filterEvent} and {@link #recogniseWindow} have both matched.
     *
     * @param window the window to automate
     * @param eventID the AWT event ID that triggered this call
     */
    void handleWindow(Window window, int eventID);

    /**
     * Determines whether this handler is responsible for the given window.
     * Implementations typically match by title text or a distinctive component structure.
     *
     * @param window the window to inspect
     * @return true if this handler recognises and should handle the window, false otherwise
     */
    boolean recogniseWindow(Window window);
}
