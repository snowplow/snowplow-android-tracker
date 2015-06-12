package com.snowplowanalytics.snowplowtrackerdemo.utils;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Emitter;

import android.content.Context;

public class DemoUtils {

    public static final String namespace = "SnowplowAndroidTrackerDemo";
    public static final String appId = "DemoID";

    public static Tracker getTracker(Emitter emitter, Subject subject) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId)
                .level(LogLevel.VERBOSE)
                .base64(false)
                .platform(DevicePlatforms.Mobile)
                .subject(subject)
                .build();
    }

    public static Emitter getEmitterLite(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.lite.Emitter.class)
                .callback(callback)
                .build();
    }

    public static Emitter getEmitterRx(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.rx.Emitter.class)
                .callback(callback)
                .build();
    }

    public static Subject getSubject(Context context) {
        return new Subject.SubjectBuilder().context(context).build();
    }
}
