/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.configuration

import android.net.Uri
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.entity.ClientSessionEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * This configuration tells the tracker to send requests with the user ID in session context entity
 * to a Kantar endpoint used with FocalMeter.
 * The request is made when the first event with a new user ID is tracked.
 * The requests are only made if session context is enabled (default).
 * @param kantarEndpoint The Kantar URI endpoint including the HTTP protocol to send the requests to.
 * @param processUserId Callback to process user ID before sending it in a request. This may be used to apply hashing to the value.
 */
class FocalMeterConfiguration(
    val kantarEndpoint: String,
    val processUserId: ((String) -> String)? = null,
) : Configuration, PluginAfterTrackCallable, PluginIdentifiable {
    private val TAG = FocalMeterConfiguration::class.java.simpleName

    private var lastUserId: String? = null

    override val identifier: String
        get() = "KantarFocalMeter"

    override val afterTrackConfiguration: PluginAfterTrackConfiguration?
        get() = PluginAfterTrackConfiguration { event ->
            val session = event.entities.find { it is ClientSessionEntity } as? ClientSessionEntity
            session?.userId?.let { newUserId ->
                if (shouldUpdate(newUserId)) {
                    val processedUserId = processUserId?.invoke(newUserId) ?: newUserId
                    makeRequest(processedUserId)
                }
            }
        }

    private fun shouldUpdate(userId: String): Boolean {
        synchronized(this) {
            if (lastUserId == null || lastUserId != userId) {
                lastUserId = userId
                return true
            }
            return false
        }
    }

    private fun makeRequest(userId: String) {
        val uriBuilder = Uri.parse(kantarEndpoint).buildUpon()
        uriBuilder.appendQueryParameter("vendor", "snowplow")
        uriBuilder.appendQueryParameter("cs_fpid", userId)
        uriBuilder.appendQueryParameter("c12", "not_set")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(uriBuilder.build().toString())
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Logger.d(TAG, "Request to Kantar endpoint sent with user ID: $userId")
            } else {
                Logger.e(TAG, "Request to Kantar endpoint failed with code: ${response.code}")
            }
        } catch (e: IOException) {
            Logger.e(TAG, "Request to Kantar endpoint failed with exception: ${e.message}")
        }
    }

    override fun copy(): Configuration {
        return FocalMeterConfiguration(kantarEndpoint = kantarEndpoint)
    }

}
