// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for ErrorCodes constants.

package ibcalpha.ibc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodesTest {

    @Test
    void errorCodesHaveExpectedValues() {
        assertEquals(1100, ErrorCodes.UNHANDLED_EXCEPTION);
        assertEquals(1101, ErrorCodes.CANT_FIND_CONTROL);
        assertEquals(1102, ErrorCodes.INCORRECT_NUMBER_OF_ARGS);
        assertEquals(1103, ErrorCodes.INI_FILE_DOES_NOT_EXIST);
        assertEquals(1104, ErrorCodes.INVALID_TRADING_MODE);
        assertEquals(1105, ErrorCodes.INVALID_SETTING_VALUE);
        assertEquals(1106, ErrorCodes.CANT_CREATE_TWS_SETTINGS_DIR);
        assertEquals(1107, ErrorCodes.CANT_FIND_ENTRYPOINT);
        assertEquals(1108, ErrorCodes.INVALID_JTSINI_PATH);
        assertEquals(1109, ErrorCodes.IO_EXCEPTION_ON_JTSINI);
        assertEquals(1110, ErrorCodes.INVALID_STATE);
        assertEquals(1111, ErrorCodes.SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT);
        assertEquals(1112, ErrorCodes.LOGIN_DIALOG_DISPLAY_TIMED_OUT);
    }
}
