/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.emitter

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.tracker.Logger
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Static Class which holds the logic for controlling
 * the Thread Pool for the Classic Tracker.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object Executor {
    private var executor: ExecutorService? = null
    
    
    var threadCount = 2 // Minimum amount of threads.

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
    fun threadCount(count: Int) {
        if (count >= 2) {
            threadCount = count
        }
    }

    /**
     * If the executor is null creates a
     * new executor.
     *
     * @return the executor
     */
    @Synchronized
    private fun getExecutor(): ExecutorService {
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(threadCount)
        }
        return executor!!
    }

    /**
     * Sends a runnable to the executor service.
     * Errors are logged but not tracked with the diagnostic feature.
     *
     * @param tag string indicating the source of the runnable for logging purposes in case of
     * exceptions raised by the runnable
     * @param runnable the runnable to be queued
     */
    fun execute(tag: String?, runnable: Runnable?) {
        execute(false, tag, runnable)
    }

    /**
     * Sends a runnable to the executor service.
     *
     * @param reportsOnDiagnostic weather or not the error has to be tracked with diagnostic feature
     * @param tag string indicating the source of the runnable for logging purposes in case of
     * exceptions raised by the runnable
     * @param runnable the runnable to be queued
     */
    fun execute(reportsOnDiagnostic: Boolean, tag: String?, runnable: Runnable?) {
        val loggerTag: String = tag ?: "Source not provided"
        execute(runnable) { t: Throwable? ->
            var message = t?.localizedMessage
            if (message == null) {
                message = "No message provided."
            }
            if (reportsOnDiagnostic) {
                Logger.track(loggerTag, message, t)
            } else {
                Logger.e(loggerTag, message, t)
            }
        }
    }

    /**
     * Sends a runnable to the executor service.
     *
     * @param runnable the runnable to be queued
     * @param exceptionHandler the handler of exception raised by the runnable
     */
    @JvmStatic
    fun execute(runnable: Runnable?, exceptionHandler: ExceptionHandler?) {
        val executor = getExecutor()
        try {
            executor.execute {
                try {
                    runnable?.run()
                } catch (t: Throwable) {
                    exceptionHandler?.handle(t)
                }
            }
        } catch (e: Exception) {
            exceptionHandler?.handle(e)
        }
    }

    /**
     * Sends a callable to the executor service and
     * returns a Future.
     *
     * @param callable the callable to be queued
     * @return the future object to be queried
     */
    fun futureCallable(callable: Callable<*>): Future<*> {
        return getExecutor().submit(callable)
    }

    /**
     * Shuts the executor service down and resets
     * the executor to a null state.
     */
    fun shutdown(): ExecutorService? {
        if (executor != null) {
            executor!!.shutdown()
            val es = executor
            executor = null
            return es
        }
        return null
    }

    /**
     * Handle exceptions raised by a Runnable
     */
    fun interface ExceptionHandler {
        fun handle(t: Throwable?)
    }
}
