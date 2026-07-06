package org.blackum.blackaddons.common.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ThreadUtils {
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public static void loop(long intervalMs, Supplier<Boolean> stopCondition, Runnable task) {
        executor.scheduleAtFixedRate(() -> {
            if (!stopCondition.get()) {
                task.run();
            }
        }, 0L, intervalMs, TimeUnit.MILLISECONDS);
    }
}
