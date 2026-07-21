// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for JtsIniManager.

package ibcalpha.ibc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JtsIniManagerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void reset() {
        StubSettings stub = new StubSettings();
        Settings.initialise(stub);
        TestUtils.setSessionState(false, false, true);
        TestUtils.setStaticField(JtsIniManager.class, "settingsUpdated", false);
    }

    @Test
    void createsMinimalJtsIniWhenFileDoesNotExist() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        assertFalse(Files.exists(jtsIni));

        JtsIniManager.initialise(jtsIni.toString());

        assertTrue(Files.exists(jtsIni));
        List<String> lines = Files.readAllLines(jtsIni);
        assertTrue(lines.contains("[Logon]"));
        assertTrue(lines.contains("s3store=true"));
        assertTrue(lines.contains("Locale=en"));
        assertTrue(lines.contains("displayedproxymsg=1"));
        assertTrue(lines.contains("UseSSL=true"));
        assertTrue(lines.contains("[IBGateway]"));
        assertTrue(lines.contains("ApiOnly=true"));
    }

    @Test
    void leavesExistingValidFileUnchanged() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        Files.write(jtsIni, List.of(
                "[Logon]",
                "Locale=en",
                "displayedproxymsg=1",
                "UseSSL=true",
                "s3store=true",
                "[IBGateway]",
                "ApiOnly=true"
        ));

        JtsIniManager.initialise(jtsIni.toString());

        List<String> lines = Files.readAllLines(jtsIni);
        assertEquals(7, lines.size());
    }

    @Test
    void addsMissingSettingsToExistingFile() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        Files.write(jtsIni, List.of(
                "[Logon]",
                "Locale=en"
        ));

        JtsIniManager.initialise(jtsIni.toString());

        List<String> lines = Files.readAllLines(jtsIni);
        assertTrue(lines.contains("s3store=true"));
        assertTrue(lines.contains("displayedproxymsg=1"));
        assertTrue(lines.contains("UseSSL=true"));
        assertTrue(lines.contains("[IBGateway]"));
        assertTrue(lines.contains("ApiOnly=true"));
    }

    @Test
    void updatesDifferentSettingValues() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        Files.write(jtsIni, List.of(
                "[Logon]",
                "Locale=fr",
                "displayedproxymsg=1",
                "UseSSL=true",
                "s3store=true",
                "[IBGateway]",
                "ApiOnly=false"
        ));

        JtsIniManager.initialise(jtsIni.toString());

        List<String> lines = Files.readAllLines(jtsIni);
        assertTrue(lines.contains("Locale=en"));
        assertTrue(lines.contains("ApiOnly=true"));
    }

    @Test
    void preservesS3storeFalseWhenExplicitlySet() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        Files.write(jtsIni, List.of(
                "[Logon]",
                "Locale=en",
                "displayedproxymsg=1",
                "UseSSL=true",
                "s3store=false",
                "[IBGateway]",
                "ApiOnly=true"
        ));

        JtsIniManager.initialise(jtsIni.toString());

        List<String> lines = Files.readAllLines(jtsIni);
        assertTrue(lines.contains("s3store=false"));
    }

    @Test
    void gatewayAddsTrustedIpsToExistingFile() throws IOException {
        Path jtsIni = tempDir.resolve("jts.ini");
        Files.write(jtsIni, List.of(
                "[Logon]",
                "Locale=en",
                "displayedproxymsg=1",
                "UseSSL=true",
                "s3store=true",
                "[IBGateway]",
                "ApiOnly=true"
        ));

        StubSettings stub = new StubSettings();
        stub.set("TrustedTwsApiClientIPs", "192.168.1.1");
        Settings.initialise(stub);
        TestUtils.setSessionState(true, false, true);

        JtsIniManager.initialise(jtsIni.toString());

        List<String> lines = Files.readAllLines(jtsIni);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("TrustedIPs=127.0.0.1,192.168.1.1")));
    }
}
