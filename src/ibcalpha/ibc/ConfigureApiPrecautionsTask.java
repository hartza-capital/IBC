// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2023 Richard L King (rlking@aultan.com)
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

import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

public class ConfigureApiPrecautionsTask implements ConfigurationAction {

    private JDialog configDialog;
    private final Map<String, String> bypassSettingsByCheckboxText;

    /**
     * Creates a task that applies a set of API precaution checkbox
     * settings.
     *
     * @param newBypassSettingsByCheckboxText map from each precaution
     *     checkbox's label text to its desired setting ("yes", "no", or
     *     "" to leave the checkbox untouched); bundled into a single map
     *     (rather than one parameter per precaution) to keep the
     *     constructor's parameter count within Checkstyle's limit
     */
    ConfigureApiPrecautionsTask(
            final Map<String, String> newBypassSettingsByCheckboxText) {
        this.bypassSettingsByCheckboxText = newBypassSettingsByCheckboxText;
    }

    /**
     * Stores the Global Configuration dialog for use during configuration.
     *
     * @param dialog the Global Configuration dialog where API
     *     precautions are configured
     */
    @Override
    public void initialise(final JDialog dialog) {
        this.configDialog = dialog;
    }

    /**
     * Configures the API precaution settings in the TWS Global
     * Configuration dialog.
     *
     * Opens the API > Precautions section and applies the bypass settings
     * for various order and trade warnings. Any setting value that is empty
     * is skipped. Invalid values result in application exit with an error
     * code.
     */
    @Override
    public void run() {
        try {
            Utils.selectConfigSection(
                    configDialog,
                    new String[] {"API", "Precautions"});

            for (Map.Entry<String, String> bypassSetting
                    : bypassSettingsByCheckboxText.entrySet()) {
                doSetting(
                        bypassSetting.getKey(),
                        bypassSetting.getValue().toLowerCase());
            }

        } catch (IbcException e) {
            Utils.exitWithError(
                    ErrorCodes.UNHANDLED_EXCEPTION,
                    "Can't find API - Precautions settings");
        }

    }

    /**
     * Applies a single precaution checkbox setting.
     *
     * Sets the checkbox to the specified state ("yes" or "no"). An empty
     * setting is ignored. Invalid values trigger application exit with an
     * error code.
     *
     * @param checkBoxText the label text of the checkbox to configure
     * @param setting "yes" to check, "no" to uncheck, or "" to skip
     */
    private void doSetting(
            final String checkBoxText,
            final String setting) {
        switch (setting) {
            case "":
                return;
            case "yes":
            case "no":
                break;
            default:
                Utils.exitWithError(
                        ErrorCodes.INVALID_SETTING_VALUE,
                        "Invalid setting value for '" + checkBoxText + "'");
        }

        JCheckBox cb = SwingUtils.findCheckBox(configDialog, checkBoxText);
        if (cb == null) {
            Utils.logToConsole("Checkbox '" + checkBoxText
                    + "' not found");
            return;
        }

        if (setting.equals("yes")) {
            if (cb.isSelected()) {
                Utils.logToConsole("'" + checkBoxText
                        + "' is already selected");
            } else {
                cb.setSelected(true);
                Utils.logToConsole("'" + checkBoxText
                        + "' is now selected");
            }
        } else if (setting.equals("no")) {
            if (!cb.isSelected()) {
                Utils.logToConsole("'" + checkBoxText
                        + "' is already unselected");
            } else {
                cb.setSelected(false);
                Utils.logToConsole("'" + checkBoxText
                        + "' is now unselected");
            }
        }
    }
}
