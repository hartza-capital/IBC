// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2019 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import static java.util.stream.Collectors.toList;

/**
 * Reads and maintains the TWS/Gateway {@code jts.ini} file, ensuring it holds the minimum
 * settings IBC requires.
 */
class JtsIniManager {

    private final static String LOGON_SECTION_HEADER = "[Logon]";
    private final static String IBGATEWAY_SECTION_HEADER = "[IBGateway]";
    private final static String DISPLAYEDPROXYMSG_SETTING = "displayedproxymsg";
    private final static String DISPLAYEDPROXYMSG_SETTING_1 = DISPLAYEDPROXYMSG_SETTING + "=1";
    private final static String LOCALE_SETTING = "Locale";
    private final static String LOCALE_SETTING_EN = LOCALE_SETTING + "=en";
    private final static String S3STORE_SETTING = "s3store";
    private final static String S3STORE_SETTING_FALSE = S3STORE_SETTING + "=false";
    private final static String S3STORE_SETTING_TRUE = S3STORE_SETTING + "=true";
    private final static String USESSL_SETTING = "UseSSL";
    private final static String USESSL_SETTING_TRUE = USESSL_SETTING + "=true";
    private final static String APIONLY_SETTING = "ApiOnly";
    private final static String APIONLY_SETTING_TRUE = APIONLY_SETTING + "=true";
    private final static String APIONLY_SETTING_FALSE = APIONLY_SETTING + "=false";
    private final static String TRUSTED_IPS_SETTING = "TrustedIPs";
    private final static String LOCAL_SERVER_PORT = "LocalServerPort";
    
    private final static String LOCAL_HOST = "127.0.0.1";

    private static String jtsIniFilePath;
    private static File jtsIniFile;
    private static List<String> lines;
    
    private static boolean settingsUpdated;

    /**
     * Creates or updates the jts.ini file so that it contains the minimum settings required
     * for TWS or Gateway to start, writing a new minimal file if none exists.
     *
     * @param jtsIniPath the path of the jts.ini file to create or update
     */
    static void initialise(String jtsIniPath) {
        jtsIniFilePath = jtsIniPath;
        loadIniFile();
        if (jtsIniFile.isFile()) {
            updateExistingFile();
        } else {
            createMinimalFile();
        }
    }
    
    /**
     * Finds the index of a setting within a section of the ini file.
     *
     * @param section the section header (e.g., "[Logon]")
     * @param setting the setting name to find
     * @return the zero-based index of the setting line, or -1 if not found
     */
    private static int getSettingIndex(String section, String setting) {
        String key = setting + "=";

        boolean inSection = false;
        for (int index = 0; index < lines.size(); index++) {
            String l = lines.get(index);
            if (l.compareTo(section) == 0) {
                inSection = true;
            } else if (l.startsWith("[")) {
                if (inSection) return -1;
                inSection = false;
            } else if (inSection) {
                if (l.startsWith(key)) {
                    return index;
                }
            }
        }

        return -1;
    }

    /**
     * Reloads the jts.ini file from disk, discarding the in-memory contents.
     */
    static void reload() {
        loadIniFile();
    }

    /**
     * Loads the jts.ini file contents into memory.
     * Creates a File reference and reads all lines if the file exists.
     */
    private static void loadIniFile() {
        jtsIniFile = new File(jtsIniFilePath);
        if (jtsIniFile.isDirectory()) {
            Utils.exitWithError(ErrorCodes.INVALID_JTSINI_PATH, 
                                jtsIniFilePath + " already exists but is a directory");
        }
        if (jtsIniFile.isFile()) {
            lines = getFileLines(jtsIniFile);
        }
    }

    /**
     * Updates an existing jts.ini file to include all required minimum settings.
     * If settings are missing or need updating, the file is rewritten.
     */
    private static void updateExistingFile() {
        Utils.logToConsole("Ensuring " + jtsIniFilePath + " contains required minimal lines");

        List<JtsIniSectionSetting> missingSettings = getMissingSettings();
        
        boolean rewrite = false;
        if (settingsUpdated) {
            Utils.logToConsole("Some settings updated in " + jtsIniFilePath);
            rewrite = true;
        }
        if (!missingSettings.isEmpty()) {
            Utils.logToConsole("Missing lines in " + jtsIniFilePath);
            rewrite = true;
        }
        if (rewrite){
            jtsIniFile.delete();
            rewriteExistingFile(missingSettings);
        } else {
            Utils.logToConsole("Confirmed " + jtsIniFilePath + " contains required minimal lines");
        }
    }

