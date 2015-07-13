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
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Emitter;

import android.content.Context;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to build the Trackers and
 * to hold a static executor.
 */
public class DemoUtils {

    public static final String namespace = "SnowplowAndroidTrackerDemo";
    public static final String appId = "DemoID";

    // Tracker Utils

    /**
     * Returns a Classic Tracker
     *
     * @param context the application context
     * @param callback the emitter callback
     * @return a new Classic Tracker
     */
    public static Tracker getAndroidTrackerClassic(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterClassic(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerClassic(emitter, subject, context);
    }

    /**
     * Returns an RxJava Tracker
     *
     * @param context the application context
     * @param callback the emitter callback
     * @return a new RxJava Tracker
     */
    public static Tracker getAndroidTrackerRx(Context context, RequestCallback callback) {
        Emitter emitter = DemoUtils.getEmitterRx(context, callback);
        Subject subject = DemoUtils.getSubject(context);
        return DemoUtils.getTrackerRx(emitter, subject, context);
    }

    /**
     * Returns a Classic Tracker
     *
     * @param emitter a Classic emitter
     * @param subject the tracker subject
     * @return a new Classic Tracker
     */
    private static Tracker getTrackerClassic(Emitter emitter, Subject subject, Context context) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId, context,
                com.snowplowanalytics.snowplow.tracker.classic.Tracker.class)
                .level(LogLevel.VERBOSE)
                .base64(false)
                .platform(DevicePlatforms.Mobile)
                .subject(subject)
                .build();
    }

    /**
     * Returns an RxJava Tracker
     *
     * @param emitter an RxJava emitter
     * @param subject the tracker subject
     * @return a new RxJava Tracker
     */
    private static Tracker getTrackerRx(Emitter emitter, Subject subject, Context context) {
        return new Tracker.TrackerBuilder(emitter, namespace, appId, context,
                com.snowplowanalytics.snowplow.tracker.rx.Tracker.class)
                .level(LogLevel.VERBOSE)
                .base64(false)
                .platform(DevicePlatforms.Mobile)
                .subject(subject)
                .build();
    }

    /**
     * Returns a Classic Emitter
     *
     * @param context the application context
     * @param callback the emitter callback
     * @return a new Classic Emitter
     */
    private static Emitter getEmitterClassic(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.classic.Emitter.class)
                .callback(callback)
                .tick(1)
                .build();
    }

    /**
     * Returns an RxJava Emitter
     *
     * @param context the application context
     * @param callback the emitter callback
     * @return a new RxJava Emitter
     */
    private static Emitter getEmitterRx(Context context, RequestCallback callback) {
        return new Emitter.EmitterBuilder("", context,
                com.snowplowanalytics.snowplow.tracker.rx.Emitter.class)
                .callback(callback)
                .tick(1)
                .build();
    }

    /**
     * Returns a Subject Object
     *
     * @param context the application context
     * @return a new subject
     */
    private static Subject getSubject(Context context) {
        return new Subject
                .SubjectBuilder()
                .context(context)
                .build();
    }

    // Executor Utils

    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Executes a runnable on the executor service
     *
     * @param runnable a new task
     */
    public static void execute(Runnable runnable) {
        executor.execute(runnable);
    }

    /**
     * Executes a repeating runnable
     *
     * @param runnable a new task
     * @param initDelay the delay before polling
     * @param delay the delay between polls
     * @param timeUnit the timeunit for the delays
     */
    public static void scheduleRepeating(Runnable runnable, long initDelay, long delay, TimeUnit timeUnit) {
        executor.scheduleAtFixedRate(runnable, initDelay, delay, timeUnit);
    }

    /**
     * Shuts the executor down and resets it.
     */
    public static void resetExecutor() {
        executor.shutdown();
        executor = Executors.newSingleThreadScheduledExecutor();
    }
}
