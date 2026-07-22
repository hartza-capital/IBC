// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for TradingModeManager and DefaultTradingModeManager.

package ibcalpha.ibc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class TradingModeManagerTest {

    @BeforeEach
    void setup() {
        Settings.initialise(new StubSettings());
    }

    @AfterEach
    void reset() {
        TradingModeManager.setDefault();
        Settings.setDefault();
    }

    @Test
    void defaultManagerIsLive() {
        TradingModeManager manager = new DefaultTradingModeManager();
        assertEquals("live", manager.getTradingMode());
    }

    @Test
    void constructorAcceptsLive() {
        TradingModeManager manager = new DefaultTradingModeManager("live");
        assertEquals("live", manager.getTradingMode());
    }

    @Test
    void constructorAcceptsPaper() {
        TradingModeManager manager = new DefaultTradingModeManager("paper");
        assertEquals("paper", manager.getTradingMode());
    }

    @Test
    void invalidTradingModeCallsExitWithError() {
        try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            new DefaultTradingModeManager("invalid");
            utils.verify(() -> Utils.exitWithError(anyInt(), anyString()));
        }
    }

    @Test
    void argsConstructorExtractsTradingMode() {
        TradingModeManager manager = new DefaultTradingModeManager(new String[] {"x", "paper"});
        assertEquals("paper", manager.getTradingMode());
    }

    @Test
    void argsConstructorFallsBackToLive() {
        TradingModeManager manager = new DefaultTradingModeManager(new String[] {"x"});
        assertEquals("live", manager.getTradingMode());
    }

    @Test
    void initialiseSetsGlobalInstance() {
        TradingModeManager manager = new DefaultTradingModeManager("paper");
        TradingModeManager.initialise(manager);
        assertEquals("paper", TradingModeManager.tradingModeManager().getTradingMode());
    }

    @Test
    void initialiseRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> TradingModeManager.initialise(null));
    }
}
