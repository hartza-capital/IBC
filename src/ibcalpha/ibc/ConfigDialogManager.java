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

import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;

public abstract class ConfigDialogManager
{
    private static ConfigDialogManager _ConfigDialogManager;

    static {
        _ConfigDialogManager = new DefaultConfigDialogManager();
    }

    /**
     * Sets the global ConfigDialogManager instance to the specified implementation.
     *
     * @param configDialogManager the ConfigDialogManager instance to use; must not be null
     * @throws IllegalArgumentException if configDialogManager is null
     */
    public static void initialise(ConfigDialogManager configDialogManager){
        if (configDialogManager == null) throw new IllegalArgumentException("configDialogManager");
        _ConfigDialogManager = configDialogManager;
    }

    /**
     * Resets the global ConfigDialogManager instance to a new DefaultConfigDialogManager.
     */
    public static void setDefault() {
        _ConfigDialogManager = new DefaultConfigDialogManager();
    }

    /**
     * Returns the current global ConfigDialogManager instance.
     *
     * @return the active ConfigDialogManager instance
     */
    public static ConfigDialogManager configDialogManager() {
        return _ConfigDialogManager;
    }

    /**
     * Logs diagnostic information about the current state of the Global
     * Configuration dialog.
     */
    public abstract void logDiagnosticMessage();

    /**
     * Records the fact that the config dialog has closed.
     */
    public abstract void clearConfigDialog();

    /**
     * Determines whether an API configuration change confirmation dialog is
     * expected.
     *
     * @return true if a confirmation dialog is expected, false otherwise
     */
    public abstract boolean getApiConfigChangeConfirmationExpected();

    /**
     * Returns the Global Configuration dialog, if necessary blocking the calling thread until
     * either it is available or a specified timeout has elapsed.
     *
     * If the Global Configuration dialog is currently open, it is returned immediately without blocking
     * the calling thread.
     *
     * Calling this method from the Swing event dispatch thread results in an IllegalStateException
     * being thrown.
     *
     * @param timeout
     * the length of time to wait for the Global Configuration dialog to become available. If this is
     * negative, the calling thread blocks until the dialog becomes available.
     * @param unit
     * the time units for the timeout parameter
     * @return
     * null if the timeout expires before the Global Configuration dialog becomes available; otherwise
     * the Global Configuration dialog
     * @throws IllegalStateException
     * the method has been called from the Swing event dispatch thread
     */
    public abstract JDialog getConfigDialog(long timeout, TimeUnit unit) throws IllegalStateException;

    /**
     * Returns the Global Configuration dialog, if necessary blocking the calling thread until
     * it is available.
     *
     * If the Global Configuration dialog is currently open, it is returned immediately without blocking
     * the calling thread.
     *
     * Calling this method from the Swing event dispatch thread results in an IllegalStateException
     * being thrown.
     *
     * @return
     * the Global Configuration dialog, or null if the relevant menu entries cannot be found
     * @throws IllegalStateException
     * the method has been called from the Swing event dispatch thread
     */
    public abstract JDialog getConfigDialog() throws IllegalStateException;

    /**
     * Marks the Global Configuration dialog as no longer in use after
     * configuration tasks complete.
     */
    public abstract void releaseConfigDialog();

    /**
     * Sets the flag indicating that an API configuration change confirmation
     * dialog is expected.
     */
    public abstract void setApiConfigChangeConfirmationExpected();

    /**
     * Clears the flag indicating that an API configuration change
     * confirmation is expected.
     */
    public abstract void setApiConfigChangeConfirmationHandled();

    /**
     * Records the specified Global Configuration dialog for later retrieval.
     *
     * @param window the Global Configuration dialog to store
     */
    public abstract void setConfigDialog(JDialog window);

}
