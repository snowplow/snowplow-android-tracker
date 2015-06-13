package com.snowplowanalytics.snowplowtrackerdemo.utils;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Emitter;

import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DemoUtils {

    public static final String namespace = "SnowplowAndroidTrackerDemo";
    public static final String appId = "DemoID";

    // Tracker Utils

    public static Tracker getAndroidTrackerLite(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterLite(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerLite(emitter, subject);
    }

    public static Tracker getAndroidTrackerRx(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterRx(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerRx(emitter, subject);
    }

    private static Tracker getTrackerLite(Emitter emitter, Subject subject) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId,
                com.snowplowanalytics.snowplow.tracker.lite.Tracker.class)
                .level(LogLevel.VERBOSE)
                .base64(false)
                .platform(DevicePlatforms.Mobile)
                .subject(subject)
                .build();
    }

    private static Tracker getTrackerRx(Emitter emitter, Subject subject) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId,
                com.snowplowanalytics.snowplow.tracker.rx.Tracker.class)
                .level(LogLevel.VERBOSE)
                .base64(false)
                .platform(DevicePlatforms.Mobile)
                .subject(subject)
                .build();
    }

    private static Emitter getEmitterLite(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.lite.Emitter.class)
                .callback(callback)
                .build();
    }

    private static Emitter getEmitterRx(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.rx.Emitter.class)
                .callback(callback)
                .build();
    }

    private static Subject getSubject(Context context) {
        return new Subject.SubjectBuilder().context(context).build();
    }

    // Executor Utils

    public static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}
