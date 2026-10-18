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
 * Configuration action that applies the API precaution bypass settings in the
 * TWS Global Configuration dialog.
 */
public class ConfigureApiPrecautionsTask implements ConfigurationAction {
    
    /** Global Configuration dialog the settings are applied to. */
    private JDialog configDialog;
    
    /** Bypass setting for order precautions: "yes", "no", or empty to leave it unchanged. */
    private final String bypassOrderPrecautions;
    /** Bypass setting for the bond warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassBondWarning;
    /** Bypass setting for the negative yield to worst confirmation: "yes", "no", or empty to leave it unchanged. */
    private final String bypassNegativeYieldToWorstConfirmation;
    /** Bypass setting for the called bond warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassCalledBondWarning;
    /** Bypass setting for the same action pair trade warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassSameActionPairTradeWarning;
    /** Bypass setting for the price-based volatility risk warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassPriceBasedVolatilityRiskWarning;
    /** Bypass setting for the US stocks market data in shares warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassUSStocksMarketDataInSharesWarning;
    /** Bypass setting for the redirect order warning: "yes", "no", or empty to leave it unchanged. */
    private final String bypassRedirectOrderWarning;
    /** Bypass setting for the no overfill protection precaution: "yes", "no", or empty to leave it unchanged. */
    private final String bypassNoOverfillProtectionPrecaution;


    /**
     * Creates a task that applies the given precaution bypass settings; an empty
     * value leaves the corresponding precaution unchanged.
     *
     * @param bypassOrderPrecautions "yes" or "no" for bypassing order precautions
     * @param bypassBondWarning "yes" or "no" for bypassing the bond warning
     * @param bypassNegativeYieldToWorstConfirmation "yes" or "no" for bypassing the
     *     negative yield to worst confirmation
     * @param bypassCalledBondWarning "yes" or "no" for bypassing the called bond warning
     * @param bypassSameActionPairTradeWarning "yes" or "no" for bypassing the same action
     *     pair trade warning
     * @param bypassPriceBasedVolatilityRiskWarning "yes" or "no" for bypassing the
     *     price-based volatility risk warning
     * @param bypassUSStocksMarketDataInSharesWarning "yes" or "no" for bypassing the US
     *     stocks market data in shares warning
     * @param bypassRedirectOrderWarning "yes" or "no" for bypassing the redirect order
     *     warning
     * @param bypassNoOverfillProtectionPrecaution "yes" or "no" for bypassing the no
     *     overfill protection precaution
     */
    ConfigureApiPrecautionsTask (
            String bypassOrderPrecautions,
            String bypassBondWarning,
            String bypassNegativeYieldToWorstConfirmation,
            String bypassCalledBondWarning,
            String bypassSameActionPairTradeWarning,
            String bypassPriceBasedVolatilityRiskWarning,
            String bypassUSStocksMarketDataInSharesWarning,
            String bypassRedirectOrderWarning,
            String bypassNoOverfillProtectionPrecaution) {
        this.bypassOrderPrecautions = bypassOrderPrecautions;
        this.bypassBondWarning = bypassBondWarning;
        this.bypassNegativeYieldToWorstConfirmation = bypassNegativeYieldToWorstConfirmation;
        this.bypassCalledBondWarning = bypassCalledBondWarning;
        this.bypassSameActionPairTradeWarning = bypassSameActionPairTradeWarning;
        this.bypassPriceBasedVolatilityRiskWarning = bypassPriceBasedVolatilityRiskWarning;
        this.bypassUSStocksMarketDataInSharesWarning = bypassUSStocksMarketDataInSharesWarning;
        this.bypassRedirectOrderWarning = bypassRedirectOrderWarning;
        this.bypassNoOverfillProtectionPrecaution = bypassNoOverfillProtectionPrecaution;
    }

    /**
     * Stores the Global Configuration dialog for use during configuration.
     *
     * @param configDialog the Global Configuration dialog where API
     *     precautions are configured
     */
    @Override
    public void initialise(JDialog configDialog) {
        this.configDialog = configDialog;
    }

    /**
     * Configures the API precaution settings in the TWS Global Configuration dialog.
     *
     * Opens the API > Precautions section and applies the bypass settings for various order
     * and trade warnings. Any setting value that is empty is skipped.
     * Invalid values result in application exit with an error code.
     */
    @Override
    public void run() {
        try {
            Utils.selectConfigSection(configDialog, new String[] {"API", "Precautions"});

            doSetting("Bypass Order Precautions for API Orders", bypassOrderPrecautions.toLowerCase());
            doSetting("Bypass Bond warning for API Orders.", bypassBondWarning.toLowerCase());
            doSetting("Bypass negative yield to worst confirmation for API Orders.", bypassNegativeYieldToWorstConfirmation.toLowerCase());
            doSetting("Bypass Called Bond warning for API Orders", bypassCalledBondWarning.toLowerCase());
            doSetting("Bypass \"same action pair trade\" warning for API orders.", bypassSameActionPairTradeWarning.toLowerCase());
            doSetting("Bypass price-based volatility risk warning for API Orders.", bypassPriceBasedVolatilityRiskWarning.toLowerCase());
            doSetting("Bypass US Stocks market data in shares warning for API Orders", bypassUSStocksMarketDataInSharesWarning.toLowerCase());
            doSetting("Bypass Redirect Order warning for Stock API Orders", bypassRedirectOrderWarning.toLowerCase());
            doSetting("Bypass No Overfill Protection precaution for destinations where implied natively.", bypassNoOverfillProtectionPrecaution.toLowerCase());

        } catch (IbcException e) {
            Utils.exitWithError(ErrorCodes.UNHANDLED_EXCEPTION, "Can't find API - Precautions settings");
        }

    }

    /**
     * Applies a single precaution checkbox setting.
     *
     * Sets the checkbox to the specified state ("yes" or "no"). An empty setting is ignored.
     * Invalid values trigger application exit with an error code.
     *
     * @param checkBoxText the label text of the checkbox to configure
     * @param setting "yes" to check, "no" to uncheck, or "" to skip
     */
    private void doSetting(String checkBoxText, String setting) {
        switch (setting) {
            case "":
                return;
            case "yes":
            case "no":
                break;
            default:
                Utils.exitWithError(ErrorCodes.INVALID_SETTING_VALUE, "Invalid setting value for '" + checkBoxText + "'" );
        }
        
        JCheckBox cb = SwingUtils.findCheckBox(configDialog, checkBoxText);
        if (cb == null) {
            Utils.logToConsole("Checkbox '" + checkBoxText + "' not found");
            return;
        }
        
        if (setting.equals("yes")) {
            if (cb.isSelected()) {
                Utils.logToConsole("'" + checkBoxText + "' is already selected");
            } else {
                cb.setSelected(true);
                Utils.logToConsole("'" + checkBoxText + "' is now selected");
            }
        } else if (setting.equals("no")) {
            if (!cb.isSelected()) {
                Utils.logToConsole("'" + checkBoxText + "' is already unselected");
            } else {
                cb.setSelected(false);
                Utils.logToConsole("'" + checkBoxText + "' is now unselected");
            }
        }
    }
}
