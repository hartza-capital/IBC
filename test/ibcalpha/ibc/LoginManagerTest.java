// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for LoginManager state machine.

package ibcalpha.ibc;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginManagerTest {

    private StubSettings settings;
    private StubLoginManager loginManager;

    @BeforeEach
    void reset() {
        settings = new StubSettings();
        Settings.initialise(settings);
        TestUtils.setSessionState(false, false, true);
        loginManager = new StubLoginManager();
        LoginManager.initialise(loginManager);
        MainWindowManager.initialise(new StubMainWindowManager());
    }

    @AfterEach
    void restoreDefaults() {
        LoginManager.setDefault();
        TradingModeManager.setDefault();
        SessionManager.initialise(false);
        Settings.setDefault();
        MainWindowManager.setDefault();
    }

    @Test
    void initialStateIsLoggedOut() {
        assertEquals(LoginManager.LoginState.LOGGED_OUT, loginManager.getLoginState());
    }

    @Test
    void setLoginStateTransitions() {
        loginManager.setLoginState(LoginManager.LoginState.LOGGING_IN);
        assertEquals(LoginManager.LoginState.LOGGING_IN, loginManager.getLoginState());

        loginManager.setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);
        assertEquals(LoginManager.LoginState.TWO_FA_IN_PROGRESS, loginManager.getLoginState());

        loginManager.setLoginState(LoginManager.LoginState.LOGGED_IN);
        assertEquals(LoginManager.LoginState.LOGGED_IN, loginManager.getLoginState());
    }

    @Test
    void repeatedStateIsIgnored() {
        loginManager.setLoginState(LoginManager.LoginState.LOGGING_IN);
        loginManager.setLoginState(LoginManager.LoginState.LOGGING_IN);
        assertEquals(LoginManager.LoginState.LOGGING_IN, loginManager.getLoginState());
    }

    @Test
    void readonlyLoginRequiredFalseWhenDisabled() {
        settings.set("ReadOnlyLogin", false);
        assertFalse(loginManager.readonlyLoginRequired());
    }

    @Test
    void readonlyLoginRequiredTrueForTws() {
        settings.set("ReadOnlyLogin", true);
        TestUtils.setSessionState(false, false, true);
        assertTrue(loginManager.readonlyLoginRequired());
    }

    @Test
    void readonlyLoginRequiredFalseForGateway() {
        settings.set("ReadOnlyLogin", true);
        TestUtils.setSessionState(true, false, true);
        assertFalse(loginManager.readonlyLoginRequired());
    }

    @Test
    void secondFactorDialogClosedDoesNothingWhenLoginNeverStarted() {
        // LoginStartTime is null, so the method returns early.
        loginManager.secondFactorAuthenticationDialogClosed();
        assertEquals(LoginManager.LoginState.LOGGED_OUT, loginManager.getLoginState());
    }

    @Test
    void initialiseRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> LoginManager.initialise(null));
    }

    @Test
    void setDefaultRestoresGlobalInstance() {
        LoginManager.setDefault();
        assertTrue(LoginManager.loginManager() instanceof DefaultLoginManager);
    }

    @Test
    void twoFaInProgressRecordsLoginStartTime() throws Exception {
        loginManager.setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);

        Field field = LoginManager.class.getDeclaredField("loginStartTime");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<java.time.Instant> startTime = (AtomicReference<java.time.Instant>) field.get(loginManager);

        assertNotNull(startTime.get());
    }

    @Test
    void secondFactorDialogClosedClearsLoginStartTime() throws Exception {
        loginManager.setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);
        loginManager.secondFactorAuthenticationDialogClosed();

        Field field = LoginManager.class.getDeclaredField("loginStartTime");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<java.time.Instant> startTime = (AtomicReference<java.time.Instant>) field.get(loginManager);

        assertNull(startTime.get());
    }

    @Test
    void reloginPermittedPrefersNewSetting() throws Exception {
        settings.set("ReloginAfterSecondFactorAuthenticationTimeout", true);

        Method method = LoginManager.class.getDeclaredMethod("reloginPermitted");
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(loginManager));
    }

    @Test
    void reloginPermittedFallsBackToDeprecatedSetting() throws Exception {
        settings.set("ExitAfterSecondFactorAuthenticationTimeout", true);

        Method method = LoginManager.class.getDeclaredMethod("reloginPermitted");
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(loginManager));
    }

    private static final class StubLoginManager extends LoginManager {

        private AbstractLoginHandler handler;

        @Override
        public void logDiagnosticMessage() {
            // no-op
        }

        @Override
        public char[] FIXPassword() {
            return "fixpass".toCharArray();
        }

        @Override
        public String FIXUserName() {
            return "fixuser";
        }

        @Override
        public char[] IBAPIPassword() {
            return "apipass".toCharArray();
        }

        @Override
        public String IBAPIUserName() {
            return "apiuser";
        }

        @Override
        public AbstractLoginHandler getLoginHandler() {
            return handler;
        }

        @Override
        public void setLoginHandler(AbstractLoginHandler handler) {
            this.handler = handler;
        }
    }
}
