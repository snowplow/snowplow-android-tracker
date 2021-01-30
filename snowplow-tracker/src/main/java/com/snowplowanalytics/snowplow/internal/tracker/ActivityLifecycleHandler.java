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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;


import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = ActivityLifecycleHandler.class.getSimpleName();
    private static List<SelfDescribingJson> contexts = new ArrayList<SelfDescribingJson>();

    public ActivityLifecycleHandler(List<SelfDescribingJson> contexts) {
        contexts = contexts;
    }

    public ActivityLifecycleHandler() {}

    public static void setContexts(List<SelfDescribingJson> contexts) {
        contexts = contexts;
    }

    public static List<SelfDescribingJson> getLifecycleContexts() {
        return contexts;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        Logger.d(TAG, "Auto screenview occurred - activity has resumed");
        try {
            ScreenView event = ScreenView.buildWithActivity(activity);
            Tracker.instance().track(event);
        } catch (Exception e) {
            Logger.e(TAG, "Method onActivityResumed raised an exception: %s", e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
