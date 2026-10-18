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
import javax.swing.JFrame;

/**
 * Abstract accessor for the TWS/Gateway main window, used by IBC's handlers and
 * configuration tasks.
 */
public abstract class MainWindowManager {

    /** The active MainWindowManager implementation. */
    private static MainWindowManager _mainWindowManager;

    static {
        _mainWindowManager = new DefaultMainWindowManager();
    }

    /**
     * Sets the global MainWindowManager instance to the specified implementation.
     *
     * @param mainWindowManager the MainWindowManager instance to use; must not be null
     * @throws IllegalArgumentException if mainWindowManager is null
     */
    public static void initialise(MainWindowManager mainWindowManager){
        if (mainWindowManager == null) throw new IllegalArgumentException("mainWindowManager");
        _mainWindowManager = mainWindowManager;
    }

    /**
     * Resets the global MainWindowManager instance to a new DefaultMainWindowManager.
     */
    public static void setDefault() {
        _mainWindowManager = new DefaultMainWindowManager();
    }

    /**
     * Returns the current global MainWindowManager instance.
     *
     * @return the active MainWindowManager instance
     */
    public static MainWindowManager mainWindowManager() {
        return _mainWindowManager;
    }

    /**
     * Logs diagnostic information about the main window manager.
     */
    public abstract void logDiagnosticMessage();


    /**
     * Returns the main window, blocking the calling thread until it is available or the
     * timeout elapses. Returns immediately if the main window is already open.
     *
     * @param timeout how long to wait; a negative value blocks until the window becomes available
     * @param unit the time unit of timeout
     * @return the main window, or null if the timeout expires first
     * @throws IllegalStateException if called from the Swing event dispatch thread
     */
    public abstract JFrame getMainWindow(long timeout, TimeUnit unit);

    /**
     * Returns the main window, blocking the calling thread until it is available. Returns
     * immediately if the main window is already open.
     *
     * @return the main window
     * @throws IllegalStateException if called from the Swing event dispatch thread
     */
    public abstract JFrame getMainWindow() throws IllegalStateException;

    /**
     * Sets the main window reference and completes initialization tasks.
     *
     * @param window the main application window
     */
    public abstract void setMainWindow(JFrame window);

    /**
     * Minimizes the main window if configured to do so.
     */
    public abstract void iconizeIfRequired();

}
