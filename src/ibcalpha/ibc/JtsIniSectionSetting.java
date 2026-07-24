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

/**
 * Represents a single setting within a section of the jts.ini file.
 * Used by JtsIniManager to track settings that need to be added or updated.
 */
class JtsIniSectionSetting {
    /**
     * The section header (e.g., "[Logon]" or "[IBGateway]").
     */
    String section;

    /**
     * The setting line content (e.g., "Locale=en" or "ApiOnly=true").
     */
    String setting;

    /**
     * Flag indicating whether this setting has been written to the ini file.
     */
    boolean isProcessed;

    /**
     * Constructs a JtsIniSectionSetting with the specified section and setting.
     *
     * @param sectionValue the section header for this setting
     * @param settingValue the setting line content
     */
    JtsIniSectionSetting(final String sectionValue, final String settingValue) {
        this.section = sectionValue;
        this.setting = settingValue;
    }
}
