// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for SwingUtils helpers that can be exercised with lightweight components.

package ibcalpha.ibc;

import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingUtilsTest {

    @Test
    void findButtonMatchesByText() {
        JPanel panel = new JPanel();
        JButton ok = new JButton("OK");
        panel.add(ok);
        panel.add(new JButton("Cancel"));

        assertEquals(ok, SwingUtils.findButton(panel, "OK"));
        assertNull(SwingUtils.findButton(panel, "Missing"));
    }

    @Test
    void findButtonIsCaseInsensitive() {
        JPanel panel = new JPanel();
        panel.add(new JButton("Save"));

        assertNotNull(SwingUtils.findButton(panel, "SAVE"));
    }

    @Test
    void findCheckBoxMatchesByText() {
        JPanel panel = new JPanel();
        JCheckBox cb = new JCheckBox("Enable");
        panel.add(cb);

        assertEquals(cb, SwingUtils.findCheckBox(panel, "enable"));
        assertNull(SwingUtils.findCheckBox(panel, "Disable"));
    }

    @Test
    void findLabelMatchesSubstring() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("Welcome to IBC"));

        assertNotNull(SwingUtils.findLabel(panel, "welcome"));
        assertNull(SwingUtils.findLabel(panel, "goodbye"));
    }

    @Test
    void findTextFieldByIndex() {
        JPanel panel = new JPanel();
        JTextField first = new JTextField();
        JTextField second = new JTextField();
        panel.add(first);
        panel.add(second);

        assertEquals(first, SwingUtils.findTextField(panel, 0));
        assertEquals(second, SwingUtils.findTextField(panel, 1));
        assertNull(SwingUtils.findTextField(panel, 2));
    }

    @Test
    void findRadioButtonMatchesByText() {
        JPanel panel = new JPanel();
        JRadioButton rb = new JRadioButton("Paper");
        panel.add(rb);

        assertEquals(rb, SwingUtils.findRadioButton(panel, "paper"));
        assertNull(SwingUtils.findRadioButton(panel, "Live"));
    }

    @Test
    void findToggleButtonMatchesByText() {
        JPanel panel = new JPanel();
        JToggleButton tb = new JToggleButton("Toggle");
        panel.add(tb);

        assertEquals(tb, SwingUtils.findToggleButton(panel, "toggle"));
    }

    @Test
    void getLabelTextsJoinsLabels() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("One"));
        panel.add(new JLabel("Two"));

        String texts = SwingUtils.getLabelTexts(panel);
        assertTrue(texts.contains("One"));
        assertTrue(texts.contains("Two"));
    }

    @Test
    void setPasswordFieldPrefersJPasswordField() {
        JPanel panel = new JPanel();
        JPasswordField passwordField = new JPasswordField(20);
        JTextField textField = new JTextField(20);
        panel.add(textField);
        panel.add(passwordField);

        char[] password = "secret".toCharArray();
        assertTrue(SwingUtils.setPasswordField(panel, 1, password));

        assertEquals("secret", new String(passwordField.getPassword()));
        assertEquals("", textField.getText());
    }

    @Test
    void setPasswordFieldFallsBackToJTextField() {
        JPanel panel = new JPanel();
        JTextField firstField = new JTextField(20);
        JTextField secondField = new JTextField(20);
        panel.add(firstField);
        panel.add(secondField);

        char[] password = "fallback".toCharArray();
        assertTrue(SwingUtils.setPasswordField(panel, 1, password));

        assertEquals("fallback", secondField.getText());
    }

    @Test
    void setPasswordFieldReturnsFalseWhenNoFieldAtIndex() {
        JPanel panel = new JPanel();
        panel.add(new JTextField(20));

        assertFalse(SwingUtils.setPasswordField(panel, 2, "secret".toCharArray()));
    }

    @Test
    void windowEventToStringMapsKnownEvents() {
        assertEquals("Opened", SwingUtils.windowEventToString(WindowEvent.WINDOW_OPENED));
        assertEquals("Activated", SwingUtils.windowEventToString(WindowEvent.WINDOW_ACTIVATED));
        assertEquals("Closed", SwingUtils.windowEventToString(WindowEvent.WINDOW_CLOSED));
        assertEquals("???", SwingUtils.windowEventToString(-1));
    }
}
