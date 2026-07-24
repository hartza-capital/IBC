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

public final class SessionManager {
    /**
     * Private constructor to hide default constructor for utility class.
     */
    private SessionManager() {
    }

    /**
     * Initializes the SessionManager with information about whether the
     * session is for Gateway or TWS.
     *
     * @param isGateway true if the session is for IB Gateway; false if for TWS
     */
    public static void initialise(final boolean isGateway) {
        isGatewayFlag = isGateway;
    }

    private static boolean isGatewayFlag = false;
    /**
     * Indicates whether the current session is for IB Gateway.
     *
     * @return true if the session is for IB Gateway; false if for TWS
     */
    public static boolean isGateway() {
        return isGatewayFlag;
    }

    private static boolean isFIXFlag = false;
    /**
     * Indicates whether the current session is a FIX protocol session.
     *
     * @return true if the session is a FIX protocol session; false otherwise
     * @throws SystemException if called before the session has started
     */
    public static boolean isFIX() {
        if (!isSessionStartedFlag) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    "isFix() called before session has started");
        }
        return isFIXFlag;
    }

    private static boolean isRestartFlag;

    /**
     * Indicates whether the current session is a restart.
     *
     * @return true if the session is restarting; false otherwise
     */
    static boolean isRestart() {
        if (!isSessionStartedFlag) {
            Utils.exitWithError(ErrorCodes.INVALID_STATE,
                    "isRestart() called before session has started");
        }
        return isRestartFlag;
    }

    private static boolean isSessionStartedFlag = false;
    private static final int DEFAULT_LOGIN_DIALOG_TIMEOUT_SECONDS = 60;

    /**
     * Starts the session and sets up initial state and monitoring.
     */
    static void startSession() {
        isSessionStartedFlag = true;

        isFIXFlag = Settings.settings().getBoolean("FIX", false);

        // test to see if the -Drestart VM option has been supplied
        isRestartFlag = !(System.getProperties()
                .getProperty("restart", "").isEmpty());
        int loginDialogDisplayTimeout = Settings.settings()
                .getInt("LoginDialogDisplayTimeout",
                        DEFAULT_LOGIN_DIALOG_TIMEOUT_SECONDS);
        if (isRestartFlag) {
            Utils.logToConsole("Re-starting session");
            // TWS/Gateway will re-establish the session with no intervention
            // from IBC needed
        } else {
            Utils.logToConsole(
                    "Starting session: will exit if login dialog is not "
                    + "displayed within " + loginDialogDisplayTimeout
                    + " seconds");
            MyScheduledExecutorService.getInstance().schedule(() -> {
                GuiExecutor.instance().execute(() -> {
                    if (LoginManager.loginManager().getLoginState()
                            != LoginManager.LoginState.LOGGED_OUT) {
                        // Login dialog has been shown - no need for IBC
                        // to exit
                        return;
                    }
                    Utils.exitWithError(
                            ErrorCodes.LOGIN_DIALOG_DISPLAY_TIMED_OUT,
                            "IBC closing after TWS/Gateway failed to "
                            + "display login dialog");
                });
            }, loginDialogDisplayTimeout, TimeUnit.SECONDS);
        }
    }

    private static volatile boolean initialisationCompleted;
    /**
     * Synchronization lock protecting access to initialization state and
     * condition variable.
     */
    private static final Lock LOCK = new ReentrantLock();
    /**
     * Condition variable for signaling completion of Gateway initialization.
     * Signals when both the splash screen closes and non-brokerage account
     * dialog closes.
     */
    private static final Condition INITIALISED = LOCK.newCondition();
    /**
     * Awaits the completion of the initialization sequence.
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

        LOCK.lock();
        try {
            while (!initialisationCompleted) {
                try {
                    INITIALISED.await();
                } catch (InterruptedException e) {
                    // Continue waiting if interrupted
                }
            }
        } finally {
            LOCK.unlock();
        }
    }

    private static volatile boolean splashScreenClosed;

    /**
     * Marks the splash screen as closed.
     */
    static void setSplashScreenClosed() {
        if (!SessionManager.isGateway()) {
            return;
        }
        LOCK.lock();
        try {
            splashScreenClosed = true;
            if (nonBrokerageAccountDialogClosed) {
                initialisationCompleted = true;
                INITIALISED.signal();
            }
        } finally {
            LOCK.unlock();
        }
    }

    private static volatile boolean nonBrokerageAccountDialogClosed;

    /**
     * Marks the non-brokerage account dialog as closed.
     */
    static void setNonBrokerageAccountDialogClosed() {
        if (!SessionManager.isGateway()) {
            return;
        }
        LOCK.lock();
        try {
            nonBrokerageAccountDialogClosed = true;
            if (splashScreenClosed) {
                initialisationCompleted = true;
                INITIALISED.signal();
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Sets the main window and marks initialization as complete.
     *
     * @param window the main window frame
     */
    static void setMainWindow(final JFrame window) {
        LOCK.lock();
        try {
            initialisationCompleted = true;
            INITIALISED.signal();
        } finally {
            LOCK.unlock();
            MainWindowManager.mainWindowManager().setMainWindow(window);
        }
    }
}
