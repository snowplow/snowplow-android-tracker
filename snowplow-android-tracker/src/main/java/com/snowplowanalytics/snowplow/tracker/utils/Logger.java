package com.snowplowanalytics.snowplow.tracker.utils;

import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;

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

    public static void w(String tag, String msg) {
        Log.w(getTag(tag), msg);
    }

    public static void w(String tag, String msg, Throwable e) {
        Log.w(getTag(tag), msg, e);
    }

    public static void e(String tag, String msg) {
        Log.e(getTag(tag), msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        Log.e(getTag(tag), msg, e);
    }

    private static String getTag(String tag) {
        return "SnowplowTracker->" + tag;
    }

    private static String getThread() {
        return Thread.currentThread().getName();
    }
}
