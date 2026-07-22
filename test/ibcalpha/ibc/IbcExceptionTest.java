// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for IbcException.

package ibcalpha.ibc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IbcExceptionTest {

    @Test
    void storesMessage() {
        IbcException e = new IbcException("something went wrong");
        assertEquals("something went wrong", e.getMessage());
    }

    @Test
    void toStringReturnsMessage() {
        IbcException e = new IbcException("short");
        assertEquals("short", e.toString());
    }
}
