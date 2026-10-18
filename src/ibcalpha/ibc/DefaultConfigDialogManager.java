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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * Default {@link ConfigDialogManager} implementation that locates the Global Configuration
 * dialog through the TWS or Gateway menu and caches it while it remains open.
 */
public class DefaultConfigDialogManager extends ConfigDialogManager {

    /** Cached Global Configuration dialog, null while none is open. */
    private volatile JDialog configDialog = null;
    /** Task opening the config dialog, null when no open is pending. */
    private volatile GetConfigDialogTask configDialogTask;

    /** Guards creation and assignment of the config dialog future. */
    private final Object futureCreationLock = new Object();
    /** Pending lookup that yields the config dialog, null when none is running. */
    private Future<JDialog> configDialogFuture;

    /** Number of open handles on the config dialog, possibly including the user. */
    private int usageCount;

    /**
     * Logs a diagnostic message indicating that the default config dialog manager is in use.
     */
    @Override
    public void logDiagnosticMessage(){
        Utils.logToConsole("using default config dialog manager");
    }

    /**
     * Records the fact that the config dialog has closed and clears the cached reference.
     */
    @Override
    public void clearConfigDialog() {
        openedByUser = false;
        configDialog = null;
    }

    /**
     * Returns the Global Configuration dialog, blocking until it is available or
     * the timeout expires, and immediately if it is already open.
     *
     * @param timeout how long to wait for the dialog; negative blocks indefinitely
     * @param unit the time unit of timeout
     * @return the Global Configuration dialog, or null if the timeout expires first
     * @throws IllegalStateException if called from the Swing event dispatch thread
     */
    @Override
    public JDialog getConfigDialog(long timeout, TimeUnit unit) throws IllegalStateException {
        /* Note that caching a config dialog doesn't work, since they seem to
         * be one-time-only. So we have to go via the menu each time this 
         * method is called (if it isn't currently open or being opened).
        */

        if (SwingUtilities.isEventDispatchThread()) throw new IllegalStateException();

        Utils.logToConsole("Getting config dialog");

        incrementUsage();

        if (configDialog != null) {
            Utils.logToConsole("Config dialog already found");
            return configDialog;
        }

        synchronized(futureCreationLock) {
            if (configDialogFuture != null) {
                    Utils.logToConsole("Waiting for config dialog future to complete");
            } else {
                Utils.logToConsole("Creating config dialog future");
                configDialogTask = new GetConfigDialogTask();
                ExecutorService exec = Executors.newSingleThreadExecutor();
                configDialogFuture = exec.submit((Callable<JDialog>)configDialogTask);
                exec.shutdown();
            }
        }

        try {
            if (timeout < 0) {
                configDialog = configDialogFuture.get();
            } else {
                configDialog = configDialogFuture.get(timeout, unit);
            }
            Utils.logToConsole("Got config dialog from future");
            return configDialog;
        } catch (TimeoutException | InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof IbcException) {
                Utils.logError("getConfigDialog could not find " + t.getMessage());
                return null;
            }
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            if (t instanceof Error) throw (Error)t;
            throw new IllegalStateException(t);
        }
    }

    /**
     * Returns the Global Configuration dialog, blocking the calling thread until
     * it is available.
     *
     * <p>Returns immediately if the dialog is already open.</p>
     *
     * @return the Global Configuration dialog, or null if it cannot be found
     * @throws IllegalStateException if called from the Swing event dispatch thread
     */
    @Override
    public JDialog getConfigDialog() throws IllegalStateException {
        return getConfigDialog(-1, TimeUnit.MILLISECONDS);
    }

    /** True when the user opened the config dialog rather than an automated task. */
    private boolean openedByUser;
    /**
     * Sets the Global Configuration dialog and records whether it was opened by an automated task or the user.
     *
     * @param window the Global Configuration dialog
     */
    @Override
    public void setConfigDialog(JDialog window) {
        configDialog = window;
        if (configDialogTask == null) {
            // config dialog opened by user
            openedByUser = true;
        } else {
            configDialogTask.setConfigDialog(window);
            configDialogTask = null;
            configDialogFuture = null;
        }
    }

    /** True when an API configuration change confirmation dialog is expected. */
    private boolean apiConfigChangeConfirmationExpected;
    /**
     * Indicates whether an API configuration change confirmation dialog is expected to appear.
     *
     * @return true if a confirmation dialog is expected; false otherwise
     */
    @Override
    public boolean getApiConfigChangeConfirmationExpected() {
        return apiConfigChangeConfirmationExpected;
    }

    /**
     * Releases a reference to the Global Configuration dialog, potentially closing it if no other references remain.
     */
    @Override
    public void releaseConfigDialog() {
        decrementUsage();
    }

    /**
     * Records that an API configuration change confirmation dialog is expected to appear.
     */
    @Override
    public void setApiConfigChangeConfirmationExpected() {
        apiConfigChangeConfirmationExpected = true;
    }

    /**
     * Records that the expected API configuration change confirmation dialog has been handled.
     */
    @Override
    public void setApiConfigChangeConfirmationHandled() {
        apiConfigChangeConfirmationExpected = false;
    }

    /**
     * Increments the usage counter for the Global Configuration dialog.
     */
    private synchronized void incrementUsage() {
        usageCount++;
    }

    /**
     * Decrements the usage counter for the Global Configuration dialog and closes it if the count reaches zero.
     */
    private synchronized void decrementUsage() {
        usageCount--;
        if (openedByUser) return;
        if (usageCount == 0){
            GuiDeferredExecutor.instance().execute(() -> {
                Utils.logToConsole("Configuration tasks completed");
                SwingUtils.clickButton(configDialog, "OK");
                GuiDeferredExecutor.instance().execute(() -> MainWindowManager.mainWindowManager().iconizeIfRequired());
            });
        }
    }



}
