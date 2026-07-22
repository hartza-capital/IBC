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

import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;

/**
 * Handles the cryptocurrency order confirmation dialog.
 *
 * Automatically transmits, cancels, or leaves the dialog untouched based on the
 * ConfirmCryptoCurrencyOrders setting.
 */
public class CryptoOrderConfirmationDialogHandler implements WindowHandler {

    /**
     * Determines whether to process window events for the cryptocurrency order
     * confirmation dialog.
     *
     * @param window the window to check
     * @param eventId the AWT window event ID
     * @return true if this is a WINDOW_OPENED event, false otherwise
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
     * Handles the cryptocurrency order confirmation dialog based on the
     * ConfirmCryptoCurrencyOrders setting.
     *
     * Can transmit, cancel, or leave the dialog for manual handling.
     *
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        String setting = Settings.settings().getString(
                "ConfirmCryptoCurrencyOrders", "manual");

        switch (setting) {
            case "manual":
                break;
            case "transmit":
                if (!SwingUtils.clickButton(window, "Transmit")) {
                    Utils.logError(
                            "could not confirm cryptocurrency order because "
                            + "we could not find the Transmit button");
                }
                break;
            case "cancel":
                if (!SwingUtils.clickButton(window, "Cancel")) {
                    Utils.logError(
                            "could not cancel cryptocurrency order because "
                            + "we could not find the Cancel button");
                }
                break;
            default:
                Utils.logError(
                        "ConfirmCryptoCurrencyOrders setting is "
                        + "invalid");
        }
    }

    /**
     * Identifies the cryptocurrency order confirmation dialog by checking the
     * window title.
     *
     * @param window the window to identify
     * @return true if this is the cryptocurrency order confirmation dialog,
     *         false otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return SwingUtils.titleContains(
                window, "Cryptocurrency order confirmation");
    }

}
