// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2023 Richard L King (rlking@aultan.com)
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
 * Handles the confirmation dialog shown when TWS requests to reset the API
 * order ID sequence.
 */
public final class ResetOrderIdConfirmationDialogHandler
        implements WindowHandler {

    /**
     * Indicates whether the order ID reset was requested automatically at
     * startup (as opposed to being triggered by a user action).
     */
    private static volatile boolean orderIdResetRequestedAtStart;

    /**
     * Sets whether the order ID reset was requested at startup.
     *
     * @param requested true if the reset was requested at startup
     */
    public static void setOrderIdResetRequestedAtStart(
            final boolean requested) {
        orderIdResetRequestedAtStart = requested;
    }

    /**
     * Determines whether to process window events for the reset order ID
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
     * Handles the reset order ID confirmation dialog based on the
     * ConfirmOrderIdReset setting.
     *
     * <p>Behavior depends on whether the reset was initiated at startup or by
     * user action.
     *
     * @param window the dialog window
     * @param eventID the AWT window event ID
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        final String confirmReset = "confirm";
        final String rejectReset = "reject";
        final String ignoreDialog = "ignore";
        final String invalidSetting = "invalid";

        String confirmSetting =
                Settings.settings().getString("ConfirmOrderIdReset", "");

        String[] confirmSettings = confirmSetting.split("/");
        String confirmAtStart = confirmSettings[0];
        String confirmUserInitiated = confirmSettings[1];

        String action;
        if (orderIdResetRequestedAtStart) {
            Utils.logToConsole("Order id reset requested at startup");
            switch (confirmAtStart) {
                case confirmReset:
                case rejectReset:
                case ignoreDialog:
                    action = confirmAtStart;
                    break;
                default:
                    action = invalidSetting;
            }
            orderIdResetRequestedAtStart = false;
        } else {
            Utils.logToConsole("Order id reset requested by user");
            switch (confirmUserInitiated) {
                case confirmReset:
                case rejectReset:
                case ignoreDialog:
                    action = confirmUserInitiated;
                    break;
                default:
                    action = invalidSetting;
            }
        }

        try {
            switch (action) {
                case invalidSetting:
                    throw new IbcException(
                            "Invalid setting ConfirmOrderIdReset="
                                    + confirmSetting);
                case confirmReset:
                    Utils.logToConsole("Accepting order id reset");
                    if (!SwingUtils.clickButton(window, "Yes")) {
                        throw new IbcException("Can't find 'Yes' button");
                    }
                    break;
                case rejectReset:
                    Utils.logToConsole("Rejecting order id reset");
                    if (!SwingUtils.clickButton(window, "No")) {
                        throw new IbcException("Can't find 'Yes' button");
                    }
                    break;
                case ignoreDialog:
                    Utils.logToConsole(
                        "User must handle order id reset confirmation dialog");
                    break;
                default:
                    // No other actions are possible because invalidSetting is
                    // already handled above.
                    break;
            }
        } catch (IbcException e) {
            Utils.logException(e);
        }
    }

    /**
     * Identifies the reset order ID confirmation dialog by checking for a
     * specific label.
     *
     * @param window the window to identify
     * @return true if this is the reset order ID confirmation dialog, false
     *         otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return (SwingUtils.findLabel(window,
                "Are you sure you want to reset API order ID sequence?")
                != null);
    }

}
