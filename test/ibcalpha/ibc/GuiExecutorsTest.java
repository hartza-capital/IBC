// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for the GUI executor singletons.

package ibcalpha.ibc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiExecutorsTest {

    @Test
    void guiDeferredExecutorRunsTaskOnEdt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ranOnEdt = new AtomicBoolean(false);

        GuiDeferredExecutor.instance().execute(() -> {
            ranOnEdt.set(SwingUtilities.isEventDispatchThread());
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(ranOnEdt.get());
    }

    @Test
    void guiSynchronousExecutorRunsTask() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        GuiSynchronousExecutor.instance().execute(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void guiExecutorRunsTaskOnEdtWhenCalledOffEdt() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ranOnEdt = new AtomicBoolean(false);

        GuiExecutor.instance().execute(() -> {
            ranOnEdt.set(SwingUtilities.isEventDispatchThread());
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(ranOnEdt.get());
    }
}
