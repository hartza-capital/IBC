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

/**
 * Abstract source of IBC's configuration settings, backed by implementations such as
 * {@link DefaultSettings}.
 */
public abstract class Settings {

    /** The active Settings implementation. */
    private static Settings _settings;

    /**
     * Sets the global Settings instance to the specified implementation.
     *
     * @param settings the Settings instance to use; must not be null
     * @throws IllegalArgumentException if settings is null
     */
    public static void initialise(Settings settings){
        if (settings == null) throw new IllegalArgumentException("settings");
        _settings = settings;
    }

    /**
     * Resets the global Settings instance to a new DefaultSettings.
     */
    public static void setDefault() {
        _settings = new DefaultSettings();
    }

    /**
     * Returns the current global Settings instance.
     *
     * @return the active Settings instance
     */
    public static Settings settings() {
        return _settings;
    }

    /**
     * Logs diagnostic information about the current settings configuration
     * for troubleshooting.
     */
    public abstract void logDiagnosticMessage();


    /**
     * Returns the boolean value of the property named key.
     *
     * @param key the property key
     * @param defaultValue the value returned if the property does not exist or is not a valid boolean
     * @return the property's boolean value, or defaultValue
     */
    public abstract boolean getBoolean(String key, boolean defaultValue);

    /**
     * Returns the character value associated with the specified property key.
     * Returns the first character of defaultValue if the property does not
     * exist or is empty.
     *
     * @param key the property key
     * @param defaultValue the default value string (first character is used
     *     as the fallback)
     * @return the character value of the property, or the first character of
     *     defaultValue
     */
    public abstract char getChar(String key, String defaultValue);

    /**
     * Returns the double value of the property named key.
     *
     * @param key the property key
     * @param defaultValue the value returned if the property does not exist or is not a valid double
     * @return the property's double value, or defaultValue
     */
    public abstract double getDouble(String key, double defaultValue);

    /**
     * Returns the int value of the property named key.
     *
     * @param key the property key
     * @param defaultValue the value returned if the property does not exist or is not a valid int
     * @return the property's int value, or defaultValue
     */
    public abstract int getInt(String key, int defaultValue);

    /**
     * Returns the value of the property named key.
     *
     * @param key the property key
     * @param defaultValue the value returned if the property does not exist
     * @return the property's value, or defaultValue
     */
    public abstract String getString(String key, String defaultValue);

}
