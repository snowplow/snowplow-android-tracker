/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.tracker.classic;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Static Class which holds the logic for controlling
 * the Thread Pool for the Classic Tracker.
 */
public class Executor {

    private static ScheduledExecutorService executor;

    /**
     * If the executor is null creates a
     * new executor.
     *
     * @return the executor
     */
    private static ScheduledExecutorService getExecutor() {
        synchronized (Executor.class) {
            if (executor == null) {
                executor = Executors.newScheduledThreadPool(10);
            }
        }
        return executor;
    }

    /**
     * Sends a runnable to the executor service.
     *
     * @param runnable the runnable to be queued
     */
    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    /**
     * Schedules a runnable to run at some point in
     * the future.
     *
     * @param runnable the runnable to be queued
     * @param delay the count of units to delay
     *              execution by
     * @param timeUnit the time unit for the delay
     */
    public static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        getExecutor().schedule(runnable, delay, timeUnit);
    }

    /**
     * Shuts the executor service down and resets
     * the executor to a null state.
     */
    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    /**
     * Returns the status of the executor.
     *
     * @return if the executor is active
     */
    public static boolean status() {
        if (executor == null || executor.isShutdown()) {
            return false;
        } else {
            return true;
        }
    }
}
