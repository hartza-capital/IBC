// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for Utils helpers that do not require a running TWS.

package ibcalpha.ibc;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void stringToHexConvertsCharacters() {
        assertEquals("4142", Utils.stringToHex("AB"));
        assertEquals("30", Utils.stringToHex("0"));
    }

    @Test
    void formatDateUsesExpectedPattern() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 21, 14, 30, 15, 123_000_000);
        String formatted = Utils.formatDate(date);
        assertTrue(formatted.contains("2026-07-21 14:30:15"));
    }

    @Test
    void pauseDoesNotThrow() {
        long start = System.currentTimeMillis();
        Utils.pause(10);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 5);
    }
}
