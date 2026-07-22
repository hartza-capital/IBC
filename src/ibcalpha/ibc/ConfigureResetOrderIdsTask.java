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

import javax.swing.JDialog;

/**
 * Resets the API order ID sequence in TWS Global Configuration.
 */
public class ConfigureResetOrderIdsTask implements ConfigurationAction {

    /** Whether to reset the API order IDs. */
    private final boolean resetOrderIds;

    /** The TWS Global Configuration dialog used to perform the reset. */
    private JDialog mConfigDialog;

    /**
     * Creates a new task that resets API order IDs when enabled.
     *
     * @param shouldResetOrderIds true to reset order IDs; false otherwise
     */
    ConfigureResetOrderIdsTask(final boolean shouldResetOrderIds) {
        this.resetOrderIds = shouldResetOrderIds;
    }

    /**
     * Initializes this task with the global configuration dialog.
     *
     * @param configDialog the TWS Global Configuration dialog
     */
    @Override
    public void initialise(final JDialog configDialog) {
        this.mConfigDialog = configDialog;
    }

    /**
     * Resets the API order ID sequence in the TWS configuration.
     *
     * If resetOrderIds is true, opens the API settings section and clicks the
     * "Reset API order ID sequence" button. On TWS (not Gateway), flags that a
     * confirmation dialog is expected. If resetOrderIds is false, does nothing.
     */
    @Override
    public void run() {
        try {
            if (!resetOrderIds) {
                return;
            }

            Utils.logToConsole("Resetting API order ids");

            if (!SessionManager.isGateway()) {
                // NB: Gateway never displays the confirmation dialog
                ResetOrderIdConfirmationDialogHandler
                        .setOrderIdResetRequestedAtStart(true);
            }

            Utils.selectApiSettings(mConfigDialog);

            if (!SwingUtils.clickButton(
                    mConfigDialog, "Reset API order ID sequence")) {
                throw new IbcException(
                        "could not find 'Reset API order ID sequence' button");
            }
        } catch (IbcException e) {
            Utils.logException(e);
        }
    }
}
