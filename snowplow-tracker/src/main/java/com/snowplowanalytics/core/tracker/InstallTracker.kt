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
import android.content.SharedPreferences
import android.os.AsyncTask
import androidx.preference.PreferenceManager
import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

/**
 * Class used to keep track of install state of app.
 * If a file does not exist, the tracker will send an `application_install` event.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class InstallTracker private constructor(context: Context) {
    private var isNewInstall: Boolean? = null
    private var sharedPreferences: SharedPreferences? = null

    init {
        SharedPreferencesTask().execute(context)
    }

    private inner class SharedPreferencesTask : AsyncTask<Context?, Void?, Boolean>() {
        
        override fun doInBackground(vararg params: Context?): Boolean? {
            sharedPreferences = params[0]?.let { PreferenceManager.getDefaultSharedPreferences(it) }
            isNewInstall = if (sharedPreferences?.getString(TrackerConstants.INSTALLED_BEFORE, null) == null) {
                // mark the install if there's no value
                val editor = sharedPreferences?.edit()
                editor?.putString(TrackerConstants.INSTALLED_BEFORE, "YES")
                editor?.putLong(TrackerConstants.INSTALL_TIMESTAMP, Calendar.getInstance().timeInMillis)
                editor?.apply()
                // since the value was missing in sharedPreferences, we're assuming this is a new install
                true
            } else {
                // if there's an INSTALLED_BEFORE record in sharedPreferences - someone has been there!
                false
            }
            return isNewInstall
        }

        override fun onPostExecute(isNewInstall: Boolean) {
            val installTimestamp = sharedPreferences?.getLong(TrackerConstants.INSTALL_TIMESTAMP, 0)
            // We send the installEvent if it's a new installed app but in case the tracker hasn't been able
            // to send the event before we can retry checking if INSTALL_TIMESTAMP was already removed.
            installTimestamp?.let {
                if (!isNewInstall && installTimestamp <= 0) {
                    return
                }
                sendInstallEvent(it) 
            }
            // clear install timestamp
            val editor = sharedPreferences?.edit()
            editor?.remove(TrackerConstants.INSTALL_TIMESTAMP)
            editor?.commit()
        }
    }

    private fun sendInstallEvent(installTimestamp: Long) {
        val event = SelfDescribing(SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL))
        if (installTimestamp > 0) {
            event.trueTimestamp(installTimestamp)
        }
        val notificationData: MutableMap<String, Any> = HashMap()
        notificationData["event"] = event
        postNotification("SnowplowInstallTracking", notificationData)
    }

    companion object {
        private val TAG = InstallTracker::class.java.simpleName
        private var sharedInstance: InstallTracker? = null
        
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): InstallTracker {
            if (sharedInstance == null) {
                sharedInstance = InstallTracker(context)
            }
            return sharedInstance!!
        }
    }
}
