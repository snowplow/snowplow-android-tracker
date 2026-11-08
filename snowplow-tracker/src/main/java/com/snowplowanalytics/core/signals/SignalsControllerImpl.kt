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

package com.snowplowanalytics.core.signals

import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.configuration.SignalsConfiguration
import com.snowplowanalytics.snowplow.signals.ServiceAttributesResult
import com.snowplowanalytics.snowplow.signals.SignalsAttributeKeys
import com.snowplowanalytics.snowplow.signals.SignalsController
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SignalsControllerImpl(serviceProvider: ServiceProviderInterface) :
    Controller(serviceProvider), SignalsController {

    private val configuration: SignalsConfiguration?
        get() = serviceProvider.signalsConfiguration

    private var client: OkHttpClient? = null
    private var clientTimeoutMillis: Long? = null

    // Service attributes

    override fun getServiceAttributes(service: String, callback: Consumer<ServiceAttributesResult>) {
        getServiceAttributes(service, SignalsAttributeKeys.CLIENT_SESSION_USER_ID, null, callback)
    }

    override fun getServiceAttributes(
        service: String,
        attributeKey: String,
        callback: Consumer<ServiceAttributesResult>
    ) {
        getServiceAttributes(service, attributeKey, null, callback)
    }

    override fun getServiceAttributes(
        service: String,
        attributeKey: String,
        identifier: String?,
        callback: Consumer<ServiceAttributesResult>
    ) {
        Executor.execute(TAG) {
            callback.accept(fetchServiceAttributes(service, attributeKey, identifier))
        }
    }

    private fun fetchServiceAttributes(
        service: String,
        attributeKey: String,
        identifier: String?
    ): ServiceAttributesResult {
        val configuration = configuration ?: return failure(0, NOT_CONFIGURED)

        val resolvedIdentifier = identifier ?: identifierFromSession(attributeKey).getOrElse {
            return failure(0, it.message ?: "No identifier for attribute key '$attributeKey'.")
        }

        val url = buildUrl(configuration.endpoint)
            ?.newBuilder()
            ?.addPathSegments(SERVICES_PATH)
            ?.addPathSegment(service)
            ?.addQueryParameter(attributeKey, resolvedIdentifier)
            ?.build()
            ?: return failure(0, "Invalid Signals endpoint: ${configuration.endpoint}")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        return try {
            httpClient(configuration).newCall(request).execute().use { response ->
                val body = response.body.string()
                if (response.isSuccessful) {
                    parseAttributes(response.code, body)
                } else {
                    failure(response.code, parseErrorMessage(response.code, body))
                }
            }
        } catch (e: IOException) {
            failure(0, "Request to Signals failed: ${e.message}")
        }
    }

    private fun parseAttributes(status: Int, body: String): ServiceAttributesResult {
        return try {
            val attributes = JSONObject(body).optJSONObject("attributes")
                ?: return failure(status, "Signals response has no attributes.")
            ServiceAttributesResult.success(SignalsJson.toMap(attributes))
        } catch (e: JSONException) {
            failure(status, "Signals response is not valid JSON: ${e.message}")
        }
    }

    private fun parseErrorMessage(status: Int, body: String): String {
        return try {
            JSONObject(body).optString("error").ifEmpty { null }
        } catch (e: JSONException) {
            null
        } ?: "Request to Signals failed with status $status."
    }

    private fun failure(status: Int, message: String): ServiceAttributesResult {
        Logger.e(TAG, "Could not read service attributes: $message")
        return ServiceAttributesResult.failure(status, message)
    }

    // Identifiers

    /**
     * Reads the value of a built-in attribute key from the current session.
     * Fails if the session is disabled, user anonymisation is on, or no session has started yet.
     */
    internal fun identifierFromSession(attributeKey: String): Result<String> {
        if (attributeKey != SignalsAttributeKeys.CLIENT_SESSION_USER_ID &&
            attributeKey != SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID
        ) {
            return Result.failure(IllegalArgumentException(
                "No identifier given for attribute key '$attributeKey'. Only " +
                    "'${SignalsAttributeKeys.CLIENT_SESSION_USER_ID}' and " +
                    "'${SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID}' are filled in from the session."
            ))
        }

        val trackerController = serviceProvider.getOrMakeTrackerController()
        if (trackerController.userAnonymisation) {
            return Result.failure(IllegalStateException(
                "User anonymisation is on, so the session identifiers can't be sent to Signals."
            ))
        }
        val session = trackerController.session ?: return Result.failure(IllegalStateException(
            "Session tracking is disabled, so there are no session identifiers to send to Signals."
        ))

        val value = if (attributeKey == SignalsAttributeKeys.CLIENT_SESSION_USER_ID) {
            session.userId
        } else {
            session.sessionId
        }
        if (value.isNullOrEmpty()) {
            return Result.failure(IllegalStateException(
                "No session has started yet. Track an event before reading from Signals."
            ))
        }
        return Result.success(value)
    }

    // Networking

    @Synchronized
    internal fun httpClient(configuration: SignalsConfiguration): OkHttpClient {
        val timeoutMillis = configuration.timeout.convert(TimeUnit.MILLISECONDS)
        val current = client
        if (current != null && clientTimeoutMillis == timeoutMillis) {
            return current
        }
        return OkHttpClient.Builder()
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .build()
            .also {
                client = it
                clientTimeoutMillis = timeoutMillis
            }
    }

    companion object {
        private val TAG = SignalsControllerImpl::class.java.simpleName

        internal const val NOT_CONFIGURED =
            "Signals is not configured. Pass a SignalsConfiguration to Snowplow.createTracker."
        private const val SERVICES_PATH = "api/v1/services"

        /** Parses the endpoint, assuming `https://` when no scheme is given. */
        internal fun buildUrl(endpoint: String): HttpUrl? {
            val trimmed = endpoint.trim()
            val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
            return withScheme.toHttpUrlOrNull()
        }
    }
}
