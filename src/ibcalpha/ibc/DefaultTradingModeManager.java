// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
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

public class DefaultTradingModeManager extends TradingModeManager {

    /**
     * The configured trading mode ("live" or "paper").
     */
    private String tradingMode;

    /**
     * Diagnostic message describing the source of the trading mode setting.
     */
    private final String message;

    /**
     * Whether the trading mode was taken from settings rather than from a
     * constructor argument.
     */
    private final boolean fromSettings;

    /**
     * The number of command-line arguments that includes a trading mode in
     * the second position.
     */
    private static final int TWO_ARGS_COUNT = 2;

    /**
     * The index of the trading mode within a two-argument command line.
     */
    private static final int TRADING_MODE_INDEX_TWO_ARGS = 1;

    /**
     * The number of command-line arguments that includes a trading mode in
     * the fourth position.
     */
    private static final int FOUR_ARGS_COUNT = 4;

    /**
     * The index of the trading mode within a four-argument command line.
     */
    private static final int TRADING_MODE_INDEX_FOUR_ARGS = 3;

    /**
     * The number of command-line arguments that includes a trading mode in
     * the sixth position.
     */
    private static final int SIX_ARGS_COUNT = 6;

    /**
     * The index of the trading mode within a six-argument command line.
     */
    private static final int TRADING_MODE_INDEX_SIX_ARGS = 5;

    /**
     * Creates a new DefaultTradingModeManager with trading mode set to live.
     */
    public DefaultTradingModeManager() {
        fromSettings = false;
        setTradingMode(TRADING_MODE_LIVE);
        message = "parameterless constructor (trading mode live assumed)";
    }

    /**
     * Creates a new DefaultTradingModeManager with the specified trading mode.
     *
     * @param requestedTradingMode the trading mode (either "live" or "paper")
     */
    public DefaultTradingModeManager(final String requestedTradingMode) {
        fromSettings = false;
        setTradingMode(requestedTradingMode);
        message = "constructor parameter tradingMode=" + requestedTradingMode;
    }

    /**
     * Creates a new DefaultTradingModeManager with trading mode extracted
     * from command-line arguments.
     *
     * <p>Trading mode must be in args[1] (if there are two args), args[3]
     * (if there are four args), or args[5] (if there are six args). If not
     * found, trading mode is loaded from settings.
     *
     * @param args the command-line arguments containing trading mode at a
     *     specific position
     */
    public DefaultTradingModeManager(final String[] args) {
        if (args.length == 0) {
            setTradingMode(TRADING_MODE_LIVE);
        } else if (args.length == TWO_ARGS_COUNT) {
            setTradingMode(args[TRADING_MODE_INDEX_TWO_ARGS]);
        } else if (args.length == FOUR_ARGS_COUNT) {
            setTradingMode(args[TRADING_MODE_INDEX_FOUR_ARGS]);
        } else if (args.length == SIX_ARGS_COUNT) {
            setTradingMode(args[TRADING_MODE_INDEX_SIX_ARGS]);
        }

        if (tradingMode != null) {
            fromSettings = false;
            message = "constructor parameter args: tradingMode=" + tradingMode;
        } else {
            fromSettings = true;
            message = "constructor parameter args but trading mode not "
                    + "present - will be taken from settings";
        }
    }

    /**
     * Sets the trading mode to the specified value after validation.
     *
     * @param value the trading mode to set (must be "live" or "paper")
     * @throws SystemException if the value is not a valid trading mode
     */
    private void setTradingMode(final String value) {
        if (!(value.equalsIgnoreCase(TRADING_MODE_LIVE)
                || value.equalsIgnoreCase(TRADING_MODE_PAPER))) {
            Utils.exitWithError(
                    ErrorCodes.INVALID_TRADING_MODE,
                    "Invalid Trading Mode argument or .ini file setting: "
                            + tradingMode);
        }
        tradingMode = value;
    }

    /**
     * Logs a diagnostic message describing the source of the trading mode
     * setting.
     */
    @Override
    public void logDiagnosticMessage() {
        Utils.logToConsole("using default trading mode manager: " + message);
    }

    /**
     * Returns the current trading mode, retrieving from settings if
     * necessary.
     *
     * @return the trading mode ("live" or "paper")
     */
    @Override
    public String getTradingMode() {
        if (fromSettings) {
            setTradingMode(
                    Settings.settings().getString(
                            "TradingMode", TRADING_MODE_LIVE));
            Utils.logToConsole(
                    "trading mode from settings: tradingMode=" + tradingMode);
        }
        return tradingMode;
    }

}
