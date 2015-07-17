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

package com.snowplowanalytics.snowplow.tracker.rx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rx.schedulers.Schedulers;
import rx.Scheduler;

/**
 * Creates a single Scheduler for use
 * by the Tracker and Emitter.
 */
public class SchedulerRx {

    private static Scheduler scheduler;
    private static ExecutorService executor;
    private static int threadCount = 2; // Minimum amount of threads.

    /**
     * Returns the Rx Scheduler
     *
     * @return the scheduler
     */
    public static Scheduler getScheduler() {
        if (scheduler == null) {
            scheduler = Schedulers.from(getExecutor());
        }
        return scheduler;
    }

    /**
     * Returns the executor for the scheduler.
     *
     * @return the executor
     */
    private static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(threadCount);
        }
        return executor;
    }

    /**
     * Changes the amount of threads the
     * scheduler will be able to use.
     * - This can only be set before the scheduler
     *   is first accessed, after this point the
     *   function will not effect anything.
     *
     * @param count the thread count.
     */
    public static void setThreadCount(final int count) {
        threadCount = count;
    }
}