    /**
     * Determines which required settings are missing from the current jts.ini file.
     * Different settings are required depending on whether FIX, TWS, or Gateway is running.
     *
     * @return a list of missing JtsIniSectionSetting objects
     */
    private static List<JtsIniSectionSetting> getMissingSettings() {
        List<JtsIniSectionSetting> missingSettings = new ArrayList<>();

        if (SessionManager.isFIX()) {
            if (! findSettingAndLog(IBGATEWAY_SECTION_HEADER, APIONLY_SETTING, "true", true)) 
                missingSettings.add(new JtsIniSectionSetting(IBGATEWAY_SECTION_HEADER, APIONLY_SETTING_FALSE));
            
            String trustedIPs = Settings.settings().getString("TrustedTwsApiClientIPs", "");
            trustedIPs = LOCAL_HOST + (trustedIPs.equals("") ? "" : "," + trustedIPs);
            if (! findSettingAndLog(IBGATEWAY_SECTION_HEADER, TRUSTED_IPS_SETTING, trustedIPs, true))
                missingSettings.add(new JtsIniSectionSetting(IBGATEWAY_SECTION_HEADER, TRUSTED_IPS_SETTING + "=" + trustedIPs));
        
            String apiPort = Settings.settings().getString("OverrideTwsApiPort", "");
            if (! "".equals(apiPort)) {
                if (! findSettingAndLog(IBGATEWAY_SECTION_HEADER, LOCAL_SERVER_PORT, apiPort, true))
                    missingSettings.add(new JtsIniSectionSetting(IBGATEWAY_SECTION_HEADER, LOCAL_SERVER_PORT + "=" + apiPort));
            }
        } else {
            if (! findSettingAndLog(LOGON_SECTION_HEADER, S3STORE_SETTING, "true", false))
                missingSettings.add(new JtsIniSectionSetting(LOGON_SECTION_HEADER, S3STORE_SETTING_TRUE));

            if (! findSettingAndLog(LOGON_SECTION_HEADER, LOCALE_SETTING, "en", true)) 
                missingSettings.add(new JtsIniSectionSetting(LOGON_SECTION_HEADER, LOCALE_SETTING_EN));

            if (! findSettingAndLog(LOGON_SECTION_HEADER, DISPLAYEDPROXYMSG_SETTING, "1", true))
                missingSettings.add(new JtsIniSectionSetting(LOGON_SECTION_HEADER, DISPLAYEDPROXYMSG_SETTING_1));

            if (! findSettingAndLog(LOGON_SECTION_HEADER, USESSL_SETTING, "true", true))
                missingSettings.add(new JtsIniSectionSetting(LOGON_SECTION_HEADER, USESSL_SETTING_TRUE));

            if (! findSettingAndLog(IBGATEWAY_SECTION_HEADER, APIONLY_SETTING, "true", true)) 
                missingSettings.add(new JtsIniSectionSetting(IBGATEWAY_SECTION_HEADER, APIONLY_SETTING_TRUE));
            
            if (SessionManager.isGateway()){
                String trustedIPs = Settings.settings().getString("TrustedTwsApiClientIPs", "");
                trustedIPs = LOCAL_HOST + (trustedIPs.equals("") ? "" : "," + trustedIPs);
                if (! findSettingAndLog(IBGATEWAY_SECTION_HEADER, TRUSTED_IPS_SETTING, trustedIPs, true))
                    missingSettings.add(new JtsIniSectionSetting(IBGATEWAY_SECTION_HEADER, TRUSTED_IPS_SETTING + "=" + trustedIPs));
            }
        }
        return missingSettings;
    }

    /**
     * Searches for a setting within a section and logs whether it was found.
     * Optionally updates the setting if it exists but has a different value.
     *
     * @param section the section header to search in
     * @param setting the setting name to find
     * @param expectedValue the expected value of the setting
     * @param updateIfDifferent if true, updates the setting if its value differs from expectedValue
     * @return true if the setting exists, false otherwise
     */
    private static boolean findSettingAndLog(String section, String setting, String expectedValue, boolean updateIfDifferent) {
        int index = getSettingIndex(section, setting);
        
        if (index == -1) {
            Utils.logToConsole("Can't find setting: " + section + "/" + setting + (expectedValue.length() != 0 ? "=" + expectedValue : ""));
            return false;
        }

        String value = lines.get(index).substring(setting.length()+1) + "";

        if (!updateIfDifferent || value.equals(expectedValue)){
            Utils.logToConsole("Found setting: " + section + "/" + setting + "=" + value);
        } else {
            Utils.logToConsole("Found setting: " + section + "/" + setting + "=" + value + ": updating value to " + expectedValue);
            updateSetting(index, expectedValue);
        }
        return true;
    }
    
    /**
     * Updates a setting value at the specified line index in the loaded ini file lines.
     *
     * @param index the zero-based index of the line to update
     * @param newValue the new value for the setting
     */
    private static void updateSetting(int index, String newValue) {
        String line = lines.get(index);
        String settingName = line.substring(0, line.indexOf("="));
        lines.set(index, settingName + "=" + newValue);
        settingsUpdated = true;
    }

    /**
     * Reads all lines from the jts.ini file into memory.
     *
     * @param jtsIniFile the File object representing the jts.ini file
     * @return a list of strings, one per line in the file
     */
    private static List<String> getFileLines(File jtsIniFile) {
        List<String> linesList = null;

        try {
            linesList = Files.readAllLines(jtsIniFile.toPath());
        } catch (IOException e) {
            Utils.exitWithError(ErrorCodes.IO_EXCEPTION_ON_JTSINI, 
                                "Unexpected IOException on " + jtsIniFile + ": " + e.getMessage());
        }
        return linesList;
    }

