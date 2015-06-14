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

public class DemoUtils {

    public static final String namespace = "SnowplowAndroidTrackerDemo";
    public static final String appId = "DemoID";

    // Tracker Utils

    public static Tracker getAndroidTrackerClassic(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterClassic(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerClassic(emitter, subject);
    }

    public static Tracker getAndroidTrackerRx(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterRx(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerRx(emitter, subject);
    }

    private static Tracker getTrackerClassic(Emitter emitter, Subject subject) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId,
                com.snowplowanalytics.snowplow.tracker.classic.Tracker.class)
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

    private static Emitter getEmitterClassic(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.classic.Emitter.class)
                .callback(callback)
                .tick(1)
                .build();
    }

    private static Emitter getEmitterRx(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.rx.Emitter.class)
                .callback(callback)
                .tick(1)
                .build();
    }

    private static Subject getSubject(Context context) {
        return new Subject
                .SubjectBuilder()
                .context(context)
                .build();
    }

    // Executor Utils

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}
