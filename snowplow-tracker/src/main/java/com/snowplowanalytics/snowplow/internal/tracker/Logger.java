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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.event.TrackerError;
import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom logger class to easily manage debug mode and appending
 * of 'SnowplowTracker' to the log TAG as well as logging the
 * Thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Logger {

    private static final String TAG = Logger.class.getSimpleName();
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
        try {
            Throwable throwable = null;
            for (Object arg : args) {
                if (Throwable.class.isInstance(arg)) {
                    throwable = (Throwable) arg;
                    break;
                }
            }
            TrackerError event = new TrackerError(tag, getMessage(msg, args), throwable);
            Map<String, Object> notificationData = new HashMap<String, Object>();
            notificationData.put("event", event);
            NotificationCenter.postNotification("SnowplowTrackerDiagnostic", notificationData);
        } catch (Exception e) {
            Logger.v(TAG, "Error logger can't report the error: " + e);
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
@RestrictTo(RestrictTo.Scope.LIBRARY)
class DefaultLoggerDelegate implements LoggerDelegate {
    @Override
    public void error(@NonNull String tag, @NonNull String msg) {
        Log.e(tag, msg);
    }

    @Override
    public void debug(@NonNull String tag, @NonNull String msg) {
        Log.d(tag, msg);
    }

    @Override
    public void verbose(@NonNull String tag, @NonNull String msg) {
        Log.v(tag, msg);
    }
}
