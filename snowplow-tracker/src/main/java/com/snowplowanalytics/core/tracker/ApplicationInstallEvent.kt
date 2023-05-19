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
package com.snowplowanalytics.core.tracker

import android.content.Context
import androidx.preference.PreferenceManager
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import java.util.*

/**
 * An event tracked on the first launch of the app in case install autotracking is enabled.
 * It is accompanied by an install referrer entity if available.
 */
class ApplicationInstallEvent : AbstractSelfDescribing() {

    override val schema: String
        get() = TrackerConstants.SCHEMA_APPLICATION_INSTALL

    override val dataPayload: Map<String, Any?>
        get() = emptyMap()

    companion object {
        private val TAG = ApplicationInstallEvent::class.java.simpleName

        /**
         * Asynchronous function that tracks an `application_install` event if it wasn't tracked yet.
         * @param context the Android context
         */
        fun trackIfFirstLaunch(context: Context) {
            Executor.execute(TAG) {
                if (isNewInstall(context)) {
                    sendInstallEvent(context)
                }
            }
        }

        private fun isNewInstall(context: Context): Boolean {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            // if the value was missing in sharedPreferences, we're assuming this is a new install
            return sharedPreferences.getString(TrackerConstants.INSTALLED_BEFORE, null) == null;
        }

        private fun sendInstallEvent(context: Context) {
            val event = ApplicationInstallEvent()

            // add install referrer entity if available
            InstallReferrerDetails.fetch(context) { referrer ->
                referrer?.let { event.entities.add(it) }

                val notificationData: MutableMap<String, Any> = HashMap()
                notificationData["event"] = event
                postNotification("SnowplowInstallTracking", notificationData)

                saveInstallTrackedInfo(context)
            }
        }

        /**
         * Save install event tracked info to shared preferences
         */
        private fun saveInstallTrackedInfo(context: Context) {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()
            editor?.putString(TrackerConstants.INSTALLED_BEFORE, "YES")
            editor?.apply()
        }
    }
}
