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
 * Manages the lifecycle and initialization state of an IBC session.
 */
public final class SessionManager {

    /** true if the current session is for IB Gateway rather than TWS. */
    private static boolean gatewaySession = false;

    /** true if the current session is a FIX protocol session. */
    private static boolean fixSession = false;

    /** true if this process was started with the -Drestart VM option. */
    private static boolean restartSession;

    /** true once {@link #startSession()} has been called. */
    private static boolean sessionStarted = false;

    /** true when Gateway initialization has fully completed. */
    private static volatile boolean initialisationCompleted;

    /** true once the Gateway splash screen has closed. */
    private static volatile boolean splashScreenClosed;

    /** true once the Gateway non-brokerage account dialog has closed. */
    private static volatile boolean nonBrokerageAccountDialogClosed;

    /**
     * Synchronization lock protecting access to initialization state and the
     * condition variable.
     */
    private static final Lock INITIALISATION_LOCK = new ReentrantLock();

    /**
     * Condition variable for signaling completion of Gateway initialization.
     * Signals when both the splash screen closes and the non-brokerage account
     * dialog closes.
     */
    private static final Condition INITIALISATION_CONDITION =
            INITIALISATION_LOCK.newCondition();

    /** Default login dialog display timeout in seconds. */
    private static final int DEFAULT_LOGIN_DIALOG_TIMEOUT_SECONDS = 60;

    /**
     * Prevent instantiation of this utility class.
     */
    private SessionManager() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Initializes the SessionManager with information about whether the
     * session is for Gateway or TWS.
     *
     * @param isGateway true if the session is for IB Gateway; false if for TWS
     */
    public static void initialise(final boolean isGateway) {
        gatewaySession = isGateway;
    }

    /**
     * Indicates whether the current session is for IB Gateway.
     *
     * @return true if the session is for IB Gateway; false if for TWS
     */
    public static boolean isGateway() {
        return gatewaySession;
    }

    /**
     * Indicates whether the current session is a FIX protocol session.
     *
     * @return true if the session is a FIX protocol session; false otherwise
     * @throws SystemException if called before the session has started
     */
    public static boolean isFIX() {
        if (!sessionStarted) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    "isFix() called before session has started");
        }
        return fixSession;
    }

    /**
     * Indicates whether the current process was started as a session restart.
     *
     * @return true if the process was started with the -Drestart option
     */
    static boolean isRestart() {
        if (!sessionStarted) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    "isRestart() called before session has started");
        }
        return restartSession;
    }

    /**
     * Marks the session as started and schedules the login dialog timeout.
     */
    static void startSession() {
        sessionStarted = true;

        fixSession = Settings.settings().getBoolean("FIX", false);

        // test to see if the -Drestart VM option has been supplied
        restartSession =
                !(System.getProperties().getProperty("restart", "")
                        .isEmpty());
        int loginDialogDisplayTimeout =
                Settings.settings().getInt("LoginDialogDisplayTimeout",
                        DEFAULT_LOGIN_DIALOG_TIMEOUT_SECONDS);
        if (restartSession) {
            Utils.logToConsole("Re-starting session");
            // TWS/Gateway will re-establish the session with no intervention
            // from IBC needed
        } else {
            Utils.logToConsole(
                    "Starting session: will exit if login dialog is not"
                            + " displayed within "
                            + loginDialogDisplayTimeout + " seconds");
            final int timeoutErrorCode =
                    ErrorCodes.LOGIN_DIALOG_DISPLAY_TIMED_OUT;
            MyScheduledExecutorService.getInstance().schedule(
                    () -> {
                        GuiExecutor.instance().execute(
                                () -> {
                                    LoginManager.LoginState loginState =
                                            LoginManager.loginManager()
                                                    .getLoginState();
                                    if (loginState
                                            != LoginManager.LoginState
                                                    .LOGGED_OUT) {
                                        // Login dialog has been shown - no
                                        // need for IBC to exit
                                        return;
                                    }
                                    Utils.exitWithError(
                                            timeoutErrorCode,
                                            "IBC closing after TWS/Gateway"
                                                    + " failed to display"
                                                    + " login dialog");
                                });
                    },
                    loginDialogDisplayTimeout,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Waits until Gateway initialization has completed.
     */
    static void awaitReady() {
        /*
         * For the gateway, the main form is loaded right at the start, and
         * long before the menu items become responsive: any attempt to access
         * the Configure > Settings menu item (even after it has been enabled)
         * results in an exception being logged by Gateway.
         *
         * It's not obvious how long we need to wait before the menu becomes
         * responsive. However the splash frame that appears in front of the
         * gateway main window during initialisation disappears when
         * everything is ready, and its close can be detected as a frame
         * entitled 'Starting application...' and a Closed event.
         *
         * So we wait for the handler for that frame to call
         * setSplashScreenClosed().
         */

        INITIALISATION_LOCK.lock();
        try {
            while (!initialisationCompleted) {
                try {
                    INITIALISATION_CONDITION.await();
                } catch (InterruptedException e) {
                    // interrupted while waiting; continue checking
                }
            }
        } finally {
            INITIALISATION_LOCK.unlock();
        }
    }

    /**
     * Signals that the Gateway splash screen has closed.
     */
    static void setSplashScreenClosed() {
        if (!SessionManager.isGateway()) {
            return;
        }
        INITIALISATION_LOCK.lock();
        try {
            splashScreenClosed = true;
            if (nonBrokerageAccountDialogClosed) {
                initialisationCompleted = true;
                INITIALISATION_CONDITION.signal();
            }
        } finally {
            INITIALISATION_LOCK.unlock();
        }
    }

    /**
     * Signals that the Gateway non-brokerage account dialog has closed.
     */
    static void setNonBrokerageAccountDialogClosed() {
        if (!SessionManager.isGateway()) {
            return;
        }
        INITIALISATION_LOCK.lock();
        try {
            nonBrokerageAccountDialogClosed = true;
            if (splashScreenClosed) {
                initialisationCompleted = true;
                INITIALISATION_CONDITION.signal();
            }
        } finally {
            INITIALISATION_LOCK.unlock();
        }
    }

    /**
     * Stores the main TWS/Gateway window and completes initialization.
     *
     * @param window the main application window
     */
    static void setMainWindow(final JFrame window) {
        INITIALISATION_LOCK.lock();
        try {
            initialisationCompleted = true;
            INITIALISATION_CONDITION.signal();
        } finally {
            INITIALISATION_LOCK.unlock();
            MainWindowManager.mainWindowManager().setMainWindow(window);
        }
    }
}
