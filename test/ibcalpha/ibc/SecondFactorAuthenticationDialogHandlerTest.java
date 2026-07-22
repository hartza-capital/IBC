// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for SecondFactorAuthenticationDialogHandler TOTP validation.

package ibcalpha.ibc;

import java.awt.Container;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondFactorAuthenticationDialogHandlerTest {

    private StubSettings settings;

    @BeforeEach
    void reset() {
        settings = new StubSettings();
        Settings.initialise(settings);
        TestUtils.setSessionState(false, false, false);
        LoginManager.initialise(new StubLoginManager());
    }

    @AfterEach
    void restoreDefaults() {
        LoginManager.setDefault();
        Settings.setDefault();
        SessionManager.initialise(false);
    }

    @Test
    void doValidate2FASubmitsValidTotp() throws Exception {
        settings.set("Ib2FA", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");

        JPanel panel = createSecondFactorPanel();
        JTextField codeField = (JTextField) panel.getComponent(0);
        AtomicBoolean clicked = new AtomicBoolean(false);
        JButton okButton = (JButton) panel.getComponent(1);
        okButton.addActionListener(event -> clicked.set(true));

        invokeDoValidate2FA(panel);

        String code = codeField.getText();
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"), "Expected 6-digit TOTP but got: " + code);

        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(clicked.get(), "OK button should be clicked for a valid TOTP");
    }

    @Test
    void doValidate2FADoesNotSubmitInvalidTotp() throws Exception {
        settings.set("Ib2FA", "not-a-valid-base32-secret");

        JPanel panel = createSecondFactorPanel();
        JTextField codeField = (JTextField) panel.getComponent(0);
        AtomicBoolean clicked = new AtomicBoolean(false);
        JButton okButton = (JButton) panel.getComponent(1);
        okButton.addActionListener(event -> clicked.set(true));

        invokeDoValidate2FA(panel);

        assertEquals("", codeField.getText());

        SwingUtilities.invokeAndWait(() -> { });
        assertFalse(clicked.get(), "OK button must not be clicked when TOTP generation fails");
    }

    private static JPanel createSecondFactorPanel() {
        JPanel panel = new JPanel();
        panel.add(new JTextField(20));
        panel.add(new JButton("OK"));
        return panel;
    }

    private static void invokeDoValidate2FA(Container container) throws Exception {
        SecondFactorAuthenticationDialogHandler handler = SecondFactorAuthenticationDialogHandler.getInstance();
        Method method = SecondFactorAuthenticationDialogHandler.class.getDeclaredMethod("doValidate2FA", Container.class);
        method.setAccessible(true);
        method.invoke(handler, container);
    }

    private static final class StubLoginManager extends LoginManager {

        private AbstractLoginHandler handler;

        @Override
        public void logDiagnosticMessage() {
            // no-op
        }

        @Override
        public char[] FIXPassword() {
            return new char[0];
        }

        @Override
        public String FIXUserName() {
            return "";
        }

        @Override
        public char[] IBAPIPassword() {
            return new char[0];
        }

        @Override
        public String IBAPIUserName() {
            return "";
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
