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

    /**
     * The configuration action to execute.
     */
    private final ConfigurationAction configAction;

    /**
     * Constructs a ConfigurationTask that wraps a ConfigurationAction.
     *
     * @param configurationAction the configuration action to execute
     */
    public ConfigurationTask(final ConfigurationAction configurationAction) {
        this.configAction = configurationAction;
    }

    /**
     * Executes the configuration action asynchronously on a background thread.
     *
     * <p>This method obtains the global configuration dialog, initializes the
     * action with it, executes the action on the Swing EDT, and releases the
     * dialog when complete.
     */
    public void executeAsync() {
        MyCachedThreadPool.getInstance().execute(new ConfigTaskRunner());
    }

    /**
     * Executes the configuration action synchronously on the calling thread.
     *
     * <p>This method runs the same workflow as executeAsync but blocks until
     * completion.
     */
    public void execute() {
        (new ConfigTaskRunner()).run();
    }

    private class ConfigTaskRunner implements Runnable {
        /**
         * Executes the configuration action workflow.
         *
         * <p>Retrieves the Global Configuration dialog, initializes the
         * configuration action with it, executes the action on the Swing EDT,
         * and releases the dialog on completion or error.
         */
        @Override
        public void run() {
            try {
                final JDialog configDialog =
                        ConfigDialogManager.configDialogManager()
                                .getConfigDialog();
                configAction.initialise(configDialog);

                FutureTask<?> futureTask =
                        new FutureTask<>((Runnable) configAction, null);
                GuiExecutor.instance().execute(futureTask);
                futureTask.get();

                ConfigDialogManager.configDialogManager().releaseConfigDialog();
            } catch (Exception e) {
                Utils.logException(e);
            }
        }
    }

}
