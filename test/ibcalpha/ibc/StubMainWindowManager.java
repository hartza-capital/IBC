// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Test-only MainWindowManager implementation.

package ibcalpha.ibc;

import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

/**
 * In-memory test double for {@link MainWindowManager}: the main window is
 * whatever was set last, and the timed getter returns it immediately.
 */
final class StubMainWindowManager extends MainWindowManager {

    /** The window returned by the getters, null until one is set. */
    private JFrame mainWindow;

    /**
     * Does nothing; this double writes no diagnostic output.
     */
    @Override
    public void logDiagnosticMessage() {
        // no-op for tests
    }

    /**
     * Returns the stored main window without waiting.
     *
     * @param timeout ignored; no wait is performed
     * @param unit ignored; no wait is performed
     * @return the window set by {@link #setMainWindow(JFrame)}, or null if none was set
     */
    @Override
    public JFrame getMainWindow(long timeout, TimeUnit unit) {
        return mainWindow;
    }

    /**
     * Returns the stored main window.
     *
     * @return the window set by {@link #setMainWindow(JFrame)}, or null if none was set
     */
    @Override
    public JFrame getMainWindow() {
        return mainWindow;
    }

    /**
     * Stores the window that the getters return.
     *
     * @param window the window to return, null to report that no window is available
     */
    @Override
    public void setMainWindow(JFrame window) {
        this.mainWindow = window;
    }

    /**
     * Does nothing; the stored window is never iconized.
     */
    @Override
    public void iconizeIfRequired() {
        // no-op for tests
    }
}
