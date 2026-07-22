// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Test-only MainWindowManager implementation.

package ibcalpha.ibc;

import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

/**
 * In-memory MainWindowManager implementation for unit tests.
 */
final class StubMainWindowManager extends MainWindowManager {

    private JFrame mainWindow;

    @Override
    public void logDiagnosticMessage() {
        // no-op for tests
    }

    @Override
    public JFrame getMainWindow(long timeout, TimeUnit unit) {
        return mainWindow;
    }

    @Override
    public JFrame getMainWindow() {
        return mainWindow;
    }

    @Override
    public void setMainWindow(JFrame window) {
        this.mainWindow = window;
    }

    @Override
    public void iconizeIfRequired() {
        // no-op for tests
    }
}
