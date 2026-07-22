// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for SwitchLock.

package ibcalpha.ibc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchLockTest {

    private SwitchLock lock;

    @BeforeEach
    void createLock() {
        lock = new SwitchLock();
    }

    @Test
    void setReturnsTrueWhenPreviouslyCleared() {
        assertTrue(lock.set());
    }

    @Test
    void setReturnsFalseWhenAlreadySet() {
        lock.set();
        assertFalse(lock.set());
    }

    @Test
    void queryReflectsCurrentState() {
        assertFalse(lock.query());
        lock.set();
        assertTrue(lock.query());
        lock.clear();
        assertFalse(lock.query());
    }
}
