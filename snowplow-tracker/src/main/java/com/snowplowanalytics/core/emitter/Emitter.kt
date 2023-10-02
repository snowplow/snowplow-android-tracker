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
package com.snowplowanalytics.core.emitter

import android.content.Context

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.emitter.storage.SQLiteEventStore
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.Util
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EmitterEvent
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.*
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder
import com.snowplowanalytics.snowplow.payload.Payload

import okhttp3.CookieJar
import okhttp3.OkHttpClient

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
class Emitter(context: Context, collectorUri: String, builder: ((Emitter) -> Unit)? = null) {
    private val TAG = Emitter::class.java.simpleName

    private var builderFinished = false
    private val isRunning = AtomicBoolean(false)
    private val isEmittingPaused = AtomicBoolean(false)
    private var isCustomNetworkConnection = false
    
    private val context: Context
    private lateinit var uri: String
    private var emptyCount = 0

    /**
     * This configuration option is not published in the EmitterConfiguration class.
     * Create an Emitter and Tracker directly, not via the Snowplow interface, to configure timeUnit.
     */
    var timeUnit: TimeUnit = EmitterDefaults.timeUnit
        set(unit) {
            if (!builderFinished) {
                field = unit
            }
        }

    var cookieJar: CookieJar? = null
        set(cookieJar) {
            if (!builderFinished) {
                field = cookieJar
            }
        }
    
    var threadPoolSize = EmitterDefaults.threadPoolSize
        set(poolSize) {
            if (!builderFinished) {
                field = poolSize
            }
        }

    var client: OkHttpClient? = null
        set(client) {
            if (!builderFinished) {
                field = client
            }
        }
    
    /**
     * The emitter event store object
     */
    var eventStore: EventStore? = null
        // if not set during Emitter initialisation (via builder),
        // this is set as part of Tracker initialisation, as a side-effect of setting namespace
        set(eventStore) {
            if (field == null) {
                field = eventStore
            }
        }
    
    /**
     * This configuration option is not published in the EmitterConfiguration class.
     * Create an Emitter and Tracker directly, not via the Snowplow interface, to configure tlsVersions.
     * @return the TLS versions accepted for the emitter
     */
    var tlsVersions: EnumSet<TLSVersion> = EmitterDefaults.tlsVersions

    /**
     * The emitter tick. This configuration option is not published in the EmitterConfiguration class.
     * Create an Emitter and Tracker directly, not via the Snowplow interface, to configure emitterTick.
     */
    var emitterTick: Int = EmitterDefaults.emitterTick

    /**
     * The amount of times the event store can be empty before it is shut down.
     * This configuration option is not published in the EmitterConfiguration class.
     * Create an Emitter and Tracker directly, not via the Snowplow interface, to configure emptyLimit.
     */
    var emptyLimit: Int = EmitterDefaults.emptyLimit

    /**
     * The maximum amount of events to grab for an emit attempt.
     */
    var sendLimit: Int = EmitterDefaults.sendLimit

    /**
     * The GET byte limit
     */
    var byteLimitGet: Long = EmitterDefaults.byteLimitGet

    /**
     * The POST byte limit
     */
    var byteLimitPost: Long = EmitterDefaults.byteLimitPost

    /**
     * @return the request callback method
     */
    var requestCallback: RequestCallback? = null

    /**
     * The emitter status
     */
    val emitterStatus: Boolean
        get() = isRunning.get()

