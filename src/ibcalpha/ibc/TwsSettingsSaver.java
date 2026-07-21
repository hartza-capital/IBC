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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TwsSettingsSaver {
    /**
     * Singleton instance responsible for scheduling automatic TWS/Gateway settings saves.
     */
    private static final TwsSettingsSaver instance = new TwsSettingsSaver();
    /**
     * Formats and parses the hh:mm times configured for automatic settings saving.
     */
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm");


    /**
     * Private constructor prevents external instantiation of this singleton scheduler.
     */
    private TwsSettingsSaver() {};

    static TwsSettingsSaver getInstance() {return instance;}

    /**
     * Initializes automatic TWS settings saving based on the SaveTwsSettingsAt configuration.
     *
     * Configuration format: SaveTwsSettingsAt=hh:mm [hh:mm]... or SaveTwsSettingsAt=Every n [{mins | hours}] [hh:mm [hh:mm]]
     */
    public void initialise() {
        // setting format: SaveTwsSettingsAt=hh:mm [hh:mm]...
        //             or: SaveTwsSettingsAt=Every n [{mins | hours}] [hh:mm [hh:mm]]
        String timesSetting = Settings.settings().getString("SaveTwsSettingsAt", "");
        if (timesSetting.length() == 0) return;

        String[] times = timesSetting.split("[ ]+");

        List<Date> saveTimes;

        try {
            if (!times[0].equalsIgnoreCase("Every")){
                saveTimes = convertSuppliedTimes(times);
            } else {
                saveTimes = generateSaveTimes(times);
            }

            for (Date c : saveTimes) {
                scheduleSave(c);
            }

        } catch (IbcException e) {
            Utils.logError("Invalid setting SaveTwsSettingsAt=" + timesSetting + ": " + e.getMessage() + "\nTWS Settings will not be saved automatically");
        }

    }

    /**
     * Adjusts a calendar to the next day if the current time has already passed.
     *
     * @param calendar the calendar to adjust
     * @return an adjusted calendar set to tomorrow if the original time is in the past, otherwise the original calendar
     */
    private static Calendar adjustCalendar(Calendar calendar) {
        Calendar cal = (Calendar)calendar.clone();
        if (!cal.getTime().after(new Date())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return cal;
    }

    /**
     * Converts an array of explicit time strings (hh:mm format) into a list of future save times.
     *
     * @param times the array of time strings in hh:mm format
     * @return a list of Date objects representing the save times
     * @throws IbcException if any time string is not in valid hh:mm format
     */
    private static List<Date> convertSuppliedTimes(String[] times) throws IbcException {
        List<Date> saveTimes = new ArrayList<>();
        for (String time : times) {
            saveTimes.add(adjustCalendar(getCalendarForTime(time)).getTime());
        }
        return saveTimes;
    }

    /**
     * Generates a list of save times at regular intervals, optionally within a time range.
     *
     * @param times an array starting with "Every", followed by interval count, optional unit (mins/hours), optional start and end times
     * @return a list of Date objects representing the generated save times
     * @throws IbcException if the interval is not a valid integer or falls outside the 1-1439 minute range
     */
    private static List<Date> generateSaveTimes(String[] times) throws IbcException {
        int interval = 0;
        try {
            interval = Integer.parseInt(times[1]);
        } catch (NumberFormatException e) {
            throw new IbcException("interval is '" + times[1] + "' but should be an integer");
        }

        int nextIndex = 2;

        if (times.length > 2) {
            if (times[2].equalsIgnoreCase("mins")) {
                nextIndex = 3;
            } else if (times[2].equalsIgnoreCase("hours")) {
                nextIndex = 3;
                interval = 60 * interval;
            }
        }
        if (interval < 1 || interval > 1439) {
            throw new IbcException("the saving interval must be between 1 and 1439 minutes");
        }

        String startTime = "00:00";
        String endTime = "24:00";
        if (times.length > nextIndex) {
            startTime = times[nextIndex];
            if (times.length > nextIndex + 1) {
                endTime = times[nextIndex + 1];
            }
        }

        Calendar startCal = getCalendarForTime(startTime);
        Calendar endCal = getCalendarForTime(endTime);
        if (!startCal.before(endCal)) {
            endCal.add(Calendar.DAY_OF_MONTH, 1);
        }
  
        List<Date> saveTimes = new ArrayList<Date>();
        while (startCal.before(endCal)) {
            saveTimes.add(adjustCalendar(startCal).getTime());
            startCal.add(Calendar.MINUTE, interval);
        }
        saveTimes.add(adjustCalendar(endCal).getTime());

        return saveTimes;
    }

    /**
     * Creates a Calendar set to the specified time today.
     *
     * @param time a time string in hh:mm format
     * @return a Calendar object set to the specified time
     * @throws IbcException if the time string is not in valid hh:mm format
     */
    private static Calendar getCalendarForTime(String time) throws IbcException {
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(dateFormat.parse(time));
        } catch (ParseException e) {
            throw new IbcException("value '" + time + "' should be in hh:mm format");
        }
        int saveHour = cal.get(Calendar.HOUR_OF_DAY);
        int saveMinute = cal.get(Calendar.MINUTE);
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, saveHour);
        cal.set(Calendar.MINUTE, saveMinute);
        cal.set(Calendar.SECOND, 0);
        return cal;
    }

    /**
     * Schedules a recurring task to save TWS settings at the specified time every day.
     *
     * @param saveTime the time at which to save TWS settings
     */
    private static void scheduleSave(Date saveTime) {
        Utils.logToConsole("Tws settings will be saved at " + dateFormat.format(saveTime));

        MyScheduledExecutorService.getInstance().scheduleAtFixedRate(() -> {
            Utils.logToConsole("Saving Tws settings");
            Utils.invokeMenuItem(MainWindowManager.mainWindowManager().getMainWindow(), new String[] {"File", "Save Settings"});
        }, saveTime.getTime() - System.currentTimeMillis(), 86400000, TimeUnit.MILLISECONDS);
    }

}

