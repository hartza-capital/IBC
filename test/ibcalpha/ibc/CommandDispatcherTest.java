// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for CommandDispatcher command routing.

package ibcalpha.ibc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandDispatcherTest {

    @BeforeEach
    void reset() {
        Settings.initialise(new StubSettings());
    }

    @Test
    void exitCommandWritesGoodbyeAndCloses() {
        FakeCommandChannel fake = new FakeCommandChannel("EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("OK Goodbye"));
    }

    @Test
    void invalidCommandWritesNack() {
        FakeCommandChannel fake = new FakeCommandChannel("BOGUS", "EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("ERROR Command invalid"));
        assertTrue(fake.output().contains("OK Goodbye"));
    }

    @Test
    void reconnectDataIsRejectedForFixGateway() {
        TestUtils.setSessionState(false, true, true);
        FakeCommandChannel fake = new FakeCommandChannel("RECONNECTDATA", "EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("RECONNECTDATA is not valid for the FIX Gateway"));
    }

    @Test
    void reconnectAccountIsRejectedForFixGateway() {
        TestUtils.setSessionState(false, true, true);
        FakeCommandChannel fake = new FakeCommandChannel("RECONNECTACCOUNT", "EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("RECONNECTACCOUNT is not valid for the FIX Gateway"));
    }

    @Test
    void restartIsRejectedForFixGateway() {
        TestUtils.setSessionState(false, true, true);
        FakeCommandChannel fake = new FakeCommandChannel("RESTART", "EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("RESTART is not valid for the FIX Gateway"));
    }

    @Test
    void enableApiIsRejectedForGateway() {
        TestUtils.setSessionState(true, false, true);
        FakeCommandChannel fake = new FakeCommandChannel("ENABLEAPI", "EXIT");

        new CommandDispatcher(fake.channel()).run();

        assertTrue(fake.output().contains("ENABLEAPI is not valid for the IB Gateway"));
    }
}
