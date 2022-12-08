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
import java.io.IOException

import java.util.concurrent.TimeUnit

class ConfigurationFetcher(
    context: Context,
    private val remoteConfiguration: RemoteConfiguration,
    private val onFetchCallback: Consumer<FetchedConfigurationBundle>
) {
    private val TAG = ConfigurationFetcher::class.java.simpleName

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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
        onFetchCallback: Consumer<FetchedConfigurationBundle>
    ) {
        val data = responseBody.string()
        val jsonObject = JSONObject(data)
        val bundle = FetchedConfigurationBundle(context, jsonObject)
        onFetchCallback.accept(bundle)
    }

    private fun exceptionHandler(t: Throwable?) {
        var message = t!!.message
        if (message == null) {
            message = "no message provided"
        }
        Logger.e(TAG, message, t)
    }

    companion object {
        private const val TRAFFIC_STATS_TAG = 1
    }
}
