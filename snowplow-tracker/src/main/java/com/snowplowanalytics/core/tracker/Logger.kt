/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.tracker

import android.util.Log
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.snowplow.event.TrackerError
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate

/**
 * Custom logger class to easily manage debug mode and appending
 * of 'SnowplowTracker' to the log TAG as well as logging the
 * Thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object Logger {
    private val TAG = Logger::class.java.simpleName
    private var level = 0
    
    var delegate: LoggerDelegate? = DefaultLoggerDelegate()
        /**
         * Set the logger delegate that receive logs from the tracker.
         *
         * @param delegate The app logger delegate.
         */
        set(delegate) {
            field = delegate ?: DefaultLoggerDelegate()
        }

    /**
     * Updates the logging level.
     *
     * @param newLevel The new log-level to use
     */
    @JvmStatic
    fun updateLogLevel(newLevel: LogLevel) {
        level = newLevel.level
    }
    
    // -- Log methods
    
    /**
     * Diagnostic Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    @JvmStatic
    fun track(tag: String, msg: String, vararg args: Any?) {
        e(tag, msg, *args)
        try {
            var throwable: Throwable? = null
            for (arg in args) {
                if (Throwable::class.java.isInstance(arg)) {
                    throwable = arg as? Throwable?
                    break
                }
            }
            val event = TrackerError(tag, getMessage(msg, *args), throwable)
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData["event"] = event
            postNotification("SnowplowTrackerDiagnostic", notificationData)
        } catch (e: Exception) {
            v(TAG, "Error logger can't report the error: $e")
        }
    }

    /**
     * Error Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    @JvmStatic
    fun e(tag: String, msg: String, vararg args: Any?) {
        if (level >= 1) {
            val source = getTag(tag)
            val message = getMessage(msg, *args)
            delegate?.error(source, message)
        }
    }

    /**
     * Debug Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    @JvmStatic
    fun d(tag: String, msg: String, vararg args: Any?) {
        if (level >= 2) {
            val source = getTag(tag)
            val message = getMessage(msg, *args)
            delegate?.debug(source, message)
        }
    }

    /**
     * Verbose Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    @JvmStatic
    fun v(tag: String, msg: String, vararg args: Any?) {
        if (level >= 3) {
            val source = getTag(tag)
            val message = getMessage(msg, *args)
            delegate?.verbose(source, message)
        }
    }

    /**
     * Returns a formatted logging String
     *
     * @param msg The message to log
     * @param args Any extra args to log
     * @return the formatted message
     */
    private fun getMessage(msg: String, vararg args: Any?): String {
        return thread() + "|" + String.format(msg, *args)
    }

    /**
     * Returns the updated tag.
     *
     * @param tag the tag to be appended to
     * @return the appended tag
     */
    private fun getTag(tag: String): String {
        return "SnowplowTracker->$tag"
    }

    /**
     * Returns the name of the current
     * thread.
     *
     * @return the thread's name
     */
    private fun thread(): String {
        return Thread.currentThread().name
    }
}

/**
 * Default internal logger delegate
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class DefaultLoggerDelegate : LoggerDelegate {
    override fun error(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    override fun debug(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun verbose(tag: String, msg: String) {
        Log.v(tag, msg)
    }
}
