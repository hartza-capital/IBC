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
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFrame;

/**
 * Coordinates the IBC login lifecycle, including state tracking, session
 * timeouts, re-login after second-factor authentication, and credential
 * retrieval.
 */
public abstract class LoginManager {

    /** The singleton LoginManager instance used by the rest of IBC. */
    private static LoginManager loginManager;

    /** Delay before re-initiating login after a 2FA timeout, in seconds. */
    private static final int SECOND_FACTOR_RELOGIN_DELAY_SECONDS = 5;

    /** Fallback error code used when an unexpected exception occurs. */
    private static final int UNEXPECTED_ERROR_CODE = 99999;

    /** Error code for second-factor authentication login timeout. */
    private static final int LOGIN_TIMEOUT_ERROR_CODE =
            ErrorCodes.SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT;

    static {
        loginManager = new DefaultLoginManager();
    }

    /**
     * Validates the supplied LoginManager instance.
     *
     * @param instance the LoginManager instance to validate; must not be null
     * @throws IllegalArgumentException if instance is null
     */
    public static void initialise(final LoginManager instance) {
        if (instance == null) {
            throw new IllegalArgumentException("loginManager");
        }
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

    /** The possible states of the IBC login process. */
    public enum LoginState {
        /** Not currently logged in. */
        LOGGED_OUT,
        /** Successfully logged in. */
        LOGGED_IN,
        /** Login is in progress. */
        LOGGING_IN,
        /** Second factor authentication is in progress. */
        TWO_FA_IN_PROGRESS,
        /** The login attempt failed. */
        LOGIN_FAILED,
        /** Awaiting credential entry. */
        AWAITING_CREDENTIALS
    }

    /**
     * Determines whether a read-only login should be attempted.
     * Read-only login is not supported by Gateway, so this returns false when
     * running in Gateway mode even if the setting is enabled.
     *
     * @return true if read-only login is required and supported; false
     *     otherwise
     */
    boolean readonlyLoginRequired() {
        final boolean readOnly =
                Settings.settings().getBoolean("ReadOnlyLogin", false);
        if (readOnly && SessionManager.isGateway()) {
            Utils.logError("Read-only login not supported by Gateway");
            return false;
        }
        return readOnly;
    }

    /** The Swing frame used for the login dialog. */
    private volatile JFrame loginFrame = null;

    /**
     * Returns the login dialog frame.
     *
     * @return the login frame
     */
    JFrame getLoginFrame() {
        return loginFrame;
    }

    /**
     * Sets the login dialog frame.
     *
     * @param window the login frame
     */
    void setLoginFrame(final JFrame window) {
        loginFrame = window;
    }

    /** The current state of the login process. */
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
        if (loginState != null) {
            switch (loginState) {
                case TWO_FA_IN_PROGRESS:
                    Utils.logToConsole(
                            "Second Factor Authentication initiated");
                    loginStartTime.compareAndSet(null, Instant.now());
                    break;
                case LOGGING_IN:
                    loginStartTime.compareAndSet(null, Instant.now());
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

    /** Timestamp when the current login attempt started. */
    private final AtomicReference<Instant> loginStartTime =
            new AtomicReference<>();

    /** Scheduled task that will shut down IBC after a login timeout. */
    private volatile ScheduledFuture<?> shutdownAfterTimeTask;

    /**
     * Called when the second factor authentication dialog closes, to decide
     * whether login completed, a timeout occurred, or a re-login is needed.
     */
    void secondFactorAuthenticationDialogClosed() {
        final Instant startTime = loginStartTime.get();
        if (startTime == null) {
            // login did not proceed from the SecondFactorAuthentication
            // dialog - for example because no second factor device could be
            // selected
            return;
        }

        // Second factor authentication dialog timeout period
        final int secondFactorAuthenticationTimeout =
                Settings.settings().getInt(
                        "SecondFactorAuthenticationTimeout", 180);

        // time (seconds) to allow for login to complete before exiting
        final int exitInterval =
                Settings.settings().getInt(
                        "SecondFactorAuthenticationExitInterval", 60);

        final Duration elapsed = Duration.between(startTime, Instant.now());
        loginStartTime.set(null);

        Utils.logToConsole(
                "Duration since login: " + elapsed.getSeconds() + " seconds");

        if (elapsed.getSeconds() < secondFactorAuthenticationTimeout) {
            // The 2FA prompt must have been handled by the user, so
            // authentication should be under way

            if (SessionManager.isFIX()) {
                // no Splash screen is dislayed for FIX Gateway - just let
                // things run
                LoginManager.loginManager().setLoginState(
                        LoginManager.LoginState.LOGGED_IN);
                return;
            }

            if (!reloginPermitted()) {
                // just let loading continue
                return;
            }

            Utils.logToConsole(
                    "If login has not completed, IBC will exit in "
                            + exitInterval + " seconds");
            restartAfterTime(
                    exitInterval,
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
                        + SECOND_FACTOR_RELOGIN_DELAY_SECONDS + " second");
        MyScheduledExecutorService.getInstance().schedule(() -> {
            GuiDeferredExecutor.instance().execute(
                    () -> getLoginHandler().initiateLogin(getLoginFrame()));
        }, SECOND_FACTOR_RELOGIN_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Determines whether re-login is permitted after a second factor
     * authentication timeout.
     *
     * @return true if re-login is permitted; false otherwise
     */
    private boolean reloginPermitted() {
        if (Settings.settings().getString(
                "ReloginAfterSecondFactorAuthenticationTimeout", "")
                .isEmpty()) {
            if (!Settings.settings().getString(
                    "ExitAfterSecondFactorAuthenticationTimeout", "")
                    .isEmpty()) {
                Utils.logToConsole(
                        "[WARN] Setting "
                            + "'ExitAfterSecondFactorAuthenticationTimeout' "
                            + "is deprecated. Use "
                            + "'ReloginAfterSecondFactorAuthenticationTimeout' "
                            + "instead.");
                return Settings.settings().getBoolean(
                        "ExitAfterSecondFactorAuthenticationTimeout", false);
            }
            return false;
        }
        return Settings.settings().getBoolean(
                "ReloginAfterSecondFactorAuthenticationTimeout", false);
    }

    /**
     * Schedules a shutdown after the specified delay if login has not
     * completed by then.
     *
     * @param secondsTillShutdown the delay before shutdown, in seconds
     * @param message the shutdown reason to log
     */
    void restartAfterTime(final int secondsTillShutdown,
                          final String message) {
        try {
            shutdownAfterTimeTask =
                    MyScheduledExecutorService.getInstance().schedule(() -> {
                        GuiExecutor.instance().execute(() -> {
                            if (getLoginState()
                                    == LoginManager.LoginState.LOGGED_IN) {
                                Utils.logToConsole(
                                        "Login has already completed - "
                                                + "no need for IBC to exit");
                                return;
                            }
                            Utils.exitWithError(
                                    LOGIN_TIMEOUT_ERROR_CODE, message);

                        });
                    }, secondsTillShutdown, TimeUnit.SECONDS);
        } catch (Throwable e) {
            Utils.exitWithException(UNEXPECTED_ERROR_CODE, e);
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
     * @return the FIX password; callers should zero the array after use
     */
    @SuppressWarnings("checkstyle:MethodName")
    public abstract char[] FIXPassword();

    /**
     * Returns the FIX login username from either arguments or settings.
     *
     * @return the FIX username
     */
    @SuppressWarnings("checkstyle:MethodName")
    public abstract String FIXUserName();

    /**
     * Returns the TWS login password from either arguments or settings.
     *
     * @return the TWS password; callers should zero the array after use
     */
    @SuppressWarnings("checkstyle:MethodName")
    public abstract char[] IBAPIPassword();

    /**
     * Returns the TWS login username from either arguments or settings.
     *
     * @return the TWS username
     */
    @SuppressWarnings("checkstyle:MethodName")
    public abstract String IBAPIUserName();

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
