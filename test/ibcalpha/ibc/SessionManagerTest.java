// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for SessionManager static session state.

package ibcalpha.ibc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class SessionManagerTest {

    @BeforeEach
    void reset() {
        Settings.initialise(new StubSettings());
        MainWindowManager.initialise(new StubMainWindowManager());
    }

    @AfterEach
    void restoreDefaults() {
        SessionManager.initialise(false);
        TestUtils.setStaticField(SessionManager.class, "isFIXFlag", false);
        TestUtils.setStaticField(SessionManager.class, "isSessionStartedFlag", false);
        TestUtils.setStaticField(SessionManager.class, "initialisationCompleted", false);
        MainWindowManager.setDefault();
        Settings.setDefault();
    }

    @Test
    void isGatewayReflectsInitialiseValue() {
        SessionManager.initialise(true);
        assertTrue(SessionManager.isGateway());
        SessionManager.initialise(false);
        assertFalse(SessionManager.isGateway());
    }

    @Test
    void isFixReturnsFalseWhenSessionStartedForTws() {
        SessionManager.initialise(false);
        TestUtils.setStaticField(SessionManager.class, "isFIXFlag", false);
        TestUtils.setStaticField(SessionManager.class, "isSessionStartedFlag", true);
        assertFalse(SessionManager.isFIX());
    }

    @Test
    void isFixReturnsTrueWhenSessionStartedForFix() {
        SessionManager.initialise(false);
        TestUtils.setStaticField(SessionManager.class, "isFIXFlag", true);
        TestUtils.setStaticField(SessionManager.class, "isSessionStartedFlag", true);
        assertTrue(SessionManager.isFIX());
    }

    @Test
    void isFixExitsWhenSessionNotStarted() {
        TestUtils.setStaticField(SessionManager.class, "isSessionStartedFlag", false);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            SessionManager.isFIX();
            utils.verify(() -> Utils.exitWithError(anyInt(), anyString()));
        }
    }

    @Test
    void isRestartExitsWhenSessionNotStarted() {
        TestUtils.setStaticField(SessionManager.class, "isSessionStartedFlag", false);
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            SessionManager.isRestart();
            utils.verify(() -> Utils.exitWithError(anyInt(), anyString()));
        }
    }

    @Test
    void setSplashScreenClosedCompletesForGateway() {
        SessionManager.initialise(true);
        TestUtils.setStaticField(SessionManager.class, "initialisationCompleted", false);
        SessionManager.setNonBrokerageAccountDialogClosed();
        SessionManager.setSplashScreenClosed();
        assertTrue((boolean) TestUtils.getStaticField(SessionManager.class, "initialisationCompleted"));
    }

    @Test
    void setMainWindowCompletesInitialisation() {
        SessionManager.initialise(false);
        TestUtils.setStaticField(SessionManager.class, "initialisationCompleted", false);
        SessionManager.setMainWindow(null);
        assertTrue((boolean) TestUtils.getStaticField(SessionManager.class, "initialisationCompleted"));
    }
}
