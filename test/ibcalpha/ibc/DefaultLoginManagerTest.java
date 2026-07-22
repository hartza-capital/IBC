// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for DefaultLoginManager credential handling.

package ibcalpha.ibc;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class DefaultLoginManagerTest {

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

    @Test
    void constructorStoresTwsPasswordAsCharArray() {
        DefaultLoginManager manager = new DefaultLoginManager("user", "secret");

        assertEquals("user", manager.IBAPIUserName());
        assertArrayEquals("secret".toCharArray(), manager.IBAPIPassword());
    }

    @Test
    void constructorStoresFixAndTwsPasswordsAsCharArrays() {
        DefaultLoginManager manager = new DefaultLoginManager("fixuser", "fixpass", "apiuser", "apipass");

        assertEquals("fixuser", manager.FIXUserName());
        assertArrayEquals("fixpass".toCharArray(), manager.FIXPassword());
        assertEquals("apiuser", manager.IBAPIUserName());
        assertArrayEquals("apipass".toCharArray(), manager.IBAPIPassword());
    }

    @Test
    void passwordFromConstructorIsClonedOnEachAccess() {
        DefaultLoginManager manager = new DefaultLoginManager("user", "secret");

        char[] firstPassword = manager.IBAPIPassword();
        char[] secondPassword = manager.IBAPIPassword();

        assertNotSame(firstPassword, secondPassword);
        assertArrayEquals(firstPassword, secondPassword);

        firstPassword[0] = 'X';
        assertArrayEquals("secret".toCharArray(), manager.IBAPIPassword());

        Arrays.fill(firstPassword, '\0');
        Arrays.fill(secondPassword, '\0');
    }

    @Test
    void passwordFromSettingsIsFreshCharArray() {
        settings.set("IbPassword", "fromsettings");
        DefaultLoginManager manager = new DefaultLoginManager();

        char[] firstPassword = manager.IBAPIPassword();
        char[] secondPassword = manager.IBAPIPassword();

        assertNotSame(firstPassword, secondPassword);
        assertArrayEquals("fromsettings".toCharArray(), firstPassword);

        Arrays.fill(firstPassword, '\0');
        Arrays.fill(secondPassword, '\0');
    }

    @Test
    void missingSettingPasswordReturnsEmptyCharArray() {
        DefaultLoginManager manager = new DefaultLoginManager();

        assertEquals(0, manager.IBAPIPassword().length);
    }

    @Test
    void commandLineArgsExtractTwsPasswordAsCharArray() {
        DefaultLoginManager manager = new DefaultLoginManager(new String[]{"ibc", "user", "argpass"});

        assertEquals("user", manager.IBAPIUserName());
        assertArrayEquals("argpass".toCharArray(), manager.IBAPIPassword());
    }

    @Test
    void commandLineArgsExtractFixAndTwsPasswordsAsCharArrays() {
        DefaultLoginManager manager = new DefaultLoginManager(new String[]{"ibc", "fixuser", "fixpass", "apiuser", "apipass", "extra"});

        assertEquals("fixuser", manager.FIXUserName());
        assertArrayEquals("fixpass".toCharArray(), manager.FIXPassword());
        assertEquals("apiuser", manager.IBAPIUserName());
        assertArrayEquals("apipass".toCharArray(), manager.IBAPIPassword());
    }
}
