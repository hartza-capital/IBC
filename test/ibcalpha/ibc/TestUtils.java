// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Shared helpers for unit tests.

package ibcalpha.ibc;

import java.lang.reflect.Field;

/**
 * Utility methods used across the IBC test suite.
 */
final class TestUtils {

    private TestUtils() {
        // utility class
    }

    /**
     * Resets the SessionManager static state used by JtsIniManager and other tests.
     *
     * @param isGateway true for Gateway session, false for TWS
     * @param isFix true for FIX protocol session
     * @param isSessionStarted true to mark the session as started
     */
    static void setSessionState(boolean isGateway, boolean isFix, boolean isSessionStarted) {
        SessionManager.initialise(isGateway);
        setStaticField(SessionManager.class, "isFIXFlag", isFix);
        setStaticField(SessionManager.class, "isSessionStartedFlag", isSessionStarted);
    }

    /**
     * Sets a private static field via reflection.
     *
     * @param clazz the declaring class
     * @param fieldName the field name
     * @param value the new value
     */
    static void setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a private static field via reflection.
     *
     * @param clazz the declaring class
     * @param fieldName the field name
     * @return the field value
     */
    @SuppressWarnings("unchecked")
    static <T> T getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
