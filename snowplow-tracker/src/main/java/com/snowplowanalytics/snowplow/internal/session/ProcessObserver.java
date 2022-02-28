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

package com.snowplowanalytics.snowplow.internal.session;

import android.annotation.TargetApi;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.util.HashMap;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ProcessObserver implements LifecycleObserver {
    private static final String TAG = ProcessObserver.class.getSimpleName();

    private enum InitializationState {
        NONE,
        IN_PROGRESS,
        COMPLETE
    }

    private static InitializationState initializationState = InitializationState.NONE;

    public synchronized static void initialize(@NonNull Context context) {
        if (initializationState == InitializationState.NONE) {
            initializationState = InitializationState.IN_PROGRESS;
            // addObserver must execute on the mainThread
            Handler mainHandler = new Handler(context.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProcessLifecycleOwner.get().getLifecycle().addObserver(new ProcessObserver());
                        initializationState = InitializationState.COMPLETE;
                    } catch (NoClassDefFoundError e) {
                        initializationState = InitializationState.NONE;
                        Logger.e(TAG,"Class 'ProcessLifecycleOwner' not found. The tracker can't track lifecycle events.");
                    }
                }
            });
        }
    }

    private ProcessObserver() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        Logger.d(TAG, "App enter foreground");
        try {
            Map<String, Object> notificationData = new HashMap<String, Object>();
            notificationData.put("isForeground", Boolean.TRUE);
            NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);
        } catch (Exception e) {
            Logger.e(TAG, "Method onEnterForeground raised an exception: %s", e);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        Logger.d(TAG, "App enter background");
        try {
            Map<String, Object> notificationData = new HashMap<String, Object>();
            notificationData.put("isForeground", Boolean.FALSE);
            NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData);
        } catch (Exception e) {
            Logger.e(TAG, "Method onEnterBackground raised an exception: %s", e);
        }
    }
}
