// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2021 Richard L King (rlking@aultan.com)
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

import java.awt.Window;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;

public class TooManyFailedLoginAttemptsDialogHandler implements WindowHandler {

    /**
     * Extra seconds added to the parsed lockout duration to ensure the re-login
     * attempt occurs after the lockout has expired.
     */
    private static final int EXTRA_DELAY_SECONDS = 3;

    /**
     * Determines whether to process the given AWT event for this window.
     *
     * <p>Only handles WINDOW_OPENED events.</p>
     *
     * @param window the window that generated the event
     * @param eventId the AWT event ID
     * @return true if eventId is WINDOW_OPENED, false otherwise
     */
    @Override
    public boolean filterEvent(final Window window, final int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles the "Too many failed login attempts" dialog by parsing the wait
     * time from the message and optionally scheduling an automatic re-login
     * attempt if configured.
     *
     * <p>Parses the message to extract minutes and seconds, then schedules a
     * re-login task if ReloginAfterSecondFactorAuthenticationTimeout is
     * enabled.</p>
     *
     * @param window the "Too many failed login attempts" dialog
     * @param eventID the AWT event ID
     */
    @Override
    public void handleWindow(final Window window, final int eventID) {
        // this dialog will contain a text area with a message like this:
        //      "Too many failed login attempts. Please wait 53 seconds before
        //          attempting to re-login again."
        // or like this:
        //      "Too many failed login attempts. Please wait 4 minutes & 47
        //          seconds before attempting to re-login again."
        //
        final String message = SwingUtils
                .findTextArea(window, "Too many failed login attempts")
                .getText();
        Utils.logToConsole(message);
        final Pattern pattern = Pattern.compile(
                "(?:Too many failed login attempts. Please wait "
                        + "(?:(\\d\\d?) minute(?:s)? )?"
                        + "(?:& )?(?:(\\d\\d?) second(?:s)?)?)?");
        final Matcher matcher = pattern.matcher(message);
        String minutes = "";
        String seconds = "";
        if (matcher.find()) {
            minutes = matcher.group(1);
            if (minutes == null || minutes.isEmpty()) {
                minutes = "0";
            }
            seconds = matcher.group(2);
            if (seconds == null || seconds.isEmpty()) {
                seconds = "0";
            }
        }
        final Duration waitFor = Duration.parse("PT" + minutes + "M" + seconds
                + "S").plus(Duration.ofSeconds(EXTRA_DELAY_SECONDS));

        if (Settings.settings().getBoolean(
                "ReloginAfterSecondFactorAuthenticationTimeout", false)) {
            final LocalDateTime reLoginTime = LocalDateTime.now().plus(waitFor);
            final int nextLoginAttemptNumber = LoginManager.loginManager()
                    .getLoginHandler().currentLoginAttemptNumber() + 1;
            Utils.logToConsole("Will re-login at "
                    + Utils.formatDate(reLoginTime)
                    + "; login number: " + nextLoginAttemptNumber);

            MyScheduledExecutorService.getInstance().schedule(() -> {
                GuiDeferredExecutor.instance().execute(
                        () -> LoginManager.loginManager().getLoginHandler()
                                .initiateLogin(LoginManager.loginManager()
                                        .getLoginFrame()));
            }, waitFor.getSeconds(), TimeUnit.SECONDS);

            if (!SwingUtils.clickButton(window, "OK")) {
                Utils.logError("could not dismiss \"Too many failed login"
                        + " attempts\" dialog because we could not find one of"
                        + " the controls.");
            }
        }
    }

    /**
     * Identifies the "Too many failed login attempts" dialog by checking the
     * window type and presence of the lockout message text area.
     *
     * @param window the window to check
     * @return true if the window is a JDialog with the lockout message;
     *     false otherwise
     */
    @Override
    public boolean recogniseWindow(final Window window) {
        if (!(window instanceof JDialog)) {
            return false;
        }

        return SwingUtils.findTextArea(window, "Too many failed login attempts")
                != null;
    }

}
