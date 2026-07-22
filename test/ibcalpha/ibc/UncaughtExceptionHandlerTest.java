// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for UncaughtExceptionHandler.

package ibcalpha.ibc;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class UncaughtExceptionHandlerTest {

    @Test
    void delegatesToUtilsExitWithException() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            UncaughtExceptionHandler handler = new UncaughtExceptionHandler();
            RuntimeException t = new RuntimeException("boom");
            Thread thread = new Thread();

            handler.uncaughtException(thread, t);

            utils.verify(() -> Utils.exitWithException(anyInt(), any()));
        }
    }
}
