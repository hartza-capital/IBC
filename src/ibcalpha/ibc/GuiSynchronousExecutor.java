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
 * Blocking executor that runs tasks on the Swing event dispatch thread.
 */
final class GuiSynchronousExecutor implements Executor {
    /** Singleton instance. */
    private static final GuiSynchronousExecutor INSTANCE =
            new GuiSynchronousExecutor();

    /**
     * Private constructor for singleton pattern.
     */
    private GuiSynchronousExecutor() { }

    /**
     * Returns the singleton instance.
     *
     * @return the shared GuiSynchronousExecutor instance
     */
    static GuiSynchronousExecutor instance() {
        return INSTANCE;
    }

    /**
     * Executes the task on the Swing event dispatch thread, blocking the
     * caller until completion.
     *
     * @param task the runnable task to execute
     */
    @Override
    public void execute(final Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Exception ex) {
                // ignore interruptions while waiting for the EDT
            }
        }
    }
}
