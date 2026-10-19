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
 * AWT event listener that dispatches window events to the registered
 * WindowHandler instances and optionally logs window structure.
 */
class TwsListener
        implements AWTEventListener {

    /** Handlers consulted, in order, for each window event. */
    private final List<WindowHandler> windowHandlers;

    /** Which windows have their component structure logged. */
    private static String logStructureScope;
    /** Which window event makes the component structure be logged. */
    private static String logStructureWhen;

    /**
     * Creates a listener over the given window handlers and loads the window structure
     * logging parameters.
     *
     * @param windowHandlers the handlers to consult, in order, for each window event
     */
    TwsListener (List<WindowHandler> windowHandlers) {
        this.windowHandlers = windowHandlers;
        getLogStructureParameters();
    }

    /**
     * Handles AWTEvents dispatched by the Swing toolkit.
     * Filters for window events and iterates through registered WindowHandler instances
     * to find and execute handlers that recognize the window.
     * All handler execution is deferred to the GUI event dispatch thread.
     *
     * @param event the AWTEvent to process
     */
    @Override
    public void eventDispatched(AWTEvent event) {
        try {
            final int eventID = event.getID();

            final Window window;
            window = ((WindowEvent) event).getWindow();

            GuiDeferredExecutor.instance().execute(() -> {
                try{
                    logWindow(window, eventID);

                    for (WindowHandler wh : windowHandlers) {
                        if (wh.recogniseWindow(window))  {
                                logWindowStructure(window, eventID, true);
                                if (wh.filterEvent(window, eventID)) wh.handleWindow(window, eventID);
                            return;
                        }
                    }

                    logWindowStructure(window, eventID, false);
                } catch (Throwable e) {
                    Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
                }
            });
        } catch (Throwable e) {
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
        }
    }
    
    /**
     * Loads the window structure logging parameters from the LogComponents and
     * LogStructureScope/LogStructureWhen settings.
     */
    private static void getLogStructureParameters() {
        // legacy deprecated setting overrides explicit values of LogStructureScope 
        // and LogStructureWhen
        final String logComponentsSetting = Settings.settings().getString("LogComponents", "ignore").toLowerCase();
        
        logStructureScope = getLogStructureScope(logComponentsSetting);
        logStructureWhen = getLogStructureWhen(logComponentsSetting);
    }
    
    /**
     * Determines the scope of windows whose component structure is logged, and
     * stores it in the shared scope field.
     *
     * <p>An unrecognised value is logged as an error and replaced by "known".</p>
     *
     * @param logComponentsSetting LogComponents setting value; anything other than "ignore" selects scope "all"
     * @return one of "known", "unknown", "untitled" or "all"
     */
    private static String getLogStructureScope(String logComponentsSetting) {
        if (logComponentsSetting.equals("ignore")) {
            logStructureScope = Settings.settings().getString("LogStructureScope", "known").toLowerCase();
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
                Utils.logError("the LogStructureScope setting '" + logStructureScope + "' is invalid.");
                logStructureScope = "known";
        }
        
        return logStructureScope;
    }
    
    /**
     * Determines which window event triggers component structure logging, and
     * stores it in the shared timing field.
     *
     * <p>"yes"/"true" are treated as "open" and "no"/"false" as "never". An
     * unrecognised value is logged as an error and replaced by "never".</p>
     *
     * @param logComponentsSetting LogComponents setting value; anything other than "ignore" is used as the timing value
     * @return one of "activate", "open", "openclose", "never" or a window event name
     */
    private static String getLogStructureWhen(String logComponentsSetting) {
        if (logComponentsSetting.equals("ignore")) {
            logStructureWhen = Settings.settings().getString("LogStructureWhen", "never").toLowerCase();
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
                Utils.logError("the LogStructureWhen setting is invalid: " + logStructureWhen);
                logStructureWhen = "never";
        }
        
        return logStructureWhen;
    }

    /**
     * Logs the type, title and event of a window detected by the listener.
     *
     * @param window the window to log
     * @param eventID the AWT window event ID
     */
    static void logWindow(Window window, int eventID) {
        Utils.logToConsole("detected " + getWindowTypeAndTitle(window) + "; event=" + SwingUtils.windowEventToString(eventID));
    }

    /**
     * Describes a window by its type and title, for logging.
     *
     * @param window the window to describe
     * @return a description naming the window as a frame, dialog or other component
     * @throws NullPointerException if window is null
     */
    private static String getWindowTypeAndTitle(Window window) {
        if (window == null) throw new NullPointerException("window is null");
        if (window instanceof JFrame) {
            return "frame entitled: " + SwingUtils.getWindowTitle(window);
        } else if (window instanceof JDialog) {
            return "dialog entitled: " + SwingUtils.getWindowTitle(window);
        } else {
            return "window: type=" + window.getClass().getName();
        }
    }

    /**
     * Logs the component structure of a window when it matches the configured scope
     * and event.
     *
     * @param window the window whose structure may be logged
     * @param eventID the AWT window event ID
     * @param windowKnown true if a handler recognised the window; false otherwise
     */
    static void logWindowStructure(Window window, int eventID, boolean windowKnown) {
        if (logStructureScope.equals("known") && !windowKnown) {
            return;
        } else if (logStructureScope.equals("unknown") && windowKnown) {
            return;
        } else if (logStructureScope.equals("untitled") && 
                !SwingUtils.getWindowTitle(window).equals(SwingUtils.NO_TITLE)) {
            return;
        }

        if ((eventID == WindowEvent.WINDOW_OPENED && (logStructureWhen.equals("open") || logStructureWhen.equals("activate")))
            ||
            (eventID == WindowEvent.WINDOW_ACTIVATED && logStructureWhen.equals("activate"))
            ||
            ((eventID == WindowEvent.WINDOW_OPENED || eventID == WindowEvent.WINDOW_CLOSED) && logStructureWhen.equals("openclose"))
            ||
            (logStructureWhen.equalsIgnoreCase(SwingUtils.windowEventToString(eventID))))
        {
            Utils.logToConsole("Window structure for " + getWindowTypeAndTitle(window) + "; event=" + SwingUtils.windowEventToString(eventID));
            Utils.logRawToConsole(SwingUtils.getWindowStructure(window));
        }
    }
}



