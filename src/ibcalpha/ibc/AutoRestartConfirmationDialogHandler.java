// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2022 Richard L King (rlking@aultan.com)
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

import java.awt.Container;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * Window handler for the dialog with which TWS/Gateway asks for confirmation before
 * restarting the trading platform automatically, confirming it so the restart happens.
 */
public class AutoRestartConfirmationDialogHandler implements WindowHandler {

    /** Text that identifies the automatic restart confirmation dialog. */
    private static final String AUTO_RESTART_MESSAGE = "trading platform restart automatically";

    /**
     * Determines whether to process window events for the automatic restart dialog.
     *
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED event, false otherwise
     */
    @Override
    public boolean filterEvent(Window window, int eventId) {
        return eventId == WindowEvent.WINDOW_OPENED;
    }

    /**
     * Confirms the automatic restart by clicking the dialog's OK button, logging an
     * error when the button cannot be found.
     *
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    @Override
    public void handleWindow(Window window, int eventID) {
        if (!SwingUtils.clickButton(window, "OK")) {
            Utils.logError("could not dismiss AutoRestartConfirmation because we could not find one of the controls.");
        }
    }

    /**
     * Identifies the automatic restart confirmation dialog by its message.
     *
     * @param window the window to identify
     * @return true if this is the automatic restart confirmation dialog, false otherwise
     */
    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        return containsAutoRestartMessage(window);
    }

    /**
     * Reports whether the container holds the automatic restart message, either in a
     * text pane or as the message of an option pane.
     *
     * @param container the container to search
     * @return true if the automatic restart message was found, false otherwise
     */
    static boolean containsAutoRestartMessage(Container container) {
        if (SwingUtils.findTextPane(container, AUTO_RESTART_MESSAGE) != null) return true;

        JOptionPane optionPane = SwingUtils.findOptionPane(container);
        return optionPane != null
                && optionPane.getMessage() != null
                && optionPane.getMessage().toString().contains(AUTO_RESTART_MESSAGE);
    }
}
