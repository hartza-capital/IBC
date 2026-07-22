// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the in-tree TOTP/HOTP generator.

package ibcalpha.ibc;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpTest {

    // The shared secret used by the RFC 4226 Appendix D reference vectors: the ASCII
    // string "12345678901234567890".
    private static final byte[] RFC4226_KEY =
            "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    // The same secret, Base32-encoded, for exercising the Base32-decoding entry point.
    private static final String RFC4226_KEY_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    // The ten published 6-digit HOTP values for counters 0 to 9 (RFC 4226 Appendix D).
    // These are known-answer vectors: they pin the dynamic-truncation arithmetic, which
    // is the part of RFC 4226 that is easy to get subtly wrong.
    private static final String[] RFC4226_HOTP_CODES = {
        "755224", "287082", "359152", "969429", "338314",
        "254676", "287922", "162583", "399871", "520489"
    };

    @Test
    void generateHotpMatchesRfc4226ReferenceVectors() throws Exception {
        for (int counter = 0; counter < RFC4226_HOTP_CODES.length; counter++) {
            assertEquals(RFC4226_HOTP_CODES[counter],
                         Totp.generateHotp(RFC4226_KEY, counter, 6),
                         "HOTP mismatch for counter " + counter);
        }
    }

    @Test
    void generateHotpHonoursRequestedDigitCount() throws Exception {
        // The counter-0 binary code is 1284755224, so each digit count keeps that many
        // low-order digits: this confirms the modulo uses the requested width, not a
        // hard-coded six.
        assertEquals("4755224", Totp.generateHotp(RFC4226_KEY, 0, 7));
        assertEquals("755224", Totp.generateHotp(RFC4226_KEY, 0, 6));
        assertEquals("5224", Totp.generateHotp(RFC4226_KEY, 0, 4));
    }

    @Test
    void generateHotpZeroPadsCodesShorterThanRequestedWidth() throws Exception {
        // Counter 30 truncates to 26920, i.e. only five significant digits. IBKR expects a
        // fixed-width code, so the leading zero must be preserved rather than dropped.
        assertEquals("026920", Totp.generateHotp(RFC4226_KEY, 30, 6));
    }

    @Test
    void generateHotpRejectsUnsupportedDigitCounts() {
        assertThrows(IllegalArgumentException.class,
                     () -> Totp.generateHotp(RFC4226_KEY, 0, -1));
        assertThrows(IllegalArgumentException.class,
                     () -> Totp.generateHotp(RFC4226_KEY, 0, 8));
    }

    @Test
    void generateDecodesBase32SecretAndMatchesReferenceVectors() throws Exception {
        // Same vectors as above, but reached through the Base32 entry point actually used
        // by SecondFactorAuthenticationDialogHandler.
        for (int counter = 0; counter < RFC4226_HOTP_CODES.length; counter++) {
            assertEquals(RFC4226_HOTP_CODES[counter],
                         Totp.generate(RFC4226_KEY_BASE32, counter),
                         "TOTP mismatch for time step " + counter);
        }
    }

    @Test
    void generateAlwaysProducesSixDigits() throws Exception {
        // A large time step, comparable to a real epoch/30 value.
        String code = Totp.generate(RFC4226_KEY_BASE32, System.currentTimeMillis() / 30000);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"), "expected six decimal digits but got " + code);
    }

    @Test
    void generateToleratesLowercaseAndSpacedSecrets() throws Exception {
        // Authenticator apps commonly display secrets lowercase and in space-separated
        // groups; pasting one into config.ini must still work.
        assertEquals(Totp.generate(RFC4226_KEY_BASE32, 0),
                     Totp.generate("gezdgnbv gy3tqojq gezdgnbv gy3tqojq", 0));
    }

    @Test
    void generateRejectsInvalidBase32Secret() {
        assertThrows(IllegalArgumentException.class, () -> Totp.generate("not-valid-base32!", 0));
        assertThrows(IllegalArgumentException.class, () -> Totp.generate("", 0));
        assertThrows(IllegalArgumentException.class, () -> Totp.generate(null, 0));
    }
}
