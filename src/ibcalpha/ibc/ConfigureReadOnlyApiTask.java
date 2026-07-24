// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2019 Richard L King (rlking@aultan.com)
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

import javax.swing.JCheckBox;
import javax.swing.JDialog;

public class ConfigureReadOnlyApiTask implements ConfigurationAction {

    private final boolean readOnlyApi;
    private JDialog configDialog;

    ConfigureReadOnlyApiTask(final boolean value) {
        this.readOnlyApi = value;
    }

    /**
     * Configures the Read-Only API setting in the global configuration dialog.
     *
     * Opens the API settings section and sets the Read-Only API checkbox to the
     * specified value. If API is already enabled, flags that a confirmation
     * dialog is expected. On older TWS versions that lack this setting, logs an
     * error but does not fail.
     */
    @Override
    public void run() {
        try {
            Utils.logToConsole("Setting ReadOnlyApi");

            Utils.selectApiSettings(configDialog);

            JCheckBox readOnlyApiCheckbox =
                SwingUtils.findCheckBox(configDialog, "Read-Only API");
            if (readOnlyApiCheckbox == null) {
                // NB: we don't throw here because older TWS versions did not
                // have this setting
                Utils.logError("could not find Read-Only API checkbox");
                return;
            }

            if (readOnlyApiCheckbox.isSelected() == readOnlyApi) {
                Utils.logToConsole(
                    "Read-Only API checkbox is already set to: " + readOnlyApi);
            } else {
                if (!SessionManager.isGateway()) {
                    JCheckBox cb = SwingUtils.findCheckBox(
                        configDialog, "Enable ActiveX and Socket Clients");
                    if (cb == null) {
                        throw new IbcException(
                            "could not find Enable ActiveX checkbox");
                    }
                    if (cb.isSelected()) {
                        ConfigDialogManager.configDialogManager()
                            .setApiConfigChangeConfirmationExpected();
                    }
                }
                readOnlyApiCheckbox.setSelected(readOnlyApi);
                Utils.logToConsole(
                    "Read-Only API checkbox is now set to: " + readOnlyApi);
            }
        } catch (IbcException e) {
            Utils.logException(e);
        }
    }

    /**
     * Initializes this task with the global configuration dialog.
     *
     * @param value the TWS Global Configuration dialog
     */
    @Override
    public void initialise(final JDialog value) {
        this.configDialog = value;
    }
}
