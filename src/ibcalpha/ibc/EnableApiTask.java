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

import javax.swing.JCheckBox;
import javax.swing.JDialog;

class EnableApiTask implements ConfigurationAction {

    /** Command channel used to send acknowledgments to the client. */
    private final CommandChannel commandChannel;

    /** The TWS Global Configuration dialog passed to this task. */
    private JDialog configDialog;

    EnableApiTask(final CommandChannel channel) {
        commandChannel = channel;
    }

    /**
     * Enables API connections in the Global Configuration dialog.
     *
     * <p>Navigates to the API settings, finds the "Enable ActiveX and Socket
     * Clients" checkbox, and ensures it is selected. Closes the config dialog
     * and sends appropriate acknowledgment to the client.</p>
     *
     * @throws IbcException (caught internally) if required controls cannot be
     *     found
     */
    @Override
    public void run() {
        try {
            Utils.logToConsole("Doing ENABLEAPI configuration");

            Utils.selectApiSettings(configDialog);

            final JCheckBox enableApiCheckbox = SwingUtils.findCheckBox(
                    configDialog, "Enable ActiveX and Socket Clients");
            if (enableApiCheckbox == null) {
                throw new IbcException(
                        "could not find Enable ActiveX checkbox");
            }

            if (!enableApiCheckbox.isSelected()) {
                enableApiCheckbox.doClick();
                SwingUtils.clickButton(configDialog, "OK");
                Utils.logToConsole(
                        "TWS has been configured to accept API connections");
                commandChannel.writeAck("configured");
            } else {
                Utils.logToConsole(
                        "TWS is already configured to accept API connections");
                commandChannel.writeAck("already configured");
            }
        } catch (IbcException e) {
            Utils.logError("CommandServer: " + e.getMessage());
            commandChannel.writeNack(e.getMessage());
        }
    }

    /**
     * Initializes this task with the global configuration dialog.
     *
     * @param dialog the TWS Global Configuration dialog
     */
    @Override
    public void initialise(final JDialog dialog) {
        configDialog = dialog;
    }

}
