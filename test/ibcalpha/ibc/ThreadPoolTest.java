// This file is part of IBC.
// Copyright (C) 2026 Aurélien Perrier
// Unit tests for MyCachedThreadPool and MyScheduledExecutorService.

package ibcalpha.ibc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadPoolTest {

    @Test
    void cachedThreadPoolExecutesSubmittedTask() throws Exception {
        ExecutorService pool = MyCachedThreadPool.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        pool.submit(() -> {
            ran.set(true);
            latch.countDown();
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(ran.get());
    }

    @Test
    void scheduledExecutorServiceExecutesAfterDelay() throws Exception {
        ScheduledExecutorService scheduler = MyScheduledExecutorService.getInstance();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        scheduler.schedule(() -> {
            ran.set(true);
            latch.countDown();
        }, 10, TimeUnit.MILLISECONDS);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(ran.get());
    }
}
