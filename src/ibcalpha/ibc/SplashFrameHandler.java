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

public final class SplashFrameHandler implements WindowHandler {

    /**
     * Determines whether this handler should act on the given window event.
     * Only acts on WINDOW_CLOSED because that indicates Gateway is ready to
     * handle menu commands.
     *
     * @param window the AWT window associated with the event.
     * @param eventId the AWT event ID.
     * @return true if this handler should process the event; false otherwise.
     */
    @Override
    public boolean filterEvent(final Window window, final int eventId) {
        /*
         * Note that we are only interested in the closing of the gateway splash
         * frame, because that indicates that the gateway is now in a
         * position to start handling menu commands.
         *
         * Note also that the splash frame's window title is repeatedly
         * changed during gateway initialisation, and it's only the last
         * title value that we use for recognising it
         */
        switch (eventId) {
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Marks the splash screen as closed and updates login state for Gateway.
     * For Gateway, sets login state to LOGGED_IN when the splash closes.
     *
     * @param window the splash frame window.
     * @param eventID the AWT event ID.
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        if (SessionManager.isGateway()) {
            LoginManager.loginManager()
                    .setLoginState(LoginManager.LoginState.LOGGED_IN);
        }

        SessionManager.setSplashScreenClosed();
    }

    /**
     * Identifies the splash frame by its "Starting application..." title.
     *
     * @param window the window to identify.
     * @return true if this is the splash frame; false otherwise.
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JFrame)) {
            return false;
        }

        return (SwingUtils.titleContains(window, "Starting application..."));
    }

}
