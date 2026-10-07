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

import java.util.concurrent.Executor;
import javax.swing.SwingUtilities;

/**
 * Executor singleton that runs tasks on the Swing event dispatch thread, executing them inline when
 * the caller is already on that thread.
 */
class GuiExecutor implements Executor {
    private static final GuiExecutor instance = new GuiExecutor();

    /**
     * Private constructor for singleton pattern.
     */
    private GuiExecutor() {}

    /**
     * Returns the single shared instance of this executor.
     *
     * @return the executor singleton
     */
    static GuiExecutor instance() {return instance;}

    /**
     * Executes the task on the Swing event dispatch thread, running immediately if already on the EDT or scheduling for later otherwise.
     *
     * @param task the runnable task to execute
     */
    public void execute(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
