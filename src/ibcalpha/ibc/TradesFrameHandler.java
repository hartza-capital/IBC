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

import static ibcalpha.ibc.SwingUtils.findCheckBox;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Manages the Trades window lifecycle based on the ShowAllTrades setting.
 */
public class TradesFrameHandler implements WindowHandler {

    /** Records whether the first Trades window has been opened. */
    private boolean firstTradesWindowOpened;

    /** Whether the Trades window should display all trades. */
    private boolean showAllTrades;

    /**
     * Determines whether this handler should act on the given window event.
     *
     * Acts on WINDOW_OPENED, WINDOW_CLOSING, and WINDOW_CLOSED events.
     *
     * @param window the AWT window associated with the event
     * @param eventId the AWT event ID
     * @return true if this handler should process the event; false otherwise
     */
    @Override
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_CLOSING:
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Manages the Trades window lifecycle based on the ShowAllTrades setting.
     *
     * Opens and configures the Trades window to display all trades if enabled;
     * re-opens it if the user closes it while ShowAllTrades is active.
     *
     * @param window the Trades frame
     * @param eventID the AWT event ID (WINDOW_OPENED, WINDOW_CLOSING, or
     *                WINDOW_CLOSED)
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        if (!firstTradesWindowOpened) {
            showAllTrades = Settings.settings().getBoolean(
                    "ShowAllTrades", false);
        }
        if (!showAllTrades) {
            firstTradesWindowOpened = true;
            return;
        }
        if (eventID == WindowEvent.WINDOW_OPENED) {
            if (findCheckBox(window, "Sun") != null) {
                Utils.logToConsole("Setting trades log to show all trades");
                // TWS versions before 955
                SwingUtils.setCheckBoxSelected(window, "Sun", true);
                SwingUtils.setCheckBoxSelected(window, "Mon", true);
                SwingUtils.setCheckBoxSelected(window, "Tue", true);
                SwingUtils.setCheckBoxSelected(window, "Wed", true);
                SwingUtils.setCheckBoxSelected(window, "Thu", true);
                SwingUtils.setCheckBoxSelected(window, "Fri", true);
                SwingUtils.setCheckBoxSelected(window, "Sat", true);
                SwingUtils.setCheckBoxSelected(window, "All", true);

                monitorAllTradesCheckbox(window, "All");

                if (!firstTradesWindowOpened) {
                    if (Settings.settings().getBoolean(
                            "MinimizeMainWindow", false)) {
                        ((JFrame) window).setExtendedState(
                                java.awt.Frame.ICONIFIED);
                    }
                }
            } else {
                Utils.logToConsole(
                        "IBC can't set Trade History window to show all "
                        + "trades with this TWS version: user must do this");
                /*
                 * For TWS 955 onwards, IB have replaced the row of daily
                 * checkboxes with what appears visually to be a combo box:
                 * it is indeed derived from a JComboBox, but setting the
                 * selected item to 'Last 7 Days' doesn't have the desired
                 * effect.
                 *
                 * At present I don't see a way of getting round this, but
                 * the setting chosen by the user can now be persisted
                 * between sessions, so there is really no longer a need for
                 * 'ShowAllTrades'.
                 *
                 */

                showAllTrades = false;
                ((JFrame) window).dispose();
            }

            firstTradesWindowOpened = true;

        } else if (eventID == WindowEvent.WINDOW_CLOSING) {
            Utils.logToConsole("User closing trades log");
        } else if (eventID == WindowEvent.WINDOW_CLOSED) {
            if (showAllTrades) {
                Utils.logToConsole("Trades log closed by user - recreating");
                Utils.showTradesLogWindow();
            }
        }

    }

    /**
     * Identifies the Trades window by looking for "Trades" in the window title.
     *
     * @param window the window to identify
     * @return true if this is the Trades window; false otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JFrame)) {
            return false;
        }

        return SwingUtils.titleContains(window, "Trades");
    }

    /**
     * Monitors the "All" trades checkbox to keep it selected.
     *
     * Re-selects it if the user attempts to deselect while ShowAllTrades is
     * active.
     *
     * @param window the Trades frame
     * @param text the label text of the checkbox to monitor
     */
    private void monitorAllTradesCheckbox(
            final Window window, final String text) {
        final JCheckBox check = SwingUtils.findCheckBox(window, text);
        if (check != null) {
            check.addChangeListener(new ChangeListener() {
                /**
                 * Handles state changes to the monitored checkbox by
                 * re-selecting it if deselected.
                 *
                 * @param ce the change event
                 */
                @Override
                public void stateChanged(final ChangeEvent ce) {
                    Utils.logToConsole(
                            "Checkbox: " + check.getText()
                            + "; selected=" + check.isSelected());
                    if (!check.isSelected()) {
                        GuiDeferredExecutor.instance().execute(() -> {
                            Utils.logToConsole(
                                    "Checkbox: " + check.getText()
                                    + "; setting selected");
                            if (!check.isSelected()) {
                                check.doClick();
                            }
                        });
                    }
                }
            });
        }
    }
}
