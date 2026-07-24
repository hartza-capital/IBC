// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for LoginManager state machine.

package ibcalpha.ibc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final class StubLoginManager extends LoginManager {

        private AbstractLoginHandler handler;

        @Override
        public void logDiagnosticMessage() {
            // no-op
        }

        @Override
        public String fixPassword() {
            return "fixpass";
        }

        @Override
        public String fixUserName() {
            return "fixuser";
        }

        @Override
        public String ibapiPassword() {
            return "apipass";
        }

        @Override
        public String ibapiUserName() {
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
