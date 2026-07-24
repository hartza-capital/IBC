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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;

public abstract class LoginManager {

    private static LoginManager loginManager;

    static {
        loginManager = new DefaultLoginManager();
    }

    /**
     * Sets the global LoginManager instance to the specified implementation.
     *
     * @param loginManagerImpl the LoginManager instance to use; must not
     *                        be null
     * @throws IllegalArgumentException if loginManagerImpl is null
     */
    public static void initialise(final LoginManager loginManagerImpl) {
        if (loginManagerImpl == null) {
            throw new IllegalArgumentException("loginManager");
        }
        loginManager = loginManagerImpl;
    }

    /**
     * Resets the global LoginManager instance to a new DefaultLoginManager.
     */
    public static void setDefault() {
        loginManager = new DefaultLoginManager();
    }

    /**
     * Returns the current global LoginManager instance.
     *
     * @return the active LoginManager instance
     */
    public static LoginManager loginManager() {
        return loginManager;
    }

    public enum LoginState {
        LOGGED_OUT,
        LOGGED_IN,
        LOGGING_IN,
        TWO_FA_IN_PROGRESS,
        LOGIN_FAILED,
        AWAITING_CREDENTIALS
    }

    /**
     * Determines whether a read-only login is required.
     *
     * @return true if read-only login is required; false otherwise
     */
    boolean readonlyLoginRequired() {
        boolean readOnly = Settings.settings()
            .getBoolean("ReadOnlyLogin", false);
        if (readOnly && SessionManager.isGateway()) {
            Utils.logError("Read-only login not supported by Gateway");
            return false;
        }
        return readOnly;
    }

    private volatile JFrame loginFrame = null;

    /**
     * Returns the current login frame window.
     *
     * @return the login frame
     */
    JFrame getLoginFrame() {
        return loginFrame;
    }

    /**
     * Sets the login frame window.
     *
     * @param window the login frame to set
     */
    void setLoginFrame(final JFrame window) {
        loginFrame = window;
    }

    private volatile LoginState loginState = LoginState.LOGGED_OUT;

    /**
     * Returns the current login state.
     *
     * @return the current login state
     */
    public LoginState getLoginState() {
        return loginState;
    }

