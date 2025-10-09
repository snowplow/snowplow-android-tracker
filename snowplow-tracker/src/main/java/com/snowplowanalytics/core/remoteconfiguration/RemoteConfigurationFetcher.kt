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
package com.snowplowanalytics.core.remoteconfiguration

import android.content.Context
import android.net.TrafficStats
import android.net.Uri
import androidx.core.util.Consumer

import com.snowplowanalytics.core.emitter.Executor.execute
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.snowplow.configuration.RemoteConfiguration

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

import org.json.JSONException
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds
import java.io.IOException

import java.util.concurrent.TimeUnit

class RemoteConfigurationFetcher(
    context: Context,
    private val remoteConfiguration: RemoteConfiguration,
    private val onFetchCallback: Consumer<RemoteConfigurationBundle>
) {
    private val TAG = RemoteConfigurationFetcher::class.java.simpleName

    init {
        execute(getRunnable(context)) { t: Throwable? -> exceptionHandler(t) }
    }

    // Private methods
    private fun getRunnable(context: Context): Runnable {
        return Runnable {
            try {
                val body = performRequest(remoteConfiguration.endpoint)
                body?.let { resolveRequest(context, it, onFetchCallback) }
            } catch (e: Exception) {
                Logger.e(TAG, "Unable to get remote configuration: " + e.message, e)
            }
        }
    }

    @Throws(IOException::class)
    private fun performRequest(endpoint: String): ResponseBody? {
        val uriBuilder = Uri.parse(endpoint).buildUpon()
        val uri = uriBuilder.build().toString()
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15.seconds)
            .readTimeout(15.seconds)
            .build()
        val request: Request = Request.Builder()
            .url(uri)
            .get()
            .build()
        TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG)
        val resp = client.newCall(request).execute()
        val body = resp.body
        return if (resp.isSuccessful && body != null) {
            body
        } else null
    }

    @Throws(IOException::class, JSONException::class)
    private fun resolveRequest(
        context: Context,
        responseBody: ResponseBody,
        onFetchCallback: Consumer<RemoteConfigurationBundle>
    ) {
        val data = responseBody.string()
        val jsonObject = JSONObject(data)
        val bundle = RemoteConfigurationBundle(context, jsonObject)
        onFetchCallback.accept(bundle)
    }

    private fun exceptionHandler(t: Throwable?) {
        var message = t?.message
        if (message == null) {
            message = "no message provided"
        }
        Logger.e(TAG, message, t)
    }

    companion object {
        private const val TRAFFIC_STATS_TAG = 1
    }
}
