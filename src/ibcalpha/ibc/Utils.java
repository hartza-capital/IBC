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

import java.awt.Container;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

final class Utils {

    private Utils() {
    }

    static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    // set these to the defaults, so that we can continue to use them
    // even when TWS redirects System.out and System.err to its own logfile
    private static final PrintStream OUT = System.out;
    private static final PrintStream ERR = System.err;

    /**
     * Banner string printed around error and exception messages.
     */
    private static final String ERROR_BANNER = "!".repeat(79);

    /**
     * Pause time in milliseconds before retrying menu item click.
     */
    private static final int PAUSE_RETRY_MILLIS = 250;

    /**
     * Performs a click on the menu item at the specified path, waiting if
     * necessary for the menu item to become enabled.
     *
     * If there is more than one menu bar within the specified container, they
     * are searched (in hierarchical containment order) until one is found that
     * contains the specified menu item.
     *
     * Note that this method may block the calling thread if the required menu
     * item is currently disabled.
     * @param container
     * the Container to search in
     * @param path
     * the path of the required menu item
     * @return
     * true if the menu item was successfully clicked; false if the menu item
     * could not be found
     * @throws  IllegalStateException
     * the method has been called on the Swing event dispatch thread
     */
    static boolean invokeMenuItem(final Container container,
            final String[] path) throws IllegalStateException {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "Function must not be called on the event dispatch thread, "
                + "as it may block the thread");
        }
        while (true) {
            FutureTask<Boolean> task = new FutureTask<>(() -> {
                String s = path[0];
                for (int i = 1; i < path.length; i++) {
                    s = s + " > " + path[i];
                }

                JMenuItem menuItem =
                    SwingUtils.findMenuItemInAnyMenuBar(container, path);
                if (menuItem == null) {
                    throw new IbcException("menu item: " + s);
                }
                if (!menuItem.isEnabled()) {
                    return false;
                }
                menuItem.doClick();
                return true;
            });

            GuiDeferredExecutor.instance().execute(task);

            try {
                if (task.get()) {
                    return true;
                }
            } catch (InterruptedException e) {
                logError("invokeMenuItem task interrupted");
                return false;
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                if (t instanceof IbcException) {
                    return false;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
            }

            pause(PAUSE_RETRY_MILLIS);
        }
    }

    /**
     * Logs an error message and terminates the JVM with the given error code.
     *
     * @param errorCode the exit code to report
     */
    static void exitWithError(final int errorCode) {
        logToConsole("Exiting after error with exit code=" + errorCode);
        Runtime.getRuntime().halt(errorCode);
    }

    /**
     * Logs the supplied message and terminates the JVM with the given error
     * code.
     *
     * @param errorCode the exit code to report
     * @param message the error message to log
     */
    static void exitWithError(final int errorCode, final String message) {
        logError(message);
        exitWithError(errorCode);
    }

    /**
     * Logs the supplied exception and terminates the JVM with the given code.
     *
     * @param errorCode the exit code to report
     * @param t the exception to log
     */
    static void exitWithException(final int errorCode, final Throwable t) {
        logException(t);
        exitWithError(errorCode);
    }

    static void exitWithoutError() {
        System.exit(0);
    }

    /**
     * Logs an error message surrounded by banner lines.
     *
     * @param message the error message to log
     */
    static void logError(final String message) {
        getOutStream().println(ERROR_BANNER);
        getOutStream().println(formatMessage(message));
        getOutStream().println(ERROR_BANNER);
    }

    /**
     * Logs an exception surrounded by banner lines.
     *
     * @param t the exception to log
     */
    static void logException(final Throwable t) {
        getOutStream().println(ERROR_BANNER);
        getOutStream().println(formatMessage("An exception has occurred:"));
        if (Settings.settings().getBoolean(
                "IncludeStackTraceForExceptions", false)) {
            t.printStackTrace(getOutStream());
        } else {
            getOutStream().println(t.getMessage() + "");
        }
        getOutStream().println(ERROR_BANNER);
    }

    /**
     * Writes a plain one-line text message to the console.
     * @param msg
     * The message to be written
     */
    static void logRawToConsole(final String msg) {
        getOutStream().println(msg);
    }

    /**
     * Writes a text message prefixed with the current time to the console.
     * @param msg
     * The message to be written
     */
    static void logToConsole(final String msg) {
        getOutStream().println(formatMessage(msg));
    }

    static PrintStream getErrStream() {
        return ERR;
    }

    static PrintStream getOutStream() {
        return OUT;
    }

    static String formatDate(final LocalDateTime date) {
        return DATE_FORMATTER.format(date);
    }

    /**
     * Formats a message by prepending the current timestamp and "IBC:" prefix.
     *
     * @param message the message to format
     * @return the formatted message with timestamp and prefix
     */
    private static String formatMessage(final String message) {
        return DATE_FORMATTER.format(LocalDateTime.now()) + " IBC: " + message;
    }

    /**
     * sleeps for millis milliseconds, approximately.
     *
     * Note that this method swallows the InterruptedException that may
     * result from a call to sleep().
     * @param millis
     * the number of milliseconds to sleep for
     */
    static void pause(final int millis) {
        try {
            Thread.sleep(millis); // sleep a bit before trying again.
        } catch (InterruptedException ie) {
        }
    }

    /**
     * Selects the specified section in the Global Configuration dialog.
     * @param configDialog
     * the Global Configuration dialog
     * @param path
     * the path to the required configuration section in the Global
     * Configuration dialog
     * @return
     * true if the specified section can be found; otherwise false
     * @throws IbcException
     * a UI component could not be found
     * @throws IllegalStateException
     * the method has not been called on the SWing event dispatch thread
     */
    static boolean selectConfigSection(final JDialog configDialog,
            final String[] path) throws IbcException, IllegalStateException {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                "selectConfigSection must be run on the event dispatch thread");
        }

        JTree configTree = SwingUtils.findTree(configDialog);
        if (configTree == null) {
            throw new IbcException(
                "could not find the config tree in the Global "
                + "Configuration dialog");
        }

        Object node = configTree.getModel().getRoot();
        TreePath tp = new TreePath(node);

        for (String pathElement: path) {
            node = SwingUtils.findChildNode(configTree.getModel(), node,
                    pathElement);
            if (node == null) {
                return false;
            }
            tp = tp.pathByAddingChild(node);
        }

        configTree.setExpandsSelectedPaths(true);
        configTree.setSelectionPath(tp);
        return true;
    }

    static void selectApiSettings(final JDialog configDialog)
            throws IbcException, IllegalStateException {
        if (!Utils.selectConfigSection(configDialog,
                new String[] {"API", "Settings"})) {
            // older versions of TWS don't have the Settings node below
            // the API node
            Utils.selectConfigSection(configDialog, new String[] {"API"});
        }
    }

    static void showTradesLogWindow() {
        MyCachedThreadPool.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                invokeMenuItem(
                        MainWindowManager.mainWindowManager().getMainWindow(),
                        new String[] {"Account", "Trade Log"});
            }
        });
    }

    static String stringToHex(final String value) {
        char[] chars = value.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            sb.append(Integer.toHexString(chars[i]));
        }
        return sb.toString();
    }

}

