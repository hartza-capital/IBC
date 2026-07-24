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

public abstract class Settings {

    private static Settings instance;

    /**
     * Sets the global Settings instance to the specified implementation.
     *
     * @param settings the Settings instance to use; must not be null
     * @throws IllegalArgumentException if settings is null
     */
    public static void initialise(final Settings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings");
        }
        instance = settings;
    }

    /**
     * Resets the global Settings instance to a new DefaultSettings.
     */
    public static void setDefault() {
        instance = new DefaultSettings();
    }

    /**
     * Returns the current global Settings instance.
     *
     * @return the active Settings instance
     */
    public static Settings settings() {
        return instance;
    }

    /**
     * Logs diagnostic information about the current settings configuration
     * for troubleshooting.
     */
    public abstract void logDiagnosticMessage();


    /**
     * Returns the boolean value associated with property named key.
     * Returns defaultValue if there is no such property,
     * or if the property value cannot be converted to a boolean.
     *
     * @param key the property key
     * @param defaultValue the default value to return if the property is
     *     not found or invalid
     * @return the boolean value of the property, or defaultValue
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
     * Returns the double value associated with property named key.
     * Returns defaultValue if there is no such property,
     * or if the property value cannot be converted to a double.
     *
     * @param key the property key
     * @param defaultValue the default value to return if the property is
     *     not found or invalid
     * @return the double value of the property, or defaultValue
     */
    public abstract double getDouble(String key, double defaultValue);

    /**
     * Returns the int value associated with property named key.
     * Returns defaultValue if there is no such property,
     * or if the property value cannot be converted to an int.
     *
     * @param key the property key
     * @param defaultValue the default value to return if the property is
     *     not found or invalid
     * @return the int value of the property, or defaultValue
     */
    public abstract int getInt(String key, int defaultValue);

    /**
     * Returns the value associated with property named key.
     * Returns defaultValue if no such property.
     *
     * @param key the property key
     * @param defaultValue the default value to return if property not found
     * @return the value of the property, or defaultValue
     */
    public abstract String getString(String key, String defaultValue);

}
