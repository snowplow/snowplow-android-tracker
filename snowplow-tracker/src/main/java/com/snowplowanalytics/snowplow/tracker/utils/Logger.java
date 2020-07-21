/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.snowplow.tracker.DiagnosticLogger;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;

/**
 * Custom logger class to easily manage debug mode and appending
 * of 'SnowplowTracker' to the log TAG as well as logging the
 * Thread.
 */
public class Logger {

    private static String TAG = Logger.class.getSimpleName();
    private static DiagnosticLogger errorLogger;
    private static LoggerDelegate delegate;
    private static int level = 0;

    /**
     * Updates the logging level.
     *
     * @param newLevel The new log-level to use
     */
    public static void updateLogLevel(LogLevel newLevel) {
        level = newLevel.getLevel();
    }

    /**
     * Set the error logger used to track internal errors.
     *
     * @param errorLogger The error logger delegate in the app.
     */
    public static void setErrorLogger(DiagnosticLogger errorLogger) {
        Logger.errorLogger = errorLogger;
    }

    /**
     * Set the logger delegate that receive logs from the tracker.
     *
     * @param delegate The app logger delegate.
     */
    public static void setDelegate(LoggerDelegate delegate) {
        Logger.delegate = delegate;
    }

    // -- Log methods

    /**
     * Diagnostic Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void track(String tag, String msg, Object... args) {
        Logger.e(tag, msg, args);
        if (errorLogger != null) {
            try {
                Throwable throwable = null;
                for (Object arg : args) {
                    if (Throwable.class.isInstance(arg)) {
                        throwable = (Throwable) arg;
                        break;
                    }
                }
                errorLogger.log(tag, getMessage(msg, args), throwable);
            } catch (Exception e) {
                Logger.v(TAG, "Error logger can't report the error: " + e);
            }
        }
    }

    /**
     * Error Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void e(String tag, String msg, Object... args) {
        if (level >= 1) {
            String source = getTag(tag);
            String message = getMessage(msg, args);
            if (delegate != null) {
                delegate.error(source, message);
            } else {
                Log.e(source, message);
            }
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
            String source = getTag(tag);
            String message = getMessage(msg, args);
            if (delegate != null) {
                delegate.debug(source, message);
            } else {
                Log.d(source, message);
            }
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
            String source = getTag(tag);
            String message = getMessage(msg, args);
            if (delegate != null) {
                delegate.verbose(source, message);
            } else {
                Log.v(source, message);
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
     * Returns the name of the current
     * thread.
     *
     * @return the threads name
     */
    private static String getThread() {
        return Thread.currentThread().getName();
    }

}
