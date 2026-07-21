// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for JtsIniSectionSetting.

package ibcalpha.ibc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JtsIniSectionSettingTest {

    @Test
    void storesSectionAndSetting() {
        JtsIniSectionSetting setting = new JtsIniSectionSetting("[Logon]", "Locale=en");
        assertEquals("[Logon]", setting.section);
        assertEquals("Locale=en", setting.setting);
        assertFalse(setting.isProcessed);
    }
}
