// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Shared helpers for unit tests.

package ibcalpha.ibc;

import java.lang.reflect.Field;

/**
 * Static helpers shared by the test suite: session-state setup and reflective
 * access to static fields.
 */
final class TestUtils {

    /**
     * Prevents instantiation; the class holds static helpers only.
     */
    private TestUtils() {
        // utility class
    }

    /**
     * Initialises SessionManager and sets its FIX and session-started flags.
     *
     * @param isGateway true for a Gateway session, false for a TWS session
     * @param isFix true for a FIX protocol session
     * @param isSessionStarted true to mark the session as started
     */
    static void setSessionState(boolean isGateway, boolean isFix, boolean isSessionStarted) {
        SessionManager.initialise(isGateway);
        setStaticField(SessionManager.class, "_isFIX", isFix);
        setStaticField(SessionManager.class, "_isSessionStarted", isSessionStarted);
    }

    /**
     * Sets a private static field to the given value via reflection.
     *
     * @param clazz the class declaring the field
     * @param fieldName the field name
     * @param value the value to assign
     * @throws RuntimeException if the field cannot be found or assigned
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
     * @param clazz the class declaring the field
     * @param fieldName the field name
     * @return the field value, cast to the caller's expected type
     * @throws RuntimeException if the field cannot be found or read
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
