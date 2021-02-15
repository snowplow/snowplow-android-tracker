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

package com.snowplowanalytics.snowplow.internal.session;

import android.annotation.TargetApi;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Lifecycle;
import android.os.Build;


import com.snowplowanalytics.snowplow.event.Unstructured;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ProcessObserver implements LifecycleObserver {
    private static final String TAG = ProcessObserver.class.getSimpleName();
    private static boolean isInBackground = true;
    private static final AtomicInteger foregroundIndex = new AtomicInteger(0);
    private static final AtomicInteger backgroundIndex = new AtomicInteger(0);
    private static boolean isHandlerPaused = false;
    private static List<SelfDescribingJson> lifecycleContexts = null;

    public ProcessObserver(@Nullable List<SelfDescribingJson> contexts) {
        lifecycleContexts = contexts;
    }

    public ProcessObserver() {}

    public static void setLifecycleContexts(@Nullable List<SelfDescribingJson> contexts) {
        lifecycleContexts = contexts;
    }

    @Nullable
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
                Session session = tracker.getSession();
                if (session != null) {
                    session.setIsBackground(false);
                    session.setForegroundIndex(index);
                }

                // Send Foreground Event
                if (tracker.getLifecycleEvents()) {
                    Map<String, Object> data = new HashMap<>();
                    Util.addToMap(Parameters.APP_FOREGROUND_INDEX, index, data);
                    tracker.track(new Unstructured(new SelfDescribingJson(TrackerConstants.APPLICATION_FOREGOUND_SCHEMA, data))
                            .contexts(lifecycleContexts)
                    );
                }
            } catch (Exception e) {
                Logger.e(TAG, "Method onEnterForeground raised an exception: %s", e);
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
                Session session = tracker.getSession();
                if (session != null) {
                    session.setIsBackground(true);
                    session.setBackgroundIndex(index);
                }

                // Send Background Event
                if (tracker.getLifecycleEvents()) {
                    Map<String, Object> data = new HashMap<>();
                    Util.addToMap(Parameters.APP_BACKGROUND_INDEX, index, data);
                    tracker.track(new Unstructured(new SelfDescribingJson(TrackerConstants.APPLICATION_BACKGROUND_SCHEMA, data))
                            .contexts(lifecycleContexts)
                    );
                }
            } catch (Exception e) {
                Logger.e(TAG, "Method onEnterBackground raised an exception: %s", e);
            }
        }
    }
}
