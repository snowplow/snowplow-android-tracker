package com.snowplowanalytics.snowplow.tracker.lite;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* TODO improve. This is pretty simplistic right now, but will work */

class Executor {

    static ScheduledExecutorService executor;

    static ScheduledExecutorService getExecutor() {
        synchronized (Executor.class) {
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor();
            }
        }
        return executor;
    }

    static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        getExecutor().schedule(runnable, delay, timeUnit);
    }

    static void shutdown() {
        executor = null;
        getExecutor().shutdown();
    }
}
