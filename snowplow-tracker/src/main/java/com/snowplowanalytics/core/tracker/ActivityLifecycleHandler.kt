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
package com.snowplowanalytics.core.tracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo

import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.snowplow.event.ScreenView.Companion.buildWithActivity

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ActivityLifecycleHandler private constructor(context: Context) :
    Application.ActivityLifecycleCallbacks {

    init {
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d(TAG, "Auto screenview occurred - activity has resumed")
        try {
            val event = buildWithActivity(activity)
            val notificationData: MutableMap<String, Any> = HashMap()
            notificationData["event"] = event
            postNotification("SnowplowScreenView", notificationData)
        } catch (e: Exception) {
            Logger.e(TAG, "Method onActivityResumed raised an exception: %s", e)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private val TAG = ActivityLifecycleHandler::class.java.simpleName
        private var sharedInstance: ActivityLifecycleHandler? = null
        
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): ActivityLifecycleHandler {
            if (sharedInstance == null) {
                sharedInstance = ActivityLifecycleHandler(context)
            }
            return sharedInstance!!
        }
    }
}
