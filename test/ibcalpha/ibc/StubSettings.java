// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Test-only implementation of the Settings abstraction.

package ibcalpha.ibc;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable in-memory test double for {@link Settings}: values are stored as
 * strings and read back verbatim, and an absent or empty value yields the
 * caller's default.
 */
final class StubSettings extends Settings {

    /** Stored values keyed by property name. */
    private final Map<String, String> values = new HashMap<>();

    /**
     * Stores a string value under the given key.
     *
     * @param key the property key
     * @param value the value to store
     */
    void set(String key, String value) {
        values.put(key, value);
    }

    /**
     * Stores a boolean value under the given key as "true" or "false".
     *
     * @param key the property key
     * @param value the value to store
     */
    void set(String key, boolean value) {
        values.put(key, Boolean.toString(value));
    }

    /**
     * Does nothing; this double writes no diagnostic output.
     */
    @Override
    public void logDiagnosticMessage() {
        // no-op for tests
    }

    /**
     * Returns the stored value as a boolean, recognising true/yes and
     * false/no case-insensitively.
     *
     * @param key the property key
     * @param defaultValue the value returned when the key is absent or empty, or the value is none of those words
     * @return the boolean value of the stored setting, or defaultValue
     */
    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) return true;
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) return false;
        return defaultValue;
    }

    /**
     * Returns the first character of the stored value.
     *
     * @param key the property key
     * @param defaultValue the string whose first character is returned when the key is absent or empty
     * @return the first character of the stored value, or the first character of defaultValue
     */
    @Override
    public char getChar(String key, String defaultValue) {
        String value = values.getOrDefault(key, defaultValue);
        if (value.isEmpty()) return defaultValue.charAt(0);
        return value.charAt(0);
    }

    /**
     * Returns the stored value parsed as a double.
     *
     * @param key the property key
     * @param defaultValue the value returned when the key is absent or empty, or the value does not parse
     * @return the stored value as a double, or defaultValue
     */
    @Override
    public double getDouble(String key, double defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the stored value parsed as an int.
     *
     * @param key the property key
     * @param defaultValue the value returned when the key is absent or empty, or the value does not parse
     * @return the stored value as an int, or defaultValue
     */
    @Override
    public int getInt(String key, int defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the stored value.
     *
     * @param key the property key
     * @param defaultValue the value returned when the key is absent or empty
     * @return the stored value, or defaultValue
     */
    @Override
    public String getString(String key, String defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        return value;
    }
}
