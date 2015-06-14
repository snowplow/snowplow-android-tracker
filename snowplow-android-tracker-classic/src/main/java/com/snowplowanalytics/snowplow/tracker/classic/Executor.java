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

public class Executor {

    private static ScheduledExecutorService executor;

    private static ScheduledExecutorService getExecutor() {
        synchronized (Executor.class) {
            if (executor == null) {
                executor = Executors.newScheduledThreadPool(10);
            }
        }
        return executor;
    }

    public static void execute(Runnable runnable) {
        getExecutor().execute(runnable);
    }

    public static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        getExecutor().schedule(runnable, delay, timeUnit);
    }

    public static void shutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    public static boolean status() {
        if (executor == null || executor.isShutdown()) {
            return false;
        } else {
            return true;
        }
    }
}
