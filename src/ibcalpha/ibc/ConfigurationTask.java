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

import java.util.concurrent.FutureTask;
import javax.swing.JDialog;

public class ConfigurationTask {

    private final ConfigurationAction configAction;

    /**
     * Constructs a ConfigurationTask that wraps a ConfigurationAction.
     *
     * @param configAction the configuration action to execute
     */
    public ConfigurationTask(ConfigurationAction configAction) {
        this.configAction = configAction;
    }

    /**
     * Executes the configuration action asynchronously on a background thread.
     *
     * This method obtains the global configuration dialog, initializes the action with it,
     * executes the action on the Swing EDT, and releases the dialog when complete.
     */
    public void executeAsync() {
        MyCachedThreadPool.getInstance().execute(new ConfigTaskRunner());
    }

    /**
     * Executes the configuration action synchronously on the calling thread.
     *
     * This method runs the same workflow as executeAsync but blocks until completion.
     */
    public void execute() {
        (new ConfigTaskRunner()).run();
    }

    private class ConfigTaskRunner implements Runnable {
        /**
         * Executes the configuration action workflow.
         *
         * Retrieves the Global Configuration dialog, initializes the configuration action with it,
         * executes the action on the Swing EDT, and releases the dialog on completion or error.
         */
        @Override
        public void run() {
            try {
                final JDialog configDialog = ConfigDialogManager.configDialogManager().getConfigDialog();    // blocks the thread until the config dialog is available
                configAction.initialise(configDialog);
   
                FutureTask<?> t = new FutureTask<>((Runnable)configAction, null);
                GuiExecutor.instance().execute(t);
                t.get();

                ConfigDialogManager.configDialogManager().releaseConfigDialog();
            } catch (Exception e){
                Utils.logException(e);
            }
        }
    }


}
