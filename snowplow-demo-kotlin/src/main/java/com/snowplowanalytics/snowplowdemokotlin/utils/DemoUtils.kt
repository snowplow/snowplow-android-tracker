/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplowdemokotlin.utils

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Utility class to build the Trackers and
 * to hold a static executor.
 */
object DemoUtils {
    private var executor = Executors.newSingleThreadScheduledExecutor()

    /**
     * Executes a repeating runnable
     *
     * @param runnable a new task
     * @param initDelay the delay before polling
     * @param delay the delay between polls
     * @param timeUnit the time-unit for the delays
     */
    fun scheduleRepeating(runnable: Runnable, initDelay: Long, delay: Long, timeUnit: TimeUnit) {
        executor.scheduleAtFixedRate(runnable, initDelay, delay, timeUnit)
    }

    /**
     * Shuts the executor down and resets it.
     */
    fun resetExecutor() {
        executor.shutdown()
        executor = Executors.newSingleThreadScheduledExecutor()
    }
}
