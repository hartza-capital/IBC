// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for DefaultSettings.

package ibcalpha.ibc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSettingsTest {

    @TempDir
    Path tempDir;

    private File writeIni(String content) throws IOException {
        File f = tempDir.resolve("config.ini").toFile();
        try (FileWriter w = new FileWriter(f)) {
            w.write(content);
        }
        return f;
    }

    @Test
    void missingFileIsHandledGracefully() {
        File missing = tempDir.resolve("missing.ini").toFile();
        DefaultSettings settings = new DefaultSettings(missing.getAbsolutePath());
        assertEquals("default", settings.getString("NoSuchKey", "default"));
    }

    @Test
    void getStringReturnsTrimmedValue() throws IOException {
        File f = writeIni("MyKey=  value  \n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals("value", settings.getString("MyKey", "default"));
    }

    @Test
    void getStringReturnsDefaultForEmptyValue() throws IOException {
        File f = writeIni("MyKey=\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals("default", settings.getString("MyKey", "default"));
    }

    @Test
    void getIntReturnsValue() throws IOException {
        File f = writeIni("Port=1234\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals(1234, settings.getInt("Port", 0));
    }

    @Test
    void getIntReturnsDefaultForInvalidValue() throws IOException {
        File f = writeIni("Port=notANumber\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals(0, settings.getInt("Port", 0));
    }

    @Test
    void getIntReturnsDefaultForMissingKey() throws IOException {
        File f = writeIni("Other=value\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals(42, settings.getInt("Port", 42));
    }

    @Test
    void getDoubleReturnsValue() throws IOException {
        File f = writeIni("Rate=1.5\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals(1.5, settings.getDouble("Rate", 0.0), 0.0001);
    }

    @Test
    void getDoubleReturnsDefaultForInvalidValue() throws IOException {
        File f = writeIni("Rate=bad\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals(0.0, settings.getDouble("Rate", 0.0), 0.0001);
    }

    @Test
    void getCharReturnsValue() throws IOException {
        File f = writeIni("Letter=X\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals('X', settings.getChar("Letter", "A"));
    }

    @Test
    void getCharReturnsDefaultForEmptyValue() throws IOException {
        File f = writeIni("Letter=\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertEquals('A', settings.getChar("Letter", "A"));
    }

    @Test
    void getBooleanAcceptsTrueAndYes() throws IOException {
        File f = writeIni("A=true\nB=yes\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertTrue(settings.getBoolean("A", false));
        assertTrue(settings.getBoolean("B", false));
    }

    @Test
    void getBooleanAcceptsFalseAndNo() throws IOException {
        File f = writeIni("A=false\nB=no\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertFalse(settings.getBoolean("A", true));
        assertFalse(settings.getBoolean("B", true));
    }

    @Test
    void getBooleanReturnsDefaultForUnknownValue() throws IOException {
        File f = writeIni("A=maybe\n");
        DefaultSettings settings = new DefaultSettings(f.getAbsolutePath());
        assertTrue(settings.getBoolean("A", true));
        assertFalse(settings.getBoolean("A", false));
    }

    @Test
    void settingsPathUsesArgumentWhenProvided() throws IOException {
        File f = writeIni("Key=value\n");
        DefaultSettings settings = new DefaultSettings(new String[] {f.getAbsolutePath()});
        assertEquals("value", settings.getString("Key", "default"));
    }

    @Test
    void generateDefaultIniPathIsPlatformDependent() {
        String path = DefaultSettings.generateDefaultIniPath();
        assertTrue(path.endsWith(File.separator + "config.ini"));
    }
}
