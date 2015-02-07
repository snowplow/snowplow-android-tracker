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

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;

/**
 * Custom logger class to easily manage debug
 * mode and appending of 'SnowplowTracker->'
 * to the log TAG.
 */
public class Logger {

    public static void ifDebug(String tag, String msg, Object... args) {
        if (TrackerConstants.DEBUG_MODE) {
            Log.d(getTag(tag), getThread() + "|" + String.format(msg, args));
        }
    }

    public static void ifDebug(String tag, String msg, Throwable e, Object... args) {
        if (TrackerConstants.DEBUG_MODE) {
            Log.d(getTag(tag), getThread() + "|" + String.format(msg, args), e);
        }
    }

    public static void i(String tag, String msg) {
        Log.i(getTag(tag), msg);
    }

    public static void i(String tag, String msg, Throwable e) {
        Log.i(getTag(tag), msg, e);
    }

    public static void e(String tag, String msg) {
        Log.e(getTag(tag), msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        Log.e(getTag(tag), msg, e);
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
}