    /**
     * The URI for the Emitter
     */
    var emitterUri: String
        get() = networkConnection?.uri.toString()
        set(uri) {
            this.uri = uri
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection =
                    emitTimeout?.let {
                        OkHttpNetworkConnectionBuilder(uri, context)
                            .method(httpMethod)
                            .tls(tlsVersions)
                            .emitTimeout(it)
                            .customPostPath(customPostPath)
                            .client(client)
                            .cookieJar(cookieJar)
                            .serverAnonymisation(serverAnonymisation)
                            .requestHeaders(requestHeaders)
                            .build()
                    }
            }
        }

    /**
     * The Emitters request method
     */
    var httpMethod: HttpMethod = EmitterDefaults.httpMethod
        /**
         * Sets the HttpMethod for the Emitter
         * @param method the HttpMethod
         */
        set(method) {
            field = method
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection = emitTimeout?.let {
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(it)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
                
            }
        }

    /**
     * The buffer option selected for the emitter
     */
    var bufferOption: BufferOption = EmitterDefaults.bufferOption
        /**
         * Whether the buffer should send events instantly or after the buffer has reached
         * its limit. By default, this is set to BufferOption Default.
         *
         * @param option Set the BufferOption enum to Instant to send events upon creation.
         */
        set(option) {
            if (!isRunning.get()) {
                field = option
            }
        }

    /**
     * The request security selected for the emitter
     */
    var requestSecurity: Protocol = EmitterDefaults.httpProtocol
        /**
         * Sets the Protocol for the Emitter
         * @param security the Protocol
         */
        set(security) {
            field = security
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection = emitTimeout?.let {
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(it)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
                
            }
        }

    /**
     * Emitter namespace. NB: setting the namespace has a side-effect of creating the SQLiteEventStore
     */
    var namespace: String? = null
        set(namespace) {
            field = namespace
            if (eventStore == null) {
                eventStore = field?.let { SQLiteEventStore(context, it) }
            }
        }

    /**
     * The maximum timeout for emitting events. If emit time exceeds this value 
     * TimeOutException will be thrown.
     * 
     * This configuration option, used to create an OkHttpNetworkConnection, is not published 
     * in the EmitterConfiguration class. However, it is published in the NetworkConfiguration class.
     * Configure emitTimeout by providing it via networkConfiguration to Snowplow.createTracker().
     */
    var emitTimeout: Int? = EmitterDefaults.emitTimeout
        set(emitTimeout) {
            emitTimeout?.let { 
                field = emitTimeout
                if (!isCustomNetworkConnection && builderFinished) {
                    networkConnection = OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(emitTimeout)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
            }
        }
    
    /**
     * The customPostPath for the Emitter
     */
    var customPostPath: String? = null
        set(customPostPath) {
            field = customPostPath
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection = emitTimeout?.let {
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(it)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
                
            }
        }
    
    private val _networkConnection = AtomicReference<NetworkConnection>()
    /**
     * The NetworkConnection if it exists
     */
    var networkConnection: NetworkConnection?
        get() = _networkConnection.get()
        set(value) { _networkConnection.set(value) }

    /**
     * Whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    var serverAnonymisation: Boolean = EmitterDefaults.serverAnonymisation
        /**
         * Updates the server anonymisation setting for the Emitter.
         * Ignored if using a custom network connection.
         * @param serverAnonymisation whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
         */
        set(serverAnonymisation) {
            field = serverAnonymisation
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection = emitTimeout?.let {
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(it)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
            }
        }

    private val _customRetryForStatusCodes = AtomicReference<Map<Int, Boolean>>()
    var customRetryForStatusCodes: Map<Int, Boolean>?
        get() = _customRetryForStatusCodes.get()
        set(value) {
            _customRetryForStatusCodes.set(value ?: HashMap())
        }

    private val _retryFailedRequests = AtomicReference(EmitterDefaults.retryFailedRequests)
    /**
     * Whether retrying failed requests is allowed
     */
    var retryFailedRequests: Boolean
        get() = _retryFailedRequests.get()
        set(value) { _retryFailedRequests.set(value) }

    /**
     * The request headers for the emitter
     */
    var requestHeaders: Map<String, String>? = null
        /**
         * Updates the request headers for the emitter.
         * Ignored if using a custom network connection.
         */
        set(requestHeaders) {
            field = requestHeaders
            if (!isCustomNetworkConnection && builderFinished) {
                networkConnection = emitTimeout?.let {
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(it)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .requestHeaders(requestHeaders)
                        .build()
                }
            }
        }

    /**
     * Creates an emitter object
     */
    init {
        this.context = context
        builder?.let { it(this) }

        if (networkConnection == null) {
            isCustomNetworkConnection = false
            var endpoint = collectorUri
            if (!endpoint.startsWith("http")) {
                val protocol =
                    if (requestSecurity === Protocol.HTTPS) "https://" else "http://"
                endpoint = protocol + endpoint
            }
            uri = endpoint
            networkConnection = emitTimeout?.let {
                OkHttpNetworkConnectionBuilder(endpoint, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(it)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .requestHeaders(requestHeaders)
                    .build()
            }
        } else {
            isCustomNetworkConnection = true
        }
        
        if (threadPoolSize > 2) {
            Executor.threadCount = threadPoolSize
        }
        builderFinished = true
        Logger.v(TAG, "Emitter created successfully!")
    }
    
    // --- Controls
    
    /**
     * Adds a payload to the EventStore and
     * then attempts to start the emitter
     * if it is not currently running.
     *
     * @param payload the event payload
     * to be added.
     */
    fun add(payload: Payload) {
        Executor.execute(TAG) {
            eventStore?.add(payload)
            if (isRunning.compareAndSet(false, true)) {
                try {
                    attemptEmit(networkConnection)
                } catch (t: Throwable) {
                    isRunning.set(false)
                    Logger.e(TAG, "Received error during emission process: %s", t)
                }
            }
        }
    }

    /**
     * Attempts to start the emitter if it
     * is not currently running.
     */
    fun flush() {
        Executor.execute(TAG) {
            if (isRunning.compareAndSet(false, true)) {
                try {
                    attemptEmit(networkConnection)
                } catch (t: Throwable) {
                    isRunning.set(false)
                    Logger.e(TAG, "Received error during emission process: %s", t)
                }
            }
        }
    }

    /**
     * Pause emitting events.
     */
    fun pauseEmit() {
        isEmittingPaused.set(true)
    }

    /**
     * Resume emitting events and attempt to emit any queued events.
     */
    fun resumeEmit() {
        if (isEmittingPaused.compareAndSet(true, false)) {
            flush()
        }
    }

    /**
     * Resets the `isRunning` truth to false and shutdown.
     */
    fun shutdown() {
        shutdown(0)
    }

    /**
     * Resets the `isRunning` truth to false and shutdown.
     *
     * @param timeout the amount of seconds to wait for the termination of the running threads.
     */
    fun shutdown(timeout: Long): Boolean {
        Logger.d(TAG, "Shutting down emitter.")
        isRunning.compareAndSet(true, false)
        
        val es = Executor.shutdown()
        return if (es == null || timeout <= 0) {
            true
        } else try {
            val isTerminated = es.awaitTermination(timeout, TimeUnit.SECONDS)
            Logger.d(TAG, "Executor is terminated: $isTerminated")
            isTerminated
        } catch (e: InterruptedException) {
            Logger.e(TAG, "Executor termination is interrupted: " + e.message)
            false
        }
    }

    /**
     * Attempts to send events in the database to a collector.
     *
     * - If the emitter is paused, it will not send
     * - If the emitter is not online it will not send
     * - If the emitter is online but there are no events:
     * + Increment empty counter until emptyLimit reached
     * + Incurs a backoff period between empty counters
     * - If the emitter is online and we have events:
     * + Pulls allowed amount of events from database and
     * attempts to send.
     * + If there are failures resets running state
     * + Otherwise will attempt to emit again
     */
    private fun attemptEmit(networkConnection: NetworkConnection?) {
        if (isEmittingPaused.get()) {
            Logger.d(TAG, "Emitter paused.")
            isRunning.compareAndSet(true, false)
            return
        }
        
        if (!Util.isOnline(context)) {
            Logger.d(TAG, "Emitter loop stopping: emitter offline.")
            isRunning.compareAndSet(true, false)
            return
        }
        
        if (eventStore == null) {
            Logger.d(TAG, "No EventStore set.")
            isRunning.compareAndSet(true, false)
            return
        }
        val eventStore = eventStore ?: return

        if (networkConnection == null) {
            Logger.d(TAG, "No networkConnection set.")
            isRunning.compareAndSet(true, false)
            return
        }
        
        if (eventStore.size() <= 0) {
            if (emptyCount >= emptyLimit) {
                Logger.d(TAG, "Emitter loop stopping: empty limit reached.")
                isRunning.compareAndSet(true, false)
                return
            }
            emptyCount++
            Logger.e(TAG, "Emitter database empty: $emptyCount")
            try {
                timeUnit.sleep(emitterTick.toLong())
            } catch (e: InterruptedException) {
                Logger.e(TAG, "Emitter thread sleep interrupted: $e")
            }
            attemptEmit(networkConnection) // at this point we update network connection since it might be outdated after sleep
            return
        }
        
        emptyCount = 0
        val events = eventStore.getEmittableEvents(sendLimit)
        val requests = buildRequests(events, networkConnection.httpMethod)
        val results = networkConnection.sendRequests(requests)
        
        Logger.v(TAG, "Processing emitter results.")
        
        var successCount = 0
        var failedWillRetryCount = 0
        var failedWontRetryCount = 0
        val removableEvents: MutableList<Long?> = ArrayList()

        for (res in results) {
            if (res.isSuccessful) {
                removableEvents.addAll(res.eventIds)
                successCount += res.eventIds.size
            } else if (res.shouldRetry(customRetryForStatusCodes, retryFailedRequests)) {
                failedWillRetryCount += res.eventIds.size
                Logger.e(TAG, "Request sending failed but we will retry later.")
            } else {
                failedWontRetryCount += res.eventIds.size
                removableEvents.addAll(res.eventIds)
                Logger.e(
                    TAG,
                    String.format(
                        "Sending events to Collector failed with status %d. Events will be dropped.",
                        res.statusCode
                    )
                )
            }
        }
        eventStore.removeEvents(removableEvents)
        
        val allFailureCount = failedWillRetryCount + failedWontRetryCount
        Logger.d(TAG, "Success Count: %s", successCount)
        Logger.d(TAG, "Failure Count: %s", allFailureCount)
        
        if (requestCallback != null) {
            if (allFailureCount != 0) {
                requestCallback?.onFailure(successCount, allFailureCount)
            } else {
                requestCallback?.onSuccess(successCount)
            }
        }
        if (failedWillRetryCount > 0 && successCount == 0) {
            if (Util.isOnline(context)) {
                Logger.e(TAG, "Ensure collector path is valid: %s", networkConnection.uri)
            }
            Logger.e(TAG, "Emitter loop stopping: failures.")
            isRunning.compareAndSet(true, false)
        } else {
            attemptEmit(networkConnection) // refresh network connection for next emit
        }
    }

    /**
     * Returns a list of ReadyRequests which can
     * all be sent regardless of if it is GET or POST.
     * - Checks if the event is over-sized.
     * - Stores all of the relevant event ids.
     *
     * @param events a list of EmittableEvents pulled
     * from the database.
     * @param httpMethod HTTP method to use (passed in order to ensure consistency within attemptEmit)
     * @return a list of ready to send requests
     */
    private fun buildRequests(
        events: List<EmitterEvent?>,
        httpMethod: HttpMethod
    ): List<Request> {
        val requests: MutableList<Request> = ArrayList()
        val sendingTime = Util.timestamp()
        
        if (httpMethod === HttpMethod.GET) {
            for (event in events) {
                val payload = event?.payload
                if (payload != null) {
                    addSendingTimeToPayload(payload, sendingTime)
                    val isOversize = isOversize(payload, httpMethod)
                    val request = Request(payload, event.eventId, isOversize)
                    requests.add(request)
                }
            }
        } else {
            var i = 0
            while (i < events.size) {
                var reqEventIds: MutableList<Long> = ArrayList()
                var postPayloadMaps: MutableList<Payload> = ArrayList()
                
                var j = i
                while (j < i + bufferOption.code && j < events.size) {
                    val event = events[j]
                    val payload = event?.payload
                    val eventId = event?.eventId
                    if (payload != null && eventId != null) {
                        addSendingTimeToPayload(payload, sendingTime)
                        if (isOversize(payload, httpMethod)) {
                            val request = Request(payload, eventId, true)
                            requests.add(request)
                        } else if (isOversize(payload, postPayloadMaps, httpMethod)) {
                            val request = Request(postPayloadMaps, reqEventIds)
                            requests.add(request)

                            // Clear collections and build a new POST
                            postPayloadMaps = ArrayList()
                            reqEventIds = ArrayList()

                            // Build and store the request
                            postPayloadMaps.add(payload)
                            reqEventIds.add(eventId)
                        } else {
                            postPayloadMaps.add(payload)
                            reqEventIds.add(eventId)
                        }
                        j++
                    }
                    
                }

                // Check if all payloads have been processed
                if (postPayloadMaps.isNotEmpty()) {
                    val request = Request(postPayloadMaps, reqEventIds)
                    requests.add(request)
                }
                i += bufferOption.code
            }
        }
        return requests
    }

    /**
     * Calculate if the payload exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to send.
     * @param httpMethod HTTP method to use (passed in order to ensure consistency within attemptEmit)
     * @return whether the payload exceeds the maximum size allowed.
     */
    private fun isOversize(payload: Payload, httpMethod: HttpMethod): Boolean {
        return isOversize(payload, ArrayList(), httpMethod)
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param previousPayloads already in the payload bundle.
     * @param httpMethod HTTP method to use (passed in order to ensure consistency within attemptEmit)
     * @return whether the payload bundle exceeds the maximum size allowed.
     */
    private fun isOversize(
        payload: Payload,
        previousPayloads: List<Payload>,
        httpMethod: HttpMethod
    ): Boolean {
        val byteLimit = if (httpMethod === HttpMethod.GET) byteLimitGet else byteLimitPost
        return isOversize(payload, byteLimit, previousPayloads)
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param byteLimit maximum amount of bytes allowed.
     * @param previousPayloads already in the payload bundle.
     * @return whether the payload bundle exceeds the maximum size allowed.
     */
    private fun isOversize(
        payload: Payload,
        byteLimit: Long,
        previousPayloads: List<Payload>
    ): Boolean {
        var totalByteSize = payload.byteSize
        for (previousPayload in previousPayloads) {
            totalByteSize += previousPayload.byteSize
        }
        val wrapperBytes =
            if (previousPayloads.isNotEmpty()) previousPayloads.size + POST_WRAPPER_BYTES else 0
        return totalByteSize + wrapperBytes > byteLimit
    }

    /**
     * Adds the Sending Time (stm) field
     * to each event payload.
     *
     * @param payload The payload to append the field to
     * @param timestamp An optional timestamp String
     */
    private fun addSendingTimeToPayload(payload: Payload, timestamp: String) {
        payload.add(Parameters.SENT_TIMESTAMP, timestamp)
    }
    
    companion object {
        private const val POST_WRAPPER_BYTES =
            88 // "schema":"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3","data":[]
    }
}
