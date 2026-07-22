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

import java.awt.AWTEvent;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Listens for AWT window events and dispatches them to registered handlers.
 */
class TwsListener
        implements AWTEventListener {

    /** Handlers consulted for each window event. */
    private final List<WindowHandler> windowHandlers;

    /** Current scope for window structure logging. */
    private static String logStructureScope;

    /** Current trigger for window structure logging. */
    private static String logStructureWhen;

    /**
     * Creates a new listener with the given window handlers.
     *
     * @param handlers the list of handlers to consult for window events
     */
    TwsListener(final List<WindowHandler> handlers) {
        this.windowHandlers = handlers;
        getLogStructureParameters();
    }

    /**
     * Handles AWTEvents dispatched by the Swing toolkit.
     * Filters for window events and iterates through registered WindowHandler
     * instances to find and execute handlers that recognize the window.
     * All handler execution is deferred to the GUI event dispatch thread.
     *
     * @param event the AWTEvent to process
     */
    @Override
    public void eventDispatched(final AWTEvent event) {
        try {
            final int eventID = event.getID();

            final Window window = ((WindowEvent) event).getWindow();

            GuiDeferredExecutor.instance().execute(() -> {
                try {
                    logWindow(window, eventID);

                    for (WindowHandler wh : windowHandlers) {
                        if (wh.recogniseWindow(window)) {
                            logWindowStructure(window, eventID, true);
                            if (wh.filterEvent(window, eventID)) {
                                wh.handleWindow(window, eventID);
                            }
                            return;
                        }
                    }

                    logWindowStructure(window, eventID, false);
                } catch (Throwable e) {
                    Utils.exitWithException(
                            ErrorCodes.UNHANDLED_EXCEPTION, e);
                }
            });
        } catch (Throwable e) {
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
        }
    }

    /**
     * Initializes logging parameters for window structure logging based on
     * configuration settings.
     * Handles both legacy LogComponents setting and newer LogStructureScope and
     * LogStructureWhen settings.
     */
    private static void getLogStructureParameters() {
        // legacy deprecated setting overrides explicit values of
        // LogStructureScope and LogStructureWhen
        final String logComponentsSetting = Settings.settings()
                .getString("LogComponents", "ignore")
                .toLowerCase();

        logStructureScope = getLogStructureScope(logComponentsSetting);
        logStructureWhen = getLogStructureWhen(logComponentsSetting);
    }

    /**
     * Determines the scope of windows for which component structure should be
     * logged.
     * Valid scopes are: "known" (recognized windows), "unknown" (unrecognized
     * windows), "untitled" (windows without a title), or "all" (all windows).
     *
     * @param logComponentsSetting the legacy LogComponents setting value
     * @return a validated scope string
     */
    private static String getLogStructureScope(
            final String logComponentsSetting) {
        if (logComponentsSetting.equals("ignore")) {
            logStructureScope = Settings.settings()
                    .getString("LogStructureScope", "known")
                    .toLowerCase();
        } else {
            logStructureScope = "all";
        }

        switch (logStructureScope) {
            case "known":
            case "unknown":
            case "untitled":
            case "all":
                break;
            default:
                Utils.logError(
                        "the LogStructureScope setting '"
                        + logStructureScope + "' is invalid.");
                logStructureScope = "known";
        }

        return logStructureScope;
    }

    /**
     * Determines when window component structure should be logged.
     * Valid values are: "activate" (when window is activated), "open" (when
     * window is opened), "openclose" (on both open and close), "never" (no
     * logging), or a specific window event name.
     *
     * @param logComponentsSetting the legacy LogComponents setting value
     * @return a validated "when" string
     */
    private static String getLogStructureWhen(
            final String logComponentsSetting) {
        if (logComponentsSetting.equals("ignore")) {
            logStructureWhen = Settings.settings()
                    .getString("LogStructureWhen", "never")
                    .toLowerCase();
        } else {
            logStructureWhen = logComponentsSetting;
        }

        switch (logStructureWhen) {
            case "activate":
            case "open":
            case "openclose":
            case "never":
                break;
            case "yes":
            case "true":
                logStructureWhen = "open";
                break;
            case "no":
            case "false":
                logStructureWhen = "never";
                break;

            // allow window event names
            case "activated":
            case "closed":
            case "closing":
            case "deactivated":
            case "deiconified":
            case "focused":
            case "iconified":
            case "lost focus":
            case "opened":
            case "state changed":
                break;

            default:
                Utils.logError(
                        "the LogStructureWhen setting is invalid: "
                        + logStructureWhen);
                logStructureWhen = "never";
        }

        return logStructureWhen;
    }

    /**
     * Logs detection of a window event.
     *
     * @param window the window that generated the event
     * @param eventID the AWT event identifier
     */
    static void logWindow(final Window window, final int eventID) {
        Utils.logToConsole(
                "detected " + getWindowTypeAndTitle(window)
                + "; event=" + SwingUtils.windowEventToString(eventID));
    }

    /**
     * Returns a string describing the type and title of a window.
     *
     * @param window the Window to describe
     * @return a string indicating whether it is a frame or dialog and the title
     */
    private static String getWindowTypeAndTitle(final Window window) {
        if (window == null) {
            throw new NullPointerException("window is null");
        }
        if (window instanceof JFrame) {
            return "frame entitled: " + SwingUtils.getWindowTitle(window);
        } else if (window instanceof JDialog) {
            return "dialog entitled: " + SwingUtils.getWindowTitle(window);
        } else {
            return "window: type=" + window.getClass().getName();
        }
    }

    /**
     * Logs the component structure of a window when configured to do so.
     *
     * @param window the window whose structure may be logged
     * @param eventID the AWT event identifier
     * @param windowKnown true if a handler recognised the window
     */
    static void logWindowStructure(
            final Window window,
            final int eventID,
            final boolean windowKnown) {
        if (logStructureScope.equals("known") && !windowKnown) {
            return;
        } else if (logStructureScope.equals("unknown") && windowKnown) {
            return;
        } else if (logStructureScope.equals("untitled")
                && !SwingUtils.getWindowTitle(window)
                        .equals(SwingUtils.NO_TITLE)) {
            return;
        }

        if ((eventID == WindowEvent.WINDOW_OPENED
                && (logStructureWhen.equals("open")
                        || logStructureWhen.equals("activate")))
                || (eventID == WindowEvent.WINDOW_ACTIVATED
                        && logStructureWhen.equals("activate"))
                || ((eventID == WindowEvent.WINDOW_OPENED
                        || eventID == WindowEvent.WINDOW_CLOSED)
                        && logStructureWhen.equals("openclose"))
                || (logStructureWhen.equalsIgnoreCase(
                        SwingUtils.windowEventToString(eventID)))) {
            Utils.logToConsole(
                    "Window structure for " + getWindowTypeAndTitle(window)
                    + "; event=" + SwingUtils.windowEventToString(eventID));
            Utils.logRawToConsole(SwingUtils.getWindowStructure(window));
        }
    }
}
