// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for AbstractLoginHandler credential helpers.

package ibcalpha.ibc;

import java.awt.Window;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractLoginHandlerTest {

    @Test
    void setPasswordCredentialPrefersJPasswordField() throws IbcException {
        JPanel panel = createPanelWithComponents(new JPasswordField(20));
        JPasswordField passwordField = (JPasswordField) panel.getComponent(0);
        char[] password = "secret".toCharArray();

        new TestLoginHandler().setPasswordCredential(panel, "password", 0, password);

        assertEquals("secret", new String(passwordField.getPassword()));
        assertPasswordZeroed(password);
    }

    @Test
    void setPasswordCredentialFallsBackToJTextField() throws IbcException {
        JPanel panel = createPanelWithComponents(new JTextField(20), new JTextField(20));
        JTextField textField = (JTextField) panel.getComponent(1);
        char[] password = "fallback".toCharArray();

        new TestLoginHandler().setPasswordCredential(panel, "password", 1, password);

        assertEquals("fallback", textField.getText());
        assertPasswordZeroed(password);
    }

    @Test
    void setPasswordCredentialZerosArrayWhenFieldMissing() {
        JPanel panel = createPanelWithComponents(new JTextField(20));
        char[] password = "secret".toCharArray();

        assertThrows(IbcException.class, () ->
                new TestLoginHandler().setPasswordCredential(panel, "password", 1, password));

        assertPasswordZeroed(password);
    }

    private static JPanel createPanelWithComponents(java.awt.Component... components) {
        JPanel panel = new JPanel();
        for (java.awt.Component component : components) {
            panel.add(component);
        }
        return panel;
    }

    private static void assertPasswordZeroed(char[] password) {
        for (char character : password) {
            assertEquals('\0', character);
        }
    }

    private static final class TestLoginHandler extends AbstractLoginHandler {

        @Override
        public boolean recogniseWindow(Window window) {
            return true;
        }

        @Override
        protected boolean initialise(Window window, int eventID) {
            return true;
        }

        @Override
        protected boolean preLogin(Window window, int eventID) {
            return true;
        }

        @Override
        protected boolean setFields(Window window, int eventID) {
            return true;
        }

        @Override
        protected boolean isUserIdDisabledOrAbsent(Window window) {
            return false;
        }

        @Override
        protected boolean isPasswordDisabledOrAbsent(Window window) {
            return false;
        }
    }
}
