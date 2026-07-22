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

import javax.swing.JCheckBox;
import javax.swing.JDialog;

/**
 * Configuration action that applies the API precaution settings in the TWS
 * Global Configuration dialog.
 */
public class ConfigureApiPrecautionsTask implements ConfigurationAction {

    /** Global Configuration dialog used to apply the settings. */
    private JDialog configDialog;

    /** Setting for bypassing order precautions for API orders. */
    private final String bypassOrderPrecautions;

    /** Setting for bypassing the bond warning for API orders. */
    private final String bypassBondWarning;

    /** Setting for bypassing the negative yield to worst confirmation. */
    private final String bypassNegativeYieldToWorstConfirmation;

    /** Setting for bypassing the called bond warning for API orders. */
    private final String bypassCalledBondWarning;

    /** Setting for bypassing the same-action pair trade warning. */
    private final String bypassSameActionPairTradeWarning;

    /** Setting for bypassing the price-based volatility risk warning. */
    private final String bypassPriceBasedVolatilityRiskWarning;

    /** Setting for bypassing the US stocks market data in shares warning. */
    private final String bypassUSStocksMarketDataInSharesWarning;

    /** Setting for bypassing redirect order warnings for stock API orders. */
    private final String bypassRedirectOrderWarning;

    /**
     * Setting for bypassing the no-overfill protection precaution for
     * destinations where implied natively.
     */
    private final String bypassNoOverfillProtectionPrecaution;

    /**
     * Creates a new task that applies the supplied API precaution settings.
     *
     * @param bypassOrderPrecautionsSetting bypass order precautions for API
     *     orders
     * @param bypassBondWarningSetting bypass bond warning for API orders
     * @param bypassNegativeYieldToWorstConfirmationSetting bypass negative
     *     yield to worst confirmation for API orders
     * @param bypassCalledBondWarningSetting bypass called bond warning for API
     *     orders
     * @param bypassSameActionPairTradeWarningSetting bypass same-action pair
     *     trade warning for API orders
     * @param bypassPriceBasedVolatilityRiskWarningSetting bypass price-based
     *     volatility risk warning for API orders
     * @param bypassUSStocksMarketDataInSharesWarningSetting bypass US stocks
     *     market data in shares warning for API orders
     * @param bypassRedirectOrderWarningSetting bypass redirect order warning
     *     for stock API orders
     * @param bypassNoOverfillProtectionPrecautionSetting bypass no-overfill
     *     protection precaution for destinations where implied natively
     */
    ConfigureApiPrecautionsTask(
            final String bypassOrderPrecautionsSetting,
            final String bypassBondWarningSetting,
            final String bypassNegativeYieldToWorstConfirmationSetting,
            final String bypassCalledBondWarningSetting,
            final String bypassSameActionPairTradeWarningSetting,
            final String bypassPriceBasedVolatilityRiskWarningSetting,
            final String bypassUSStocksMarketDataInSharesWarningSetting,
            final String bypassRedirectOrderWarningSetting,
            final String bypassNoOverfillProtectionPrecautionSetting) {
        this.bypassOrderPrecautions = bypassOrderPrecautionsSetting;
        this.bypassBondWarning = bypassBondWarningSetting;
        this.bypassNegativeYieldToWorstConfirmation =
                bypassNegativeYieldToWorstConfirmationSetting;
        this.bypassCalledBondWarning = bypassCalledBondWarningSetting;
        this.bypassSameActionPairTradeWarning =
                bypassSameActionPairTradeWarningSetting;
        this.bypassPriceBasedVolatilityRiskWarning =
                bypassPriceBasedVolatilityRiskWarningSetting;
        this.bypassUSStocksMarketDataInSharesWarning =
                bypassUSStocksMarketDataInSharesWarningSetting;
        this.bypassRedirectOrderWarning = bypassRedirectOrderWarningSetting;
        this.bypassNoOverfillProtectionPrecaution =
                bypassNoOverfillProtectionPrecautionSetting;
    }

    /**
     * Stores the Global Configuration dialog for use during configuration.
     *
     * @param configurationDialog the Global Configuration dialog where API
     *     precautions are configured
     */
    @Override
    public void initialise(final JDialog configurationDialog) {
        this.configDialog = configurationDialog;
    }

    /**
     * Configures the API precaution settings in the TWS Global Configuration
     * dialog.
     *
     * Opens the API > Precautions section and applies the bypass settings for
     * various order and trade warnings. Any setting value that is empty is
     * skipped. Invalid values result in application exit with an error code.
     */
    @Override
    public void run() {
        try {
            Utils.selectConfigSection(configDialog,
                    new String[] {"API", "Precautions"});

            doSetting("Bypass Order Precautions for API Orders",
                    bypassOrderPrecautions.toLowerCase());
            doSetting("Bypass Bond warning for API Orders.",
                    bypassBondWarning.toLowerCase());
            doSetting(
                    "Bypass negative yield to worst confirmation for API"
                            + " Orders.",
                    bypassNegativeYieldToWorstConfirmation.toLowerCase());
            doSetting("Bypass Called Bond warning for API Orders",
                    bypassCalledBondWarning.toLowerCase());
            doSetting(
                    "Bypass \"same action pair trade\" warning for API orders.",
                    bypassSameActionPairTradeWarning.toLowerCase());
            doSetting(
                    "Bypass price-based volatility risk warning for API"
                            + " Orders.",
                    bypassPriceBasedVolatilityRiskWarning.toLowerCase());
            doSetting(
                    "Bypass US Stocks market data in shares warning for API"
                            + " Orders",
                    bypassUSStocksMarketDataInSharesWarning.toLowerCase());
            doSetting("Bypass Redirect Order warning for Stock API Orders",
                    bypassRedirectOrderWarning.toLowerCase());
            doSetting(
                    "Bypass No Overfill Protection precaution for destinations"
                            + " where implied natively.",
                    bypassNoOverfillProtectionPrecaution.toLowerCase());

        } catch (IbcException e) {
            Utils.exitWithError(ErrorCodes.UNHANDLED_EXCEPTION,
                    "Can't find API - Precautions settings");
        }

    }

    /**
     * Applies a single precaution checkbox setting.
     *
     * Sets the checkbox to the specified state ("yes" or "no"). An empty
     * setting is ignored. Invalid values trigger application exit with an error
     * code.
     *
     * @param checkBoxText the label text of the checkbox to configure
     * @param setting "yes" to check, "no" to uncheck, or "" to skip
     */
    private void doSetting(final String checkBoxText, final String setting) {
        switch (setting) {
            case "":
                return;
            case "yes":
            case "no":
                break;
            default:
                Utils.exitWithError(ErrorCodes.INVALID_SETTING_VALUE,
                        "Invalid setting value for '" + checkBoxText + "'");
        }

        JCheckBox cb = SwingUtils.findCheckBox(configDialog, checkBoxText);
        if (cb == null) {
            Utils.logToConsole("Checkbox '" + checkBoxText + "' not found");
            return;
        }

        if (setting.equals("yes")) {
            if (cb.isSelected()) {
                Utils.logToConsole(
                        "'" + checkBoxText + "' is already selected");
            } else {
                cb.setSelected(true);
                Utils.logToConsole("'" + checkBoxText + "' is now selected");
            }
        } else if (setting.equals("no")) {
            if (!cb.isSelected()) {
                Utils.logToConsole(
                        "'" + checkBoxText + "' is already unselected");
            } else {
                cb.setSelected(false);
                Utils.logToConsole("'" + checkBoxText + "' is now unselected");
            }
        }
    }
}
