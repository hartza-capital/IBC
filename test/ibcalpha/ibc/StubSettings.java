// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Test-only implementation of the Settings abstraction.

package ibcalpha.ibc;

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable in-memory Settings implementation for unit tests.
 */
final class StubSettings extends Settings {

    private final Map<String, String> values = new HashMap<>();

    void set(String key, String value) {
        values.put(key, value);
    }

    void set(String key, boolean value) {
        values.put(key, Boolean.toString(value));
    }

    void set(String key, int value) {
        values.put(key, Integer.toString(value));
    }

    @Override
    public void logDiagnosticMessage() {
        // no-op for tests
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) return true;
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) return false;
        return defaultValue;
    }

    @Override
    public char getChar(String key, String defaultValue) {
        String value = values.getOrDefault(key, defaultValue);
        if (value.isEmpty()) return defaultValue.charAt(0);
        return value.charAt(0);
    }

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

    @Override
    public String getString(String key, String defaultValue) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) return defaultValue;
        return value;
    }
}
