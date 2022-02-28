/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
import android.content.Context;
import android.os.Build;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;

import java.util.HashMap;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = ActivityLifecycleHandler.class.getSimpleName();

    private static ActivityLifecycleHandler sharedInstance;

    @NonNull
    public synchronized static ActivityLifecycleHandler getInstance(@NonNull Context context) {
        if (sharedInstance == null) {
            sharedInstance = new ActivityLifecycleHandler(context);
        }
        return sharedInstance;
    }

    private ActivityLifecycleHandler(@NonNull Context context) {
        Application application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Logger.d(TAG, "Auto screenview occurred - activity has resumed");
        try {
            ScreenView event = ScreenView.buildWithActivity(activity);
            Map<String, Object> notificationData = new HashMap<String, Object>();
            notificationData.put("event", event);
            NotificationCenter.postNotification("SnowplowScreenView", notificationData);
        } catch (Exception e) {
            Logger.e(TAG, "Method onActivityResumed raised an exception: %s", e);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @Nullable Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
