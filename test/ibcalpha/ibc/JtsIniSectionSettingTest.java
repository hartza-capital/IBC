// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for JtsIniSectionSetting.

package ibcalpha.ibc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JtsIniSectionSettingTest {

    @Test
    void storesSectionAndSetting() {
        JtsIniSectionSetting setting = new JtsIniSectionSetting("[Logon]", "Locale=en");
        assertEquals("[Logon]", setting.getSection());
        assertEquals("Locale=en", setting.getSetting());
        assertFalse(setting.isProcessed());
    }

    @Test
    void tracksProcessedFlag() {
        JtsIniSectionSetting setting = new JtsIniSectionSetting("[Logon]", "Locale=en");

        setting.setProcessed(true);
        assertTrue(setting.isProcessed());

        setting.setProcessed(false);
        assertFalse(setting.isProcessed());
    }
}