    /**
     * Creates a minimal jts.ini file with only the required essential settings.
     * Used when the jts.ini file does not exist.
     */
    private static void createMinimalFile() {
        Utils.logToConsole("Creating minimal " + jtsIniFilePath);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(jtsIniFile))) {
            writeIniFileLine(LOGON_SECTION_HEADER, w);
            writeIniFileLine(S3STORE_SETTING_TRUE, w);
            writeIniFileLine(LOCALE_SETTING_EN, w);
            writeIniFileLine(DISPLAYEDPROXYMSG_SETTING_1, w);
            writeIniFileLine(USESSL_SETTING_TRUE, w);

            writeIniFileLine(IBGATEWAY_SECTION_HEADER, w);
            writeIniFileLine(APIONLY_SETTING_TRUE, w);
        } catch (IOException e) {
            Utils.exitWithError(ErrorCodes.IO_EXCEPTION_ON_JTSINI, 
                                "Problem creating " + jtsIniFilePath + ": " + e.getMessage());
        }
    }

    /**
     * Rewrites an existing jts.ini file, integrating missing settings into appropriate sections.
     *
     * @param missingSettings a list of settings that need to be added to the file
     */
    private static void rewriteExistingFile(List<JtsIniSectionSetting> missingSettings) {
        Utils.logToConsole("Rewriting existing " + jtsIniFilePath);

        try (BufferedWriter w = new BufferedWriter(new FileWriter(jtsIniFile))) {
            updateExistingSections(missingSettings, w);
            updateUnprocessedSettings(getUnprocessedSettings(missingSettings), w);
        } catch (IOException e){
            Utils.exitWithError(ErrorCodes.IO_EXCEPTION_ON_JTSINI, 
                                "Problem writing to " + jtsIniFilePath + ": " + e.getMessage());
        }
    }

    /**
     * Writes all existing ini file sections and settings to the output, inserting missing settings into appropriate sections.
     *
     * @param missingSettings a list of settings to insert as sections are processed
     * @param w the BufferedWriter to write output to
     * @throws IOException if an I/O error occurs while writing
     */
    private static void updateExistingSections(
            List<JtsIniSectionSetting> missingSettings,
            BufferedWriter w ) throws IOException {
        String currentSection = "";
        for (String line : lines) {
            if (line.length() != 0 && line.startsWith("[")) {
                writeMissingSettingsToSection(currentSection, missingSettings, w);
                currentSection = line;
            } 
            writeIniFileLine(line, w);
        }
        writeMissingSettingsToSection(currentSection, missingSettings, w);
    }

    /**
     * Filters the missing settings list to return only those that have not been processed yet.
     *
     * @param missingSettings the full list of missing settings
     * @return a list of unprocessed JtsIniSectionSetting objects
     */
    private static List<JtsIniSectionSetting> getUnprocessedSettings(
            List<JtsIniSectionSetting> missingSettings) {
        return missingSettings.stream()
                               .filter(s -> !s.isProcessed)
                               .collect(toList());
    }

    /**
     * Writes unprocessed (new) settings to the output, adding section headers as needed.
     *
     * @param unprocessedSettings a list of settings that have not yet been written to the file
     * @param w the BufferedWriter to write output to
     * @throws IOException if an I/O error occurs while writing
     */
    private static void updateUnprocessedSettings(
            List<JtsIniSectionSetting> unprocessedSettings,
            BufferedWriter w ) throws IOException {
        if (unprocessedSettings.isEmpty()) return;

        String currentSection = "";
        for (JtsIniSectionSetting s : unprocessedSettings){
            if (!s.section.equals(currentSection)) {
                currentSection = s.section;
                writeIniFileLine(currentSection, w);
            }
            writeIniFileLine(s.setting, w);
        }
    }

    /**
     * Writes all missing settings that belong to the specified section.
     * Marks processed settings so they are not written again.
     *
     * @param currentSection the section header to write missing settings for
     * @param missingSettings the full list of missing settings to consider
     * @param w the BufferedWriter to write output to
     * @throws IOException if an I/O error occurs while writing
     */
    private static void writeMissingSettingsToSection(
            String currentSection,
            List<JtsIniSectionSetting> missingSettings,
            BufferedWriter w) throws IOException {

        if (currentSection.length() == 0) return;

        ListIterator<JtsIniSectionSetting> missingSettingsIt = missingSettings.listIterator();
        while (missingSettingsIt.hasNext()) {
            JtsIniSectionSetting missingSetting = missingSettingsIt.next();
            if (missingSetting.section.equals(currentSection)) {
                writeIniFileLine(missingSetting.setting, w);
                missingSetting.isProcessed = true;
            }
        }
    }

    /**
     * Writes a single line to the ini file and logs it for diagnostics.
     *
     * @param line the line content to write
     * @param w the BufferedWriter to write to
     * @throws IOException if an I/O error occurs while writing
     */
    private static void writeIniFileLine(String line, BufferedWriter w) throws IOException {
        Utils.logToConsole("    jts.ini: " + line);
        w.write(line);
        w.newLine();
    }

}
