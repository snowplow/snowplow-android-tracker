/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.session

import android.content.Context
import android.os.Handler
import androidx.annotation.RestrictTo
import androidx.lifecycle.*
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.NotificationCenter

import kotlin.Any
import kotlin.Exception
import kotlin.String


@RestrictTo(RestrictTo.Scope.LIBRARY)
class ProcessObserver private constructor() : DefaultLifecycleObserver {
    private enum class InitializationState {
        NONE, IN_PROGRESS, COMPLETE
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Logger.d(TAG, "App enter foreground")
        try {
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData["isForeground"] = true
            NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData)
        } catch (e: Exception) {
            Logger.e(TAG, "Method onEnterForeground raised an exception: %s", e)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Logger.d(TAG, "App enter background")
        try {
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData["isForeground"] = false
            NotificationCenter.postNotification("SnowplowLifecycleTracking", notificationData)
        } catch (e: Exception) {
            Logger.e(TAG, "Method onEnterBackground raised an exception: %s", e)
        }
    }

    companion object {
        private val TAG = ProcessObserver::class.java.simpleName
        private var initializationState = InitializationState.NONE
        
        @JvmStatic
        @Synchronized
        fun initialize(context: Context) {
            if (initializationState == InitializationState.NONE) {
                initializationState = InitializationState.IN_PROGRESS
                // addObserver must execute on the mainThread
                val mainHandler = Handler(context.mainLooper)
                mainHandler.post {
                    try {
                        ProcessLifecycleOwner.get().lifecycle.addObserver(ProcessObserver())
                        initializationState = InitializationState.COMPLETE
                    } catch (e: NoClassDefFoundError) {
                        initializationState = InitializationState.NONE
                        Logger.e(
                            TAG,
                            "Class 'ProcessLifecycleOwner' not found. The tracker can't track lifecycle events."
                        )
                    }
                }
            }
        }
    }
}
