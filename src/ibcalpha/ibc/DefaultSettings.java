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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DefaultSettings extends Settings {

    private final Properties props = new Properties();
    private String configPath;

    /**
     * Creates a new DefaultSettings instance that loads from the default
     * settings file path.
     */
    public DefaultSettings() {
        load(generateDefaultIniPath());
    }

    /**
     * Creates a new DefaultSettings instance that loads from a path
     * determined by command-line arguments.
     *
     * @param args the command-line arguments specifying the settings file
     *        path
     */
    public DefaultSettings(final String[] args) {
        load(getSettingsPath(args));
    }

    /**
     * Creates a new DefaultSettings instance that loads from the specified
     * path.
     *
     * @param path the path to the settings file
     */
    public DefaultSettings(final String path) {
        load(path);
    }

    /**
     * Loads settings from the specified file path into the properties
     * object.
     *
     * @param path the path to the settings file
     */
    private void load(final String path) {
        this.configPath = path;
        props.clear();
        try {
            File f = new File(path);
            InputStream is = new BufferedInputStream(new FileInputStream(f));
            props.load(is);
            is.close();

            Utils.logRawToConsole("IBC Settings:");
            Object[] keys = props.stringPropertyNames().toArray();
            java.util.Arrays.sort(keys);
            for (Object key : keys) {
                Utils.logRawToConsole(
                        "    " + key + "="
                                + getSettingSanitisedValue(key.toString()));
            }
            Utils.logRawToConsole("End IBC Settings\n");
        } catch (FileNotFoundException e) {
            Utils.logToConsole("Properties file " + path + " not found");
        } catch (IOException e) {
            Utils.logToConsole(
                    "Exception accessing Properties file " + path);
            Utils.logToConsole(e.toString());
        }
    }

    /**
     * Returns a sanitized value for a settings property, masking sensitive
     * credentials.
     *
     * @param key the property key
     * @return the sanitized value, with credentials masked as "***"
     */
    private String getSettingSanitisedValue(final String key) {
        String value = props.getProperty(key);
        if (key.equalsIgnoreCase("FIXLoginId")
                || key.equalsIgnoreCase("FIXPassword")
                || key.equalsIgnoreCase("IbLoginId")
                || key.equalsIgnoreCase("IbPassword")) {
            if (!value.isEmpty()) {
                return "***";
            }
        }
        return value;
    }

    /**
     * Generates the default settings file path based on the operating system
     * and user home directory.
     *
     * @return the default path to the settings file
     */
    static String generateDefaultIniPath() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return System.getenv("HOMEDRIVE")
                    + System.getenv("HOMEPATH") + File.separator
                    + "Documents" + File.separator
                    + "IBC" + File.separator
                    + "config.ini";
        } else {
            return System.getProperty("user.home") + File.separator
                    + "ibc" + File.separator
                    + "config.ini";
        }
    }

    /**
     * Determines the settings file path from command-line arguments or
     * defaults.
     *
     * @param args the command-line arguments
     * @return the path to the settings file
     */
    static String getSettingsPath(final String[] args) {
        String iniPath;
        if (args.length == 0 || args[0].equalsIgnoreCase("NULL")) {
            iniPath = getWorkingDirectory()
                    + "config."
                    + getComputerUserName()
                    + ".ini";
        } else if (args[0].length() == 0) {
            iniPath = generateDefaultIniPath();
        } else { // args.length >= 1
            iniPath = args[0];
        }
        File finiPath = new File(iniPath);
        if (!finiPath.isFile() || !finiPath.exists()) {
            Utils.exitWithError(ErrorCodes.INI_FILE_DOES_NOT_EXIST,
                    "ini file \"" + iniPath
                    + "\" either does not exist, or is a directory. "
                    + "quitting...");
        }
        return iniPath;
    }

    /**
     * Returns a sanitized version of the computer user name for use in
     * file paths.
     *
     * @return the sanitized user name with special characters replaced by
     * underscores
     */
    private static String getComputerUserName() {
        StringBuilder sb = new StringBuilder(System.getProperty("user.name"));
        int i;
        for (i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                sb.setCharAt(i, Character.toLowerCase(c));
            } else {
                sb.setCharAt(i, '_');
            }
        }
        return sb.toString();
    }

    /**
     * Returns the current working directory with a trailing file separator.
     *
     * @return the working directory
     */
    private static String getWorkingDirectory() {
        return System.getProperty("user.dir") + File.separator;
    }

    /**
     * Logs a diagnostic message indicating the settings file path in use.
     */
    @Override
    public void logDiagnosticMessage() {
        Utils.logToConsole(
                "using default settings provider: ini file is "
                + configPath);
    }

    /**
     * Returns the string value associated with the specified property key.
     *
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * or is empty
     * @return the property value, or defaultValue if not found
     */
    @Override
    public String getString(final String key,
                            final String defaultValue) {
        String value = props.getProperty(key, defaultValue);

        // handle key=[empty string] in .ini file
        if (value.isEmpty()) {
            value = defaultValue;
        }
        return value.trim();
    }

    /**
     * Returns the integer value associated with the specified property key.
     *
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * or cannot be parsed as an integer
     * @return the property value as an integer, or defaultValue if not found
     * or invalid
     */
    @Override
    public int getInt(final String key,
                      final int defaultValue) {
        String value = props.getProperty(key);

        // handle key missing or key=[empty string] in .ini file
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Utils.logToConsole(
                    "Invalid number \""
                    + value
                    + "\" for property \""
                    + key
                    + "\"");
            return defaultValue;
        }
    }

    /**
     * Returns the character value associated with the specified property
     * key.
     *
     * @param key the property key
     * @param defaultValue the default value to return if the property is not
     * found; the first character is used
     * @return the first character of the property value, or the first
     * character of defaultValue if not found
     */
    @Override
    public char getChar(final String key,
                        final String defaultValue) {
        String value = props.getProperty(key, defaultValue);

        // handle key missing or key=[empty string] in .ini file
        if (value == null || value.length() == 0) {
            return defaultValue.charAt(0);
        }

        if (value.length() != 1) {
            Utils.logToConsole(
                    "Invalid character \""
                    + value
                    + "\" for property \""
                    + key
                    + "\"");
        }

        return value.charAt(0);
    }

    /**
     * Returns the double value associated with the specified property key.
     *
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * or cannot be parsed as a double
     * @return the property value as a double, or defaultValue if not found
     * or invalid
     */
    @Override
    public double getDouble(final String key,
                            final double defaultValue) {
        String value = props.getProperty(key);

        // handle key missing or key=[empty string] in .ini file
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            Utils.logToConsole(
                    "Invalid number \""
                    + value
                    + "\" for property \""
                    + key
                    + "\"");
            return defaultValue;
        }
    }

    /**
     * Returns the boolean value associated with the specified property key.
     *
     * @param key the property key
     * @param defaultValue the value to return if the property is not found
     * or cannot be parsed as a boolean
     * @return the property value as a boolean, or defaultValue if not found
     * or invalid
     */
    @Override
    public boolean getBoolean(final String key,
                              final boolean defaultValue) {
        String value = props.getProperty(key);

        // handle key missing or key=[empty string] in .ini file
        if (value == null || value.length() == 0) {
            return defaultValue;
        }

        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("yes")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else if (value.equalsIgnoreCase("no")) {
            return false;
        } else {
            return defaultValue;
        }
    }

}
