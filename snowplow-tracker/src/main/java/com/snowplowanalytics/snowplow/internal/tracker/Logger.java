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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.DiagnosticLogger;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

/**
 * Custom logger class to easily manage debug mode and appending
 * of 'SnowplowTracker' to the log TAG as well as logging the
 * Thread.
 */
public class Logger {

    private static final String TAG = Logger.class.getSimpleName();
    private static DiagnosticLogger errorLogger;
    private static LoggerDelegate delegate = new DefaultLoggerDelegate();
    private static int level = 0;

    /**
     * Updates the logging level.
     *
     * @param newLevel The new log-level to use
     */
    public static void updateLogLevel(@NonNull LogLevel newLevel) {
        level = newLevel.getLevel();
    }

    /**
     * Set the error logger used to track internal errors.
     *
     * @param errorLogger The error logger delegate in the app.
     */
    public static void setErrorLogger(@NonNull DiagnosticLogger errorLogger) {
        Logger.errorLogger = errorLogger;
    }

    /**
     * Set the logger delegate that receive logs from the tracker.
     *
     * @param delegate The app logger delegate.
     */
    public static void setDelegate(@Nullable LoggerDelegate delegate) {
        if (delegate != null) {
            Logger.delegate = delegate;
        } else {
            Logger.delegate = new DefaultLoggerDelegate();
        }
    }

    @Nullable
    public static LoggerDelegate getDelegate() {
        return delegate;
    }

    // -- Log methods

    /**
     * Diagnostic Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void track(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
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
    public static void e(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (level >= 1) {
            String source = getTag(tag);
            String message = getMessage(msg, args);
            delegate.error(source, message);
        }
    }

    /**
     * Debug Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void d(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (level >= 2) {
            String source = getTag(tag);
            String message = getMessage(msg, args);
            delegate.debug(source, message);
        }
    }

    /**
     * Verbose Level Logging
     *
     * @param tag the log tag
     * @param msg the log message
     * @param args extra arguments to be formatted
     */
    public static void v(@NonNull String tag, @NonNull String msg, @Nullable Object... args) {
        if (level >= 3) {
            String source = getTag(tag);
            String message = getMessage(msg, args);
            delegate.verbose(source, message);
        }
    }

    /**
     * Returns a formatted logging String
     *
     * @param msg The message to log
     * @param args Any extra args to log
     * @return the formatted message
     */
    @NonNull
    private static String getMessage(@NonNull String msg, @Nullable Object... args) {
        return getThread() + "|" + String.format(msg, args);
    }

    /**
     * Returns the updated tag.
     *
     * @param tag the tag to be appended to
     * @return the appended tag
     */
    @NonNull
    private static String getTag(@NonNull String tag) {
        return "SnowplowTracker->" + tag;
    }

    /**
     * Returns the name of the current
     * thread.
     *
     * @return the threads name
     */
    @NonNull
    private static String getThread() {
        return Thread.currentThread().getName();
    }

}

/**
 * Default internal logger delegate
 */
class DefaultLoggerDelegate implements LoggerDelegate {
    @Override
    public void error(String tag, String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void debug(String tag, String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void verbose(String tag, String msg) {
        Log.v(tag, msg);
    }
}
