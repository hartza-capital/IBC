// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the TOTP passcode handling of the 2FA dialog handler.

package ibcalpha.ibc;

import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecondFactorAuthenticationDialogHandlerTest {

    /** A well-formed Base32 secret, usable as a TOTP key. */
    private static final String VALID_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private StubSettings settings;

    @BeforeEach
    void reset() {
        settings = new StubSettings();
        Settings.initialise(settings);
    }

    @AfterEach
    void restoreDefaults() {
        Settings.setDefault();
    }

    /**
     * Builds a stand-in for the 2FA dialog: a passcode field index 0, as in the
     * real dialog.
     *
     * @return a panel holding the passcode field
     */
    private Container passcodeDialog() {
        JPanel dialog = new JPanel();
        dialog.add(new JTextField());
        return dialog;
    }

    /**
     * Returns the passcode field of a stand-in dialog.
     *
     * @param dialog the stand-in dialog
     * @return the first text field of the dialog
     */
    private JTextField passcodeField(Container dialog) {
        return (JTextField) dialog.getComponent(0);
    }

    @Test
    void validSecretFillsSixDigitPasscode() {
        settings.set("Ib2FA", VALID_SECRET);
        Container dialog = passcodeDialog();

        assertTrue(SecondFactorAuthenticationDialogHandler.getInstance().fillTotpCode(dialog));
        assertTrue(Pattern.matches("\\d{6}", passcodeField(dialog).getText()));
    }

    @Test
    void invalidSecretLeavesPasscodeEmpty() {
        settings.set("Ib2FA", "1234");
        Container dialog = passcodeDialog();

        assertFalse(SecondFactorAuthenticationDialogHandler.getInstance().fillTotpCode(dialog));
        assertTrue(passcodeField(dialog).getText().isEmpty());
    }

    @Test
    void emptySecretLeavesPasscodeEmpty() {
        Container dialog = passcodeDialog();

        assertFalse(SecondFactorAuthenticationDialogHandler.getInstance().fillTotpCode(dialog));
        assertTrue(passcodeField(dialog).getText().isEmpty());
    }

    @Test
    void dialogWithoutPasscodeFieldIsReported() {
        settings.set("Ib2FA", VALID_SECRET);

        assertFalse(SecondFactorAuthenticationDialogHandler.getInstance().fillTotpCode(new JPanel()));
    }

    @Test
    void shippedConfigShipsNoTwoFactorSecret() throws Exception {
        // A shipped secret is either the user's own (never published) or a
        // placeholder; either way it is a real passcode attempt at first login,
        // so the distribution must leave Ib2FA empty.
        String ini = Files.readString(Path.of("resources/config.ini"), StandardCharsets.ISO_8859_1);
        for (String line : ini.split("\\R")) {
            if (line.startsWith("Ib2FA=")) {
                String value = line.substring("Ib2FA=".length()).trim();
                assertTrue(value.isEmpty(), "config.ini must ship Ib2FA empty, but found: " + value);
            }
        }
    }
}
