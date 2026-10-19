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

/**
 * Configuration action that enables socket API connections in the Global Configuration dialog.
 */
class EnableApiTask implements ConfigurationAction{

    /** Channel that receives the acknowledgement or error reply for this action. */
    private final CommandChannel mChannel;

    /** Global Configuration dialog this action operates on. */
    private JDialog configDialog;

    /**
     * Creates a task that enables API connections and reports the outcome on the supplied
     * command channel.
     *
     * @param channel the command channel to which the acknowledgement or error reply is written
     */
    EnableApiTask(final CommandChannel channel) {
        mChannel = channel;
    }

    /**
     * Enables socket API connections in the Global Configuration dialog and reports the outcome
     * on the command channel.
     */
    @Override public void run() {
        try {
            Utils.logToConsole("Doing ENABLEAPI configuration");

            Utils.selectApiSettings(configDialog);

            JCheckBox cb = SwingUtils.findCheckBox(configDialog, "Enable ActiveX and Socket Clients");
            if (cb == null) throw new IbcException("could not find Enable ActiveX checkbox");

            if (!cb.isSelected()) {
                cb.doClick();
                SwingUtils.clickButton(configDialog, "OK");
                Utils.logToConsole("TWS has been configured to accept API connections");
                mChannel.writeAck("configured");
            } else {
                Utils.logToConsole("TWS is already configured to accept API connections");
                mChannel.writeAck("already configured");
            }
        } catch (IbcException e) {
            Utils.logError("CommandServer: " + e.getMessage());
            mChannel.writeNack(e.getMessage());
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
