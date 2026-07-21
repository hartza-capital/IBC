// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2023 Richard L King (rlking@aultan.com)
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.io.File;
import static java.lang.Thread.sleep;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.JPanel;

class RestartTask
        implements Runnable {

    /** Number of seconds in a minute. */
    private static final int SECONDS_IN_MINUTE = 60;

    /** Number of minutes in an hour. */
    private static final int MINUTES_IN_HOUR = 60;

    /** Number of hours in a day. */
    private static final int HOURS_IN_DAY = 24;

    /** Last whole second that is safe to schedule the next restart. */
    private static final int LAST_SAFE_SECOND = 58;

    /** Opacity used for the restart countdown overlay. */
    private static final float OVERLAY_OPACITY = 0.80f;

    /** Width and height of the countdown overlay panel. */
    private static final int COUNTDOWN_SIZE = 250;

    /** Horizontal position of the countdown text. */
    private static final int COUNTDOWN_TEXT_X = 50;

    /** Vertical position of the countdown text. */
    private static final int COUNTDOWN_TEXT_Y = 300;

    /** Lock ensuring only one restart/pause runs at a time. */
    private static final SwitchLock RUNNING = new SwitchLock();

    /** Channel used to report progress back to the client. */
    private final CommandChannel mChannel;

    /** True when this task should pause rather than fully restart. */
    private final boolean mPauseOnly;

    /** Human-readable verb used in messages ("PAUSE" or "RESTART"). */
    private final String mVerb;

    /**
     * Creates a new restart task for the given command channel.
     *
     * @param channel the channel used to report progress back to the client
     * @param pauseOnly true to pause the session without requiring
     *        re-authentication on restart; false for a full restart
     */
    RestartTask(final CommandChannel channel,
                final boolean pauseOnly) {
        Utils.logToConsole("RestartTask: pauseOnly = " + pauseOnly);
        mChannel = channel;
        mPauseOnly = pauseOnly;
        mVerb = mPauseOnly ? "PAUSE" : "RESTART";
        Utils.logToConsole("RestartTask: verb = " + mVerb);
    }

    /**
     * Executes the restart or pause sequence.
     * If a restart is already in progress, sends an error message.
     * Otherwise, initiates the TWS/Gateway restart and schedules an
     * auto-restart time if the File menu method fails.
     */
    @Override
    public void run() {
        if (!RUNNING.set()) {
            Utils.logToConsole(mVerb + " already in progress");
            writeNack(mVerb + " already in progress");
            mChannel.close();
            return;
        }

        try {
            if (mPauseOnly) {
                writeInfo("Pausing TWS");
                createPauseFlagFile();
            } else {
                writeInfo("Restarting TWS");
            }
            writeAck(mVerb + " in progress");
            restart(mPauseOnly);
        } catch (Exception ex) {
            writeNack(ex.getMessage());
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, ex);
        }
    }

    /**
     * Creates a marker file that tells the startup script to preserve the
     * current session instead of performing a full re-authentication.
     */
    private void createPauseFlagFile() {
        try {
            String pauseFlagPath = System.getProperty("jtsConfigDir")
                    + File.separator
                    + "PAUSE"
                    + System.getProperty("ibcsessionid");
            new File(pauseFlagPath).createNewFile();
        } catch (java.io.IOException e) {
            Utils.exitWithException(ErrorCodes.UNHANDLED_EXCEPTION, e);
        }
    }

    /**
     * Restarts or pauses TWS/Gateway.
     * First tries to invoke the File | Restart menu item; if that fails,
     * schedules an auto-restart one minute in the future and starts a
     * countdown overlay.
     *
     * @param pauseOnly true when pausing, false when restarting
     */
    void restart(final boolean pauseOnly) {
        if (Utils.invokeMenuItem(
                MainWindowManager.mainWindowManager().getMainWindow(),
                new String[] {"File", "Restart..."})) {
            mChannel.close();
            return;
        }

        while (LocalTime.now().getSecond() >= LAST_SAFE_SECOND) {
            try {
                sleep(1);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        LocalTime now = LocalTime.now();

        int newHour = now.getHour();
        int newMinute = now.getMinute() + 1;
        if (newMinute > MINUTES_IN_HOUR - 1) {
            newMinute = newMinute - MINUTES_IN_HOUR;
            newHour = newHour + 1;
        }
        if (newHour > HOURS_IN_DAY - 1) {
            newHour = newHour - HOURS_IN_DAY;
        }
        LocalTime actionTime = now.withHour(newHour)
                                  .withMinute(newMinute)
                                  .withSecond(0);

        Utils.logToConsole("Setting auto-restart time to "
                + actionTime.format(
                        DateTimeFormatter.ofPattern("hh:mm a")));
        (new ConfigurationTask(new ConfigureAutoLogoffOrRestartTimeTask(
                                        "Auto restart",
                                        actionTime)
                                )
        ).executeAsync();

        writeAck(mVerb + " at "
                + actionTime.format(
                        DateTimeFormatter.ofPattern("hh:mm a")));
        mChannel.close();

        try {
            if (!GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .isWindowTranslucencySupported(TRANSLUCENT)) {
                return;
            }
        } catch (IllegalComponentStateException e) {
            return;
        }

        JFrame window = MainWindowManager.mainWindowManager().getMainWindow();
        window.setOpacity(OVERLAY_OPACITY);

        RestartTask.Countdown countdown = new RestartTask.Countdown(
                actionTime.isBefore(LocalTime.now())
                    ? actionTime.atDate(LocalDate.now().plusDays(1))
                    : actionTime.atDate(LocalDate.now()));
        window.setGlassPane(countdown);
        countdown.setVisible(true);
    }

    /**
     * Sends a success acknowledgment message to the connected client.
     *
     * @param message the acknowledgment message
     */
    private void writeAck(final String message) {
        if (mChannel != null) {
            mChannel.writeAck(message);
        }
    }

    /**
     * Sends an informational message to the connected client.
     *
     * @param message the informational message
     */
    private void writeInfo(final String message) {
        if (mChannel != null) {
            mChannel.writeInfo(message);
        }
    }

    /**
     * Sends an error message to the connected client.
     *
     * @param message the error message
     */
    private void writeNack(final String message) {
        if (mChannel != null) {
            mChannel.writeNack(message);
        }
    }

    /**
     * A panel component that displays a countdown timer until the scheduled
     * restart time. Updates once per second and displays the remaining time
     * in seconds.
     */
    private class Countdown extends JPanel {
        private static final long serialVersionUID = 1L;

        /** Font used to draw the countdown text. */
        private final Font font = new Font("Arial", Font.BOLD, 36);

        /** Target date and time for the scheduled restart. */
        private final LocalDateTime countdownTo;

        /** Remaining time until the scheduled restart. */
        private Duration secsRemaining;

        /**
         * Initializes a countdown panel for the specified target date-time.
         *
         * @param targetDateTime the target LocalDateTime when the restart
         *        should occur
         */
        Countdown(final LocalDateTime targetDateTime) {
            this.setSize(COUNTDOWN_SIZE, COUNTDOWN_SIZE);
            this.setOpaque(false);
            this.countdownTo = targetDateTime;
            MyScheduledExecutorService.getInstance().scheduleAtFixedRate(
                    () -> {
                        secsRemaining = Duration.between(
                                LocalDateTime.now(), this.countdownTo);
                        this.repaint();
                    },
                    0,
                    1,
                    TimeUnit.SECONDS);
        }

        /**
         * Paints the countdown display showing seconds remaining until the
         * restart time.
         *
         * @param g the Graphics object to paint on
         */
        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            g.setColor(Color.WHITE);
            g.setFont(font);
            if (secsRemaining.isNegative()) {
                g.drawString(mVerb + " in progress",
                        COUNTDOWN_TEXT_X, COUNTDOWN_TEXT_Y);
            } else {
                g.drawString(mVerb + " in "
                        + secsRemaining.getSeconds() + " seconds",
                        COUNTDOWN_TEXT_X, COUNTDOWN_TEXT_Y);
            }
        }
    }
}
