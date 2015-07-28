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

package com.snowplowanalytics.snowplow.tracker.utils;

import android.util.Log;

/**
 * Custom logger class to easily manage debug mode and appending
 * of 'SnowplowTracker' to the log TAG as well as logging the
 * Thread.
 */
public class Logger {

    private static int level = 0;

    /**
     * Error Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void e(String tag, String msg, Object... args) {
        if (level >= 1) {
            Log.e(getTag(tag), getMessage(msg, args));
        }
    }

    /**
     * Debug Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void d(String tag, String msg, Object... args) {
        if (level >= 2) {
            Log.d(getTag(tag), getMessage(msg, args));
        }
    }

    /**
     * Verbose Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void v(String tag, String msg, Object... args) {
        if (level >= 3) {
            Log.v(getTag(tag), getMessage(msg, args));
        }
    }

    /**
     * Returns a formatted logging String
     *
     * @param msg The message to log
     * @param args Any extra args to log
     * @return the formatted message
     */
    private static String getMessage(String msg, Object... args) {
        return getThread() + "|" + String.format(msg, args);
    }

    /**
     * Returns the updated tag.
     *
     * @param tag the tag to be appended to
     * @return the appended tag
     */
    private static String getTag(String tag) {
        return "SnowplowTracker->" + tag;
    }

    /**
     * Returns the name of the current
     * thread.
     *
     * @return the threads name
     */
    private static String getThread() {
        return Thread.currentThread().getName();
    }

    /**
     * Updates the logging level.
     *
     * @param newLevel The new log-level to use
     */
    public static void updateLogLevel(LogLevel newLevel) {
        level = newLevel.getLevel();
    }
}
