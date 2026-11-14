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

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.session.ProcessObserver
import com.snowplowanalytics.core.utils.NotificationCenter
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.configuration.SignalsConfiguration
import com.snowplowanalytics.snowplow.entity.ClientSessionEntity
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.signals.Intervention
import com.snowplowanalytics.snowplow.signals.InterventionAttributeKey
import com.snowplowanalytics.snowplow.signals.InterventionHandler
import com.snowplowanalytics.snowplow.signals.ServiceAttributesResult
import com.snowplowanalytics.snowplow.signals.SignalsAttributeKeys
import com.snowplowanalytics.snowplow.signals.SignalsController
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SignalsControllerImpl(
    serviceProvider: ServiceProviderInterface,
    private val context: Context? = null
) : Controller(serviceProvider), SignalsController {

    private val configuration: SignalsConfiguration?
        get() = serviceProvider.signalsConfiguration

    private var client: OkHttpClient? = null
    private var clientTimeoutMillis: Long? = null

    private val handlers = LinkedHashMap<String, InterventionHandler>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isObservingLifecycle = false

    internal val interventionStream = InterventionStream(
        clientFactory = { configuration?.let { streamingClient(it) } },
        urlFactory = { userId, sessionId -> interventionsUrl(userId, sessionId) },
        onMessage = { dispatch(it) }
    )

    // Held strongly here because NotificationCenter keeps only weak references.
    private val lifecycleObserver = object : NotificationCenter.FunctionalObserver() {
        override fun apply(data: Map<String, Any>) {
            val isForeground = data["isForeground"] as? Boolean ?: return
            interventionStream.setForeground(isForeground)
        }
    }

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
        Logger.e(TAG, "Could not read service attributes: %s", message)
        return ServiceAttributesResult.failure(status, message)
    }

    // Interventions

    override fun addInterventionHandler(id: String, handler: InterventionHandler) {
        if (configuration == null) {
            Logger.e(TAG, "Could not add intervention handler: %s", NOT_CONFIGURED)
            return
        }
        synchronized(handlers) { handlers[id] = handler }
        observeLifecycle()

        if (!interventionStream.hasIdentifiers) {
            val userId = identifierFromSession(SignalsAttributeKeys.CLIENT_SESSION_USER_ID)
            val sessionId = identifierFromSession(SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID)
            if (userId.isSuccess && sessionId.isSuccess) {
                interventionStream.setIdentifiers(userId.getOrNull(), sessionId.getOrNull())
            } else {
                val reason = (userId.exceptionOrNull() ?: sessionId.exceptionOrNull())?.message
                Logger.d(TAG, "Not subscribing to interventions yet: %s", reason)
            }
        }
        interventionStream.setEnabled(true)
    }

    override fun removeInterventionHandler(id: String) {
        val isEmpty = synchronized(handlers) {
            handlers.remove(id)
            handlers.isEmpty()
        }
        if (isEmpty) interventionStream.setEnabled(false)
    }

    /** Called after each event is tracked, to keep the subscription's identifiers current. */
    internal fun onEventTracked(event: InspectableEvent) {
        if (configuration == null) return
        val session = event.entities.find { it is ClientSessionEntity } as? ClientSessionEntity ?: return
        interventionStream.setIdentifiers(session.userId, session.sessionId)
    }

    /** Reconnects after the tracker is reconfigured. */
    internal fun restart() {
        interventionStream.restart()
    }

    /** Closes the subscription when the tracker is removed. */
    internal fun close() {
        interventionStream.close()
        synchronized(this) {
            NotificationCenter.removeObserver(lifecycleObserver)
            isObservingLifecycle = false
        }
    }

    @Synchronized
    private fun observeLifecycle() {
        if (isObservingLifecycle) return
        isObservingLifecycle = true
        // Lifecycle notifications are only posted once the process observer is set up, which the
        // tracker skips when lifecycle autotracking is off.
        context?.let { ProcessObserver.initialize(it) }
        NotificationCenter.addObserver(LIFECYCLE_NOTIFICATION, lifecycleObserver)
    }

    private fun dispatch(data: String) {
        val intervention = try {
            parseIntervention(JSONObject(data))
        } catch (e: JSONException) {
            Logger.e(TAG, "Received an invalid intervention: %s", e.message)
            return
        }

        val currentHandlers = synchronized(handlers) { handlers.toList() }
        if (currentHandlers.isEmpty()) {
            Logger.d(TAG, "No handlers registered for intervention %s", intervention.name)
            return
        }

        Logger.d(TAG, "Dispatching intervention %s", intervention.name)
        trackMeasurement(
            INTERVENTION_RECEIVE_SCHEMA,
            mapOf("handlers" to currentHandlers.map { it.first }),
            intervention
        )

        for ((handlerId, handler) in currentHandlers) {
            mainHandler.post {
                try {
                    handler.handle(intervention)
                    trackMeasurement(INTERVENTION_HANDLE_SCHEMA, mapOf("handler" to handlerId), intervention)
                } catch (e: Exception) {
                    Logger.e(TAG, "Handler %s failed processing intervention: %s", handlerId, e.message)
                    trackMeasurement(
                        INTERVENTION_HANDLE_ERROR_SCHEMA,
                        mapOf("handler" to handlerId, "error" to e.toString()),
                        intervention
                    )
                }
            }
        }
    }

    private fun trackMeasurement(schema: String, data: Map<String, Any?>, intervention: Intervention) {
        // Don't recreate a tracker that was removed while a handler was running.
        if (!serviceProvider.isTrackerInitialized) return
        val entity = SelfDescribingJson(
            INTERVENTION_INSTANCE_SCHEMA,
            mapOf(
                "intervention_id" to intervention.interventionId,
                "name" to intervention.name,
                "version" to intervention.version,
                "attributeKey" to mapOf(
                    "name" to intervention.targetAttributeKey.name,
                    "id" to intervention.targetAttributeKey.id
                ),
                "attributes" to intervention.attributes.map { (attribute, value) ->
                    mapOf("attribute" to attribute, "value" to value)
                }
            )
        )
        val event = SelfDescribing(schema, data)
        event.entities(listOf(entity))
        serviceProvider.getOrMakeTracker().track(event)
    }

    private fun interventionsUrl(userId: String, sessionId: String): HttpUrl? {
        val configuration = configuration ?: return null
        return buildUrl(configuration.endpoint)
            ?.newBuilder()
            ?.addPathSegments(INTERVENTIONS_PATH)
            ?.addQueryParameter(SignalsAttributeKeys.CLIENT_SESSION_USER_ID, userId)
            ?.addQueryParameter(SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID, sessionId)
            ?.build()
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

    /** A client for the interventions stream: connects within the timeout but never times out reading. */
    private fun streamingClient(configuration: SignalsConfiguration): OkHttpClient {
        return httpClient(configuration).newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    companion object {
        private val TAG = SignalsControllerImpl::class.java.simpleName

        /** Identifies the internal plugin that passes tracked events to [onEventTracked]. */
        internal const val PLUGIN_IDENTIFIER = "SnowplowSignals"

        internal const val INTERVENTION_RECEIVE_SCHEMA =
            "iglu:com.snowplowanalytics.signals/intervention_receive/jsonschema/1-0-0"
        internal const val INTERVENTION_HANDLE_SCHEMA =
            "iglu:com.snowplowanalytics.signals/intervention_handle/jsonschema/1-0-0"
        internal const val INTERVENTION_HANDLE_ERROR_SCHEMA =
            "iglu:com.snowplowanalytics.signals/intervention_handle_error/jsonschema/1-0-0"
        internal const val INTERVENTION_INSTANCE_SCHEMA =
            "iglu:com.snowplowanalytics.signals/intervention_instance/jsonschema/1-0-0"

        private const val LIFECYCLE_NOTIFICATION = "SnowplowLifecycleTracking"
        private const val INTERVENTIONS_PATH = "api/v1/interventions"

        @Throws(JSONException::class)
        internal fun parseIntervention(json: JSONObject): Intervention {
            val targetAttributeKey = json.getJSONObject("target_attribute_key")
            return Intervention(
                interventionId = json.getString("intervention_id"),
                name = json.getString("name"),
                version = json.getInt("version"),
                attributes = json.optJSONObject("attributes")?.let { SignalsJson.toMap(it) } ?: emptyMap(),
                targetAttributeKey = InterventionAttributeKey(
                    name = targetAttributeKey.getString("name"),
                    id = targetAttributeKey.getString("id")
                )
            )
        }

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
