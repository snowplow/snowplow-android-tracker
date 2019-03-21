/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.tracker;

import android.annotation.TargetApi;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.Lifecycle;
import android.os.Build;


import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ProcessObserver implements LifecycleObserver {
    private static final String TAG = ProcessObserver.class.getSimpleName();
    private static boolean isInBackground = false;
    private static AtomicInteger foregroundIndex = new AtomicInteger(0);
    private static AtomicInteger backgroundIndex = new AtomicInteger(0);
    private static boolean isHandlerPaused = false;
    private static List<SelfDescribingJson> lifecycleContexts = null;

    public ProcessObserver(List<SelfDescribingJson> contexts) {
        lifecycleContexts = contexts;
    }

    public ProcessObserver() {}

    public static void setLifecycleContexts(List<SelfDescribingJson> contexts) {
        lifecycleContexts = contexts;
    }

    public static List<SelfDescribingJson> getLifecycleContexts() {
        return lifecycleContexts;
    }

    public static void pauseHandler() {
        isHandlerPaused = true;
    }

    public static void resumeHandler() {
        isHandlerPaused = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public static void onEnterForeground() {
        if (isInBackground && !isHandlerPaused){
            Logger.d(TAG, "Application is in the foreground");
            isInBackground = false;

            try {
                Tracker tracker = Tracker.instance();
                int index = foregroundIndex.addAndGet(1);

                // Update Session
                if (tracker.getSession() != null) {
                    tracker.getSession().setIsBackground(false);
                }

                // Send Foreground Event
                if (tracker.getLifecycleEvents()) {
                    Map<String, Object> data = new HashMap<>();
                    Util.addToMap(Parameters.APP_FOREGROUND_INDEX, index, data);

                    if (lifecycleContexts != null) {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_FOREGOUND_SCHEMA, data))
                                .customContext(lifecycleContexts)
                                .build()
                        );
                    } else {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_FOREGOUND_SCHEMA, data))
                                .build()
                        );
                    }
                }
            } catch (Exception e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public static void onEnterBackground() {
        if (!isHandlerPaused) {
            Logger.d(TAG, "Application is in the background");
            isInBackground = true;

            try {
                Tracker tracker = Tracker.instance();
                int index = backgroundIndex.addAndGet(1);

                // Update Session
                if (tracker.getSession() != null) {
                    tracker.getSession().setIsBackground(true);
                }

                // Send Background Event
                if (tracker.getLifecycleEvents()) {
                    Map<String, Object> data = new HashMap<>();
                    Util.addToMap(Parameters.APP_BACKGROUND_INDEX, index, data);

                    if (lifecycleContexts != null) {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_BACKGROUND_SCHEMA, data))
                                .customContext(lifecycleContexts)
                                .build()
                        );
                    } else {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_BACKGROUND_SCHEMA, data))
                                .build()
                        );
                    }
                }
            } catch (Exception e) {
                Logger.e(TAG, e.getMessage());
            }
        }
    }
}
