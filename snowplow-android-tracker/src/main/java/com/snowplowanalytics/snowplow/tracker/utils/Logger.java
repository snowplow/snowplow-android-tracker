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

import com.snowplowanalytics.snowplow.tracker.LogLevel;
import android.util.Log;

/**
 * Custom logger class to easily manage debug mode and appending of 'SnowplowTracker->' to the log TAG.
 */
public class Logger {

    private static int level = 0;

    public static void i(String tag, String msg, Throwable e, Object... args) {
        if (level >= 1) {
            if (e == null) {
                Log.i(getTag(tag), getMessage(msg, args), e);
            } else {
                Log.i(getTag(tag), getMessage(msg, args));
            }
        }
    }

    public static void e(String tag, String msg, Throwable e, Object... args) {
        if (level >= 2) {
            if (e == null) {
                Log.e(getTag(tag), getMessage(msg, args), e);
            } else {
                Log.e(getTag(tag), getMessage(msg, args));
            }
        }
    }

    public static void d(String tag, String msg, Throwable e, Object... args) {
        if (level >= 3) {
            if (e == null) {
                Log.d(getTag(tag), getMessage(msg, args), e);
            } else {
                Log.d(getTag(tag), getMessage(msg, args));
            }
        }
    }

    public static void v(String tag, String msg, Throwable e, Object... args) {
        if (level >= 4) {
            if (e == null) {
                Log.v(getTag(tag), getMessage(msg, args), e);
            } else {
                Log.v(getTag(tag), getMessage(msg, args));
            }
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
     * Gets the current thread name.
     *
     * @return the thread name
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
