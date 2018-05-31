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
import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

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
import java.util.List;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class LifecycleHandler implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {

    private static final String TAG = LifecycleHandler.class.getSimpleName();
    private static boolean isInBackground = false;
    private static AtomicInteger foregroundIndex = new AtomicInteger(0);
    private static AtomicInteger backgroundIndex = new AtomicInteger(0);
    private static boolean isHandlerPaused = false;
    private static List<SelfDescribingJson> lifecycleContext = null;

    public LifecycleHandler(List<SelfDescribingJson> context) {
        lifecycleContext = context;
    }

    public LifecycleHandler() {}

    public static void setLifecycleContext(List<SelfDescribingJson> context) {
        lifecycleContext = context;
    }

    public static List<SelfDescribingJson> getLifecycleContext() {
        return lifecycleContext;
    }

    public static void pauseHandler() {
        isHandlerPaused = true;
    }

    public static void resumeHandler() {
        isHandlerPaused = false;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {
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

                    if (lifecycleContext != null) {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_FOREGOUND_SCHEMA, data))
                                .customContext(lifecycleContext)
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

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void onConfigurationChanged(Configuration configuration) {}

    @Override
    public void onLowMemory() {}

    @Override
    public void onTrimMemory(int i) {
        if (i == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && !isHandlerPaused) {
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

                    if (lifecycleContext != null) {
                        tracker.track(SelfDescribing.builder()
                                .eventData(new SelfDescribingJson(TrackerConstants.APPLICATION_BACKGROUND_SCHEMA, data))
                                .customContext(lifecycleContext)
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