    /**
     * Sets the login state and handles associated state transitions and
     * logging.
     *
     * @param state the new login state
     */
    public void setLoginState(final LoginState state) {
        if (state == loginState) {
            return;
        }
        loginState = state;
        if (null != loginState) {
            switch (loginState) {
                case TWO_FA_IN_PROGRESS:
                    Utils.logToConsole(
                        "Second Factor Authentication initiated");
                    if (loginStartTime == null) {
                        loginStartTime = Instant.now();
                    }
                    break;
                case LOGGING_IN:
                    if (loginStartTime == null) {
                        loginStartTime = Instant.now();
                    }
                    break;
                case LOGGED_IN:
                    Utils.logToConsole("Login has completed");
                    if (shutdownAfterTimeTask != null) {
                        shutdownAfterTimeTask.cancel(false);
                        shutdownAfterTimeTask = null;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private static final int LOGIN_RESTART_DELAY_SECONDS = 5;
    private static final int DEFAULT_SECOND_FACTOR_AUTH_EXIT_CODE = 99999;

    private Instant loginStartTime;
    private ScheduledFuture<?> shutdownAfterTimeTask;

    /**
     * Handles the closure of the second factor authentication dialog.
     */
    void secondFactorAuthenticationDialogClosed() {
        if (loginStartTime == null) {
            // login did not proceed from the SecondFactorAuthentication dialog
            // - for example because no second factor device could be selected
            return;
        }

        // Second factor authentication dialog timeout period
        final int secondFactorAuthenticationTimeout =
            Settings.settings()
                .getInt("SecondFactorAuthenticationTimeout", 180);

        // time (seconds) to allow for login to complete before exiting
        final int exitInterval = Settings.settings()
            .getInt("SecondFactorAuthenticationExitInterval", 60);

        final Duration d = Duration.between(loginStartTime, Instant.now());
        loginStartTime = null;

        Utils.logToConsole("Duration since login: " + d.getSeconds()
            + " seconds");

        if (d.getSeconds() < secondFactorAuthenticationTimeout) {
            // The 2FA prompt must have been handled by the user, so
            // authentication should be under way

            if (SessionManager.isFIX()) {
                // no Splash screen is displayed for FIX Gateway - just
                // let things run
                LoginManager.loginManager()
                    .setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }

            if (!reloginPermitted()) {
                // just let loading continue
                return;
            }

            Utils.logToConsole("If login has not completed, IBC will exit"
                + " in " + exitInterval + " seconds");
            restartAfterTime(exitInterval,
                "IBC closing because login has not completed after "
                + "Second Factor Authentication");
            return;
        }

        if (!reloginPermitted()) {
            Utils.logToConsole(
                "Re-login after second factor authentication timeout "
                + "not required");
            return;
        }

        // The 2FA prompt hasn't been handled by the user, so we re-initiate
        // the login sequence after a short delay
        Utils.logToConsole(
            "Re-login after second factor authentication timeout in "
            + LOGIN_RESTART_DELAY_SECONDS + " second");
        MyScheduledExecutorService.getInstance().schedule(() -> {
            GuiDeferredExecutor.instance().execute(() -> {
                getLoginHandler().initiateLogin(getLoginFrame());
            });
        }, LOGIN_RESTART_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Determines whether re-login is permitted after a second factor
     * authentication timeout.
     *
     * @return true if re-login is permitted; false otherwise
     */
    private boolean reloginPermitted() {
        if (Settings.settings()
            .getString("ReloginAfterSecondFactorAuthenticationTimeout", "")
            .isEmpty()) {
            if (!Settings.settings()
                .getString("ExitAfterSecondFactorAuthenticationTimeout", "")
                .isEmpty()) {
                return Settings.settings()
                    .getBoolean(
                        "ExitAfterSecondFactorAuthenticationTimeout", false);
            }
            return false;
        }
        return Settings.settings()
            .getBoolean("ReloginAfterSecondFactorAuthenticationTimeout",
                false);
    }

    /**
     * Schedules a restart after a specified time interval.
     *
     * @param secondsTillShutdown seconds until shutdown
     * @param message the message to log when shutdown occurs
     */
    void restartAfterTime(final int secondsTillShutdown,
                          final String message) {
        try {
            shutdownAfterTimeTask = MyScheduledExecutorService
                .getInstance().schedule(() -> {
                    GuiExecutor.instance().execute(() -> {
                        if (getLoginState()
                            == LoginManager.LoginState.LOGGED_IN) {
                            Utils.logToConsole(
                                "Login has already completed - no need"
                                + " for IBC to exit");
                            return;
                        }
                        Utils.exitWithError(
                            ErrorCodes.SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT,
                            message);
                    });
                }, secondsTillShutdown, TimeUnit.SECONDS);
        } catch (Throwable e) {
            Utils.exitWithException(DEFAULT_SECOND_FACTOR_AUTH_EXIT_CODE,
                e);
        }
    }

    /**
     * Logs diagnostic information about the login manager and credential
     * sources.
     */
    public abstract void logDiagnosticMessage();

    /**
     * Returns the FIX login password from either arguments or settings.
     *
     * @return the FIX password
     */
    public abstract String fixPassword();

    /**
     * Returns the FIX login username from either arguments or settings.
     *
     * @return the FIX username
     */
    public abstract String fixUserName();

    /**
     * Returns the TWS login password from either arguments or settings.
     *
     * @return the TWS password
     */
    public abstract String ibapiPassword();

    /**
     * Returns the TWS login username from either arguments or settings.
     *
     * @return the TWS username
     */
    public abstract String ibapiUserName();

    /**
     * Returns the current login handler.
     *
     * @return the login handler instance
     */
    public abstract AbstractLoginHandler getLoginHandler();

    /**
     * Sets the login handler to be used for login automation.
     *
     * @param handler the login handler instance to use
     */
    public abstract void setLoginHandler(AbstractLoginHandler handler);

}
