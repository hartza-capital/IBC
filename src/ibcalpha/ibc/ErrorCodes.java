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

/**
 * Exit codes used by IBC to indicate the reason for termination.
 *
 * These constants are used when IBC exits, providing diagnostic information
 * to the host system about whether the shutdown was normal or due to an error
 * condition.
 */
public final class ErrorCodes {

    /** Exit code for an unhandled exception. */
    public static final int UNHANDLED_EXCEPTION = 1100;

    /** Exit code when a required control cannot be found. */
    public static final int CANT_FIND_CONTROL = 1101;

    /** Exit code for an incorrect number of command-line arguments. */
    public static final int INCORRECT_NUMBER_OF_ARGS = 1102;

    /** Exit code when the specified INI file does not exist. */
    public static final int INI_FILE_DOES_NOT_EXIST = 1103;

    /** Exit code for an invalid trading mode setting. */
    public static final int INVALID_TRADING_MODE = 1104;

    /** Exit code for an invalid configuration setting value. */
    public static final int INVALID_SETTING_VALUE = 1105;

    /** Exit code when the TWS settings directory cannot be created. */
    public static final int CANT_CREATE_TWS_SETTINGS_DIR = 1106;

    /** Exit code when the TWS entry point cannot be located. */
    public static final int CANT_FIND_ENTRYPOINT = 1107;

    /** Exit code for an invalid jts.ini file path. */
    public static final int INVALID_JTSINI_PATH = 1108;

    /** Exit code when an I/O error occurs while handling jts.ini. */
    public static final int IO_EXCEPTION_ON_JTSINI = 1109;

    /** Exit code when an invalid internal state is detected. */
    public static final int INVALID_STATE = 1110;

    /** Exit code when second-factor authentication login times out. */
    public static final int SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT = 1111;

    /** Exit code when the login dialog display times out. */
    public static final int LOGIN_DIALOG_DISPLAY_TIMED_OUT = 1112;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ErrorCodes() {
    }
}
