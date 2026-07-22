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

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JDialog;
import javax.swing.JFrame;

class GetConfigDialogTask implements Callable<JDialog> {

    /** The Global Configuration dialog once it has been located. */
    private volatile JDialog mConfigDialog;

    /** Lock used to wait for the configuration dialog to appear. */
    private final Lock lock = new ReentrantLock();

    /** Condition signalled when the configuration dialog is available. */
    private final Condition gotConfigDialog = lock.newCondition();

    /**
     * Retrieves the Global Configuration dialog from TWS or Gateway.
     *
     * Opens the configuration dialog via the appropriate menu item and waits for
     * it to appear. On Gateway, uses Configure > Settings; on TWS, tries Edit >
     * Global Configuration (Classic layout) or File > Global Configuration
     * (Mosaic layout). Blocks until the dialog is available.
     *
     * @return the Global Configuration dialog
     * @throws IbcException if the menu item cannot be invoked
     * @throws InterruptedException if interrupted while waiting for the dialog
     */
    @Override
    public JDialog call() throws IbcException, InterruptedException {
        final JFrame mainForm =
                MainWindowManager.mainWindowManager().getMainWindow();

        SessionManager.awaitReady();

        Utils.logToConsole("Invoking config dialog menu");

        if (SessionManager.isGateway()) {
            if (!Utils.invokeMenuItem(
                    mainForm,
                    new String[] {"Configure", "Settings"})) {
                throw new IbcException("'Configure > Settings' menu item");
            }
        } else {
            boolean invoked = Utils.invokeMenuItem(
                    mainForm,
                    new String[] {"Edit", "Global Configuration..."});

            if (!invoked) {
                invoked = Utils.invokeMenuItem(
                        mainForm,
                        new String[] {"File", "Global Configuration..."});
            }

            if (!invoked) {
                throw new IbcException(
                        "'Edit > Global Configuration' or "
                        + "'File > Global Configuration' menu items");
            }
        }

        lock.lock();
        try {
            while (mConfigDialog == null) {
                gotConfigDialog.await();
            }
        } finally {
            lock.unlock();
        }
        return mConfigDialog;
    }

    /**
     * Stores the located configuration dialog and signals waiting threads.
     *
     * @param configDialog the TWS Global Configuration dialog
     */
    void setConfigDialog(final JDialog configDialog) {
        lock.lock();
        try {
            mConfigDialog = configDialog;
            gotConfigDialog.signal();
        } finally {
            lock.unlock();
        }
    }
}
