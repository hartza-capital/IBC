// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2022 Richard L King (rlking@aultan.com)
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;

/**
 * Tracks the state of the current TWS/Gateway session and signals when Gateway
 * initialisation has completed.
 */
public class SessionManager {
    
    /**
     * Initializes the SessionManager with information about whether the session is for Gateway or TWS.
     *
     * @param isGateway true if the session is for IB Gateway; false if for TWS
     */
    public static void initialise(boolean isGateway) {
        _isGateway = isGateway;
    }
    
    /** True when the session runs IB Gateway rather than TWS. */
    private static boolean _isGateway = false;
    /**
     * Indicates whether the current session is for IB Gateway.
     *
     * @return true if the session is for IB Gateway; false if for TWS
     */
    public static boolean isGateway() {
        return _isGateway;
    };
    
    /** True when the session uses the FIX protocol. */
    private static boolean _isFIX = false;
    /**
     * Indicates whether the current session uses the FIX protocol.
     * Terminates IBC if called before the session has started.
     *
     * @return true if the session is a FIX protocol session; false otherwise
     */
    public static boolean isFIX() {
        if (!_isSessionStarted) Utils.exitWithError(ErrorCodes.INVALID_STATE, "isFix() called before session has started");
        return _isFIX;
    }

    /** True when the session was started by an automatic restart. */
    private static boolean _isRestart;
    /**
     * Indicates whether the current session was started by an automatic restart rather
     * than a normal login. Terminates IBC if called before the session has started.
     *
     * @return true if the session is a restart; false otherwise
     */
    static boolean isRestart() {
        if (!_isSessionStarted) Utils.exitWithError(ErrorCodes.INVALID_STATE, "isRestart() called before session has started");
        return _isRestart;
    }
    
    /** True once {@link #startSession()} has run. */
    private static boolean _isSessionStarted = false;
    /**
     * Marks the session as started, records the FIX and restart flags, and schedules IBC
     * to exit if the login dialog is not displayed within the LoginDialogDisplayTimeout setting.
     */
    static void startSession() {
        _isSessionStarted = true;
        
        _isFIX = Settings.settings().getBoolean("FIX", false);
        
        // test to see if the -Drestart VM option has been supplied
        _isRestart = ! (System.getProperties().getProperty("restart", "").isEmpty());
        int loginDialogDisplayTimeout = Settings.settings().getInt("LoginDialogDisplayTimeout", 60);
        if (_isRestart){
            Utils.logToConsole("Re-starting session");
            // TWS/Gateway will re-establish the session with no intervention from IBC needed
        } else {
            Utils.logToConsole("Starting session: will exit if login dialog is not displayed within " + loginDialogDisplayTimeout + " seconds");
            MyScheduledExecutorService.getInstance().schedule(()->{
                GuiExecutor.instance().execute(()->{
                    if (LoginManager.loginManager().getLoginState() != LoginManager.LoginState.LOGGED_OUT) {
                        // Login diaog has been shown - no need for IBC to exit
                        return;
                    }
                    Utils.exitWithError(ErrorCodes.LOGIN_DIALOG_DISPLAY_TIMED_OUT, "IBC closing after TWS/Gateway failed to display login dialog");
                });
            }, loginDialogDisplayTimeout, TimeUnit.SECONDS);
        }
    }
    
    /** True once the session is ready for menu and dialog automation. */
    private static volatile boolean _InitialisationCompleted;
    /**
     * Synchronization lock protecting access to initialization state and
     * condition variable.
     */
    private static final Lock lock = new ReentrantLock();
    /**
     * Condition variable for signaling completion of Gateway initialization.
     * Signals when both the splash screen closes and non-brokerage account
     * dialog closes.
     */
    private static final Condition initialised = lock.newCondition();
    /**
     * Blocks the calling thread until the session is ready: Gateway initialisation has
     * finished, or the main window has been recorded for TWS.
     */
    static void awaitReady() {
        /*
         * For the gateway, the main form is loaded right at the start, and long before
         * the menu items become responsive: any attempt to access the Configure > Settings
         * menu item (even after it has been enabled) results in an exception being logged
         * by Gateway. 
         * 
         * It's not obvious how long we need to wait before the menu becomes responsive. However the splash
         * frame that appears in front of the gateway main window during initialisation disappears when everything
         * is ready, and its close can be detected as a frame entitled 'Starting application' and a Closed event.
         * 
         * So we wait for the handler for that frame to call setSplashScreenClosed().
         * 
         */

        lock.lock();
        try {
            while (!_InitialisationCompleted) {
                try {
                    initialised.await();
                } catch (InterruptedException e) {
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** True once the Gateway splash screen has closed. */
    private static volatile boolean splashScreenClosed;
    /**
     * Records that the Gateway splash screen has closed. Completion is signalled once
     * the non-brokerage account dialog has also closed; has no effect for TWS sessions.
     */
    static void setSplashScreenClosed() {
        if (!SessionManager.isGateway()) return;
        lock.lock();
        try {
            splashScreenClosed = true;
            if (nonBrokerageAccountDialogClosed) {
                _InitialisationCompleted = true;
                initialised.signal();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /** True once the non-brokerage account dialog has closed. */
    private static volatile boolean nonBrokerageAccountDialogClosed;
    /**
     * Records that the non-brokerage account dialog has closed. Completion is signalled
     * once the Gateway splash screen has also closed; has no effect for TWS sessions.
     */
    static void setNonBrokerageAccountDialogClosed() {
        if (!SessionManager.isGateway()) return;
        lock.lock();
        try {
            nonBrokerageAccountDialogClosed = true;
            if (splashScreenClosed) {
                _InitialisationCompleted = true;
                initialised.signal();
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Records the session's main window and marks the session as initialised.
     *
     * @param window the main window of TWS/Gateway
     */
    static void setMainWindow(JFrame window) {
        lock.lock();
        try {
            _InitialisationCompleted = true;
            initialised.signal();
        } finally {
            lock.unlock();
            MainWindowManager.mainWindowManager().setMainWindow(window);
        }
    }
}
