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

import java.awt.Component;
import java.awt.Container;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTextField;

/**
 * Configuration action that sets the TWS Master API client ID in the TWS Global
 * Configuration dialog.
 */
class ConfigureTwsMasterClientIDTask implements ConfigurationAction{

    /** Master API client ID to configure. */
    private final String masterClientID;
    /** Global Configuration dialog the setting is applied to. */
    private JDialog configDialog;

    /**
     * Creates a task that sets the Master API client ID.
     *
     * @param masterClientID the master client ID to set
     */
    ConfigureTwsMasterClientIDTask(String masterClientID) {
        this.masterClientID = masterClientID;
    }

    /**
     * Configures the TWS Master API client ID in the global configuration dialog.
     *
     * Opens the API settings section, locates the Master API client ID text field, and updates it
     * to the specified value if different from the current setting.
     * If API is already enabled, flags that a confirmation dialog is expected.
     */
    @Override
    public void run() {
        try {
            Utils.logToConsole("Performing master client ID configuration");

            Utils.selectApiSettings(configDialog);

            Component comp = SwingUtils.findComponent(configDialog, "Master API client ID");
            if (comp == null) throw new IbcException("could not find Master Client ID component");

            JTextField tf = SwingUtils.findTextField((Container)comp, 0);
            if (tf == null) throw new IbcException("could not find Master Client ID field");

            String currentMasterClientID = tf.getText();
            if (currentMasterClientID.equals(masterClientID)) {
                Utils.logToConsole("TWS Master Client ID is already set to " + tf.getText());
            } else {
                if (!SessionManager.isGateway()) {
                    JCheckBox cb = SwingUtils.findCheckBox(configDialog, "Enable ActiveX and Socket Clients");
                    if (cb == null) throw new IbcException("could not find Enable ActiveX checkbox");
                    if (cb.isSelected()) ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationExpected();
                }
                Utils.logToConsole("TWS Master Client ID was set to " + tf.getText());
                tf.setText(masterClientID);
                Utils.logToConsole("TWS Master Client ID now set to " + tf.getText());
            }
        } catch (IbcException e) {
            Utils.logException(e);
        }
    }

    /**
     * Initializes this task with the global configuration dialog.
     *
     * @param configDialog the TWS Global Configuration dialog
     */
    @Override
    public void initialise(JDialog configDialog) {
        this.configDialog = configDialog;
    }
}
