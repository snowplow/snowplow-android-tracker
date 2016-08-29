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

package com.snowplowanalytics.snowplow.tracker;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Static Class which holds the logic for controlling
 * the Thread Pool for the Classic Tracker.
 */
public class Executor {

    private static ExecutorService executor;
    private static int threadCount = 2; // Minimum amount of threads.

    /**
     * If the executor is null creates a
     * new executor.
     *
     * @return the executor
     */
    private synchronized static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(threadCount);
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
     * Sends a callable to the executor service and
     * returns a Future.
     *
     * @param callable the callable to be queued
     * @return the future object to be queried
     */
    public static Future futureCallable(Callable callable) {
        return getExecutor().submit(callable);
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
     * Changes the amount of threads the
     * scheduler will be able to use.
     *
     * NOTE: This can only be set before the
     * scheduler is first accessed, after this
     * point the function will not effect anything.
     *
     * @param count the thread count
     */
    public static void setThreadCount(final int count) {
        threadCount = count;
    }
}
