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
class Emitter(context: Context, collectorUri: String, builder: EmitterBuilder?) {
    private val TAG = Emitter::class.java.simpleName
    private val context: Context
    /**
     * @return the request callback method
     */
    /**
     * @param requestCallback the callback request
     */
    var requestCallback: RequestCallback?
    private var httpMethod: HttpMethod
    private var bufferOption: BufferOption
    private var requestSecurity: Protocol

    /**
     * @return the TLS versions accepted for the emitter
     */
    val tlsVersions: EnumSet<TLSVersion>
    private var uri: String
    private var namespace: String? = null

    /**
     * @return the emitter tick
     */
    val emitterTick: Int

    /**
     * @return the amount of times the event store can be empty
     * before it is shutdown.
     */
    val emptyLimit: Int
    /**
     * @return the emitter send limit
     */
    /**
     * Sets the maximum amount of events to grab for an emit attempt.
     * @param sendLimit The maximum possible amount of events.
     */
    var sendLimit: Int
    /**
     * @return the GET byte limit
     */
    /**
     * @param byteLimitGet Set the GET byte limit
     */
    var byteLimitGet: Long
    /**
     * @return the POST byte limit
     */
    /**
     * @param byteLimitPost Set the POST byte limit
     */
    var byteLimitPost: Long
    private var emitTimeout: Int
    private val timeUnit: TimeUnit
    private var customPostPath: String?
    private val client: OkHttpClient?
    private val cookieJar: CookieJar? = null
    private var serverAnonymisation: Boolean
    private var isCustomNetworkConnection = false
    private val networkConnection = AtomicReference<NetworkConnection>()

    /**
     * @return the emitter event store object
     */
    var eventStore: EventStore?
        private set
    private var emptyCount = 0
    private val customRetryForStatusCodes = AtomicReference<Map<Int, Boolean>>()
    private val isRunning = AtomicBoolean(false)
    private val isEmittingPaused = AtomicBoolean(false)

    /**
     * Builder for the Emitter.
     */
    class EmitterBuilder {
        var requestCallback: RequestCallback? = null // Optional
        var httpMethod = HttpMethod.POST // Optional
        var bufferOption = BufferOption.DefaultGroup // Optional
        var requestSecurity = Protocol.HTTP // Optional
        var tlsVersions = EnumSet.of(TLSVersion.TLSv1_2) // Optional
        var emitterTick = 5 // Optional
        var sendLimit = 250 // Optional
        var emptyLimit = 5 // Optional
        var byteLimitGet: Long = 40000 // Optional
        var byteLimitPost: Long = 40000 // Optional
        var emitTimeout = 5 // Optional
        var threadPoolSize = 2 // Optional
        var serverAnonymisation = false // Optional
        var timeUnit = TimeUnit.SECONDS
        var client: OkHttpClient? = null //Optional
        var cookieJar: CookieJar? = null // Optional
        var customPostPath: String? = null //Optional
        var networkConnection: NetworkConnection? = null // Optional
        var eventStore: EventStore? = null // Optional
        var customRetryForStatusCodes: Map<Int, Boolean>? = null // Optional

        /**
         * @param networkConnection The component in charge for sending events to the collector.
         * @return itself
         */
        fun networkConnection(networkConnection: NetworkConnection?): EmitterBuilder {
            this.networkConnection = networkConnection
            return this
        }

        /**
         * @param eventStore The component in charge for persisting events before sending.
         * @return itself
         */
        fun eventStore(eventStore: EventStore?): EmitterBuilder {
            this.eventStore = eventStore
            return this
        }

        /**
         * @param httpMethod The method by which requests are emitted
         * @return itself
         */
        fun method(httpMethod: HttpMethod): EmitterBuilder {
            this.httpMethod = httpMethod
            return this
        }

        /**
         * @param option the buffer option for the emitter
         * @return itself
         */
        fun option(option: BufferOption): EmitterBuilder {
            bufferOption = option
            return this
        }

        /**
         * @param protocol the security chosen for requests
         * @return itself
         */
        fun security(protocol: Protocol): EmitterBuilder {
            requestSecurity = protocol
            return this
        }

        /**
         * @param version the TLS version allowed for requests
         * @return itself
         */
        fun tls(version: TLSVersion): EmitterBuilder {
            tlsVersions = EnumSet.of(version)
            return this
        }

        /**
         * @param versions the TLS versions allowed for requests
         * @return itself
         */
        fun tls(versions: EnumSet<TLSVersion>): EmitterBuilder {
            tlsVersions = versions
            return this
        }

        /**
         * @param requestCallback Request callback function
         * @return itself
         */
        fun callback(requestCallback: RequestCallback?): EmitterBuilder {
            this.requestCallback = requestCallback
            return this
        }

        /**
         * @param emitterTick The tick count between emitter attempts
         * @return itself
         */
        fun tick(emitterTick: Int): EmitterBuilder {
            this.emitterTick = emitterTick
            return this
        }

        /**
         * @param sendLimit The maximum amount of events to grab for an emit attempt
         * @return itself
         */
        fun sendLimit(sendLimit: Int): EmitterBuilder {
            this.sendLimit = sendLimit
            return this
        }

        /**
         * @param emptyLimit The amount of emitter ticks that are performed before we shut down
         * due to the database being empty.
         * @return itself
         */
        fun emptyLimit(emptyLimit: Int): EmitterBuilder {
            this.emptyLimit = emptyLimit
            return this
        }

        /**
         * @param byteLimitGet The maximum amount of bytes allowed to be sent in a payload
         * in a GET request.
         * @return itself
         */
        fun byteLimitGet(byteLimitGet: Long): EmitterBuilder {
            this.byteLimitGet = byteLimitGet
            return this
        }

        /**
         * @param byteLimitPost The maximum amount of bytes allowed to be sent in a payload
         * in a POST request.
         * @return itself
         */
        fun byteLimitPost(byteLimitPost: Long): EmitterBuilder {
            this.byteLimitPost = byteLimitPost
            return this
        }

        /**
         * @param emitTimeout The maximum timeout for emitting events. If emit time exceeds this value
         * TimeOutException will be thrown
         * @return itself
         */
        fun emitTimeout(emitTimeout: Int): EmitterBuilder {
            this.emitTimeout = emitTimeout
            return this
        }

        /**
         * @param timeUnit a valid TimeUnit
         * @return itself
         */
        fun timeUnit(timeUnit: TimeUnit): EmitterBuilder {
            this.timeUnit = timeUnit
            return this
        }

        /**
         * @param client An OkHttp client that will be used in the emitter, you can provide your
         * own if you want to share your Singleton client's interceptors, connection pool etc..
         * ,otherwise a new one is created.
         * @return itself
         */
        fun client(client: OkHttpClient?): EmitterBuilder {
            this.client = client
            return this
        }

        /**
         * @param cookieJar An OkHttp cookie jar to override the default cookie jar that stores
         * cookies in SharedPreferences. The cookie jar will be ignored in case
         * custom `client` is configured.
         * @return itself
         */
        fun cookieJar(cookieJar: CookieJar?): EmitterBuilder {
            this.cookieJar = cookieJar
            return this
        }

        /**
         * @param customPostPath A custom path that is used on the endpoint to send requests.
         * @return itself
         */
        fun customPostPath(customPostPath: String?): EmitterBuilder {
            this.customPostPath = customPostPath
            return this
        }

        /**
         * @param threadPoolSize The number of threads available for the tracker's operations.
         * @return itself
         */
        fun threadPoolSize(threadPoolSize: Int): EmitterBuilder {
            this.threadPoolSize = threadPoolSize
            return this
        }

        /**
         * Set custom retry rules for HTTP status codes received in emit responses from the Collector.
         * @param customRetryForStatusCodes Mapping of integers (status codes) to booleans (true for retry and false for not retry)
         * @return itself
         */
        fun customRetryForStatusCodes(customRetryForStatusCodes: Map<Int, Boolean>?): EmitterBuilder {
            this.customRetryForStatusCodes = customRetryForStatusCodes
            return this
        }

        /**
         * Ignored if using a custom network connection.
         * @param serverAnonymisation whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
         * @return itself
         */
        fun serverAnonymisation(serverAnonymisation: Boolean): EmitterBuilder {
            this.serverAnonymisation = serverAnonymisation
            return this
        }
    }

    /**
     * Creates an emitter object
     */
    init {
        var builder = builder
        this.context = context
        if (builder == null) {
            builder = EmitterBuilder()
        }
        requestCallback = builder.requestCallback
        bufferOption = builder.bufferOption
        requestSecurity = builder.requestSecurity
        tlsVersions = builder.tlsVersions
        emitterTick = builder.emitterTick
        emptyLimit = builder.emptyLimit
        sendLimit = builder.sendLimit
        byteLimitGet = builder.byteLimitGet
        byteLimitPost = builder.byteLimitPost
        emitTimeout = builder.emitTimeout
        timeUnit = builder.timeUnit
        client = builder.client
        eventStore = builder.eventStore
        serverAnonymisation = builder.serverAnonymisation
        uri = collectorUri
        httpMethod = builder.httpMethod
        customPostPath = builder.customPostPath
        if (builder.networkConnection == null) {
            isCustomNetworkConnection = false
            var endpoint = collectorUri
            if (!endpoint.startsWith("http")) {
                val protocol =
                    if (builder.requestSecurity === Protocol.HTTPS) "https://" else "http://"
                endpoint = protocol + endpoint
            }
            uri = endpoint
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(endpoint, context)
                    .method(builder.httpMethod)
                    .tls(builder.tlsVersions)
                    .emitTimeout(builder.emitTimeout)
                    .customPostPath(builder.customPostPath)
                    .client(builder.client)
                    .cookieJar(builder.cookieJar)
                    .serverAnonymisation(builder.serverAnonymisation)
                    .build()
            )
        } else {
            isCustomNetworkConnection = true
            setNetworkConnection(builder.networkConnection!!)
        }
        if (builder.threadPoolSize > 2) {
            Executor.setThreadCount(builder.threadPoolSize)
        }
        setCustomRetryForStatusCodes(builder.customRetryForStatusCodes)
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
            eventStore!!.add(payload)
            if (isRunning.compareAndSet(false, true)) {
                try {
                    attemptEmit(getNetworkConnection())
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
                    attemptEmit(getNetworkConnection())
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
     * Attempts to send events in the database to
     * a collector.
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
        if (eventStore!!.size <= 0) {
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
            attemptEmit(getNetworkConnection()) // at this point we update network connection since it might be outdated after sleep
            return
        }
        emptyCount = 0
        val events = eventStore!!.getEmittableEvents(sendLimit)
        val requests = buildRequests(events, networkConnection!!.httpMethod)
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
            } else if (res.shouldRetry(customRetryForStatusCodes.get())) {
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
        eventStore!!.removeEvents(removableEvents)
        val allFailureCount = failedWillRetryCount + failedWontRetryCount
        Logger.d(TAG, "Success Count: %s", successCount)
        Logger.d(TAG, "Failure Count: %s", allFailureCount)
        if (requestCallback != null) {
            if (allFailureCount != 0) {
                requestCallback!!.onFailure(successCount, allFailureCount)
            } else {
                requestCallback!!.onSuccess(successCount)
            }
        }
        if (failedWillRetryCount > 0 && successCount == 0) {
            if (Util.isOnline(context)) {
                Logger.e(TAG, "Ensure collector path is valid: %s", networkConnection.uri)
            }
            Logger.e(TAG, "Emitter loop stopping: failures.")
            isRunning.compareAndSet(true, false)
        } else {
            attemptEmit(getNetworkConnection()) // refresh network connection for next emit
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
    protected fun buildRequests(
        events: List<EmitterEvent?>,
        httpMethod: HttpMethod
    ): List<Request> {
        val requests: MutableList<Request> = ArrayList()
        val sendingTime = Util.getTimestamp()
        if (httpMethod === HttpMethod.GET) {
            for (event in events) {
                val payload = event!!.payload
                addSendingTimeToPayload(payload, sendingTime)
                val isOversize = isOversize(payload, httpMethod)
                val request = Request(payload, event.eventId, isOversize)
                requests.add(request)
            }
        } else {
            var i = 0
            while (i < events.size) {
                var reqEventIds: MutableList<Long?> = ArrayList()
                var postPayloadMaps: MutableList<Payload> = ArrayList()
                var j = i
                while (j < i + bufferOption.code && j < events.size) {
                    val event = events[j]
                    val payload = event!!.payload
                    val eventId = event.eventId
                    addSendingTimeToPayload(payload, sendingTime)
                    if (isOversize(payload, httpMethod)) {
                        val request = Request(payload, eventId, true)
                        requests.add(request)
                    } else if (isOversize(payload, postPayloadMaps, httpMethod)) {
                        val request: Request = Request(postPayloadMaps, reqEventIds)
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

                // Check if all payloads have been processed
                if (!postPayloadMaps.isEmpty()) {
                    val request: Request = Request(postPayloadMaps, reqEventIds)
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
     * @return weather the payload exceeds the maximum size allowed.
     */
    private fun isOversize(payload: Payload, httpMethod: HttpMethod): Boolean {
        return isOversize(payload, ArrayList(), httpMethod)
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param previousPaylods already in the payload bundle.
     * @param httpMethod HTTP method to use (passed in order to ensure consistency within attemptEmit)
     * @return weather the payload bundle exceeds the maximum size allowed.
     */
    private fun isOversize(
        payload: Payload,
        previousPaylods: List<Payload>,
        httpMethod: HttpMethod
    ): Boolean {
        val byteLimit = if (httpMethod === HttpMethod.GET) byteLimitGet else byteLimitPost
        return isOversize(payload, byteLimit, previousPaylods)
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param byteLimit maximum amount of bytes allowed.
     * @param previousPaylods already in the payload bundle.
     * @return weather the payload bundle exceeds the maximum size allowed.
     */
    private fun isOversize(
        payload: Payload,
        byteLimit: Long,
        previousPaylods: List<Payload>
    ): Boolean {
        var totalByteSize = payload.byteSize
        for (previousPayload in previousPaylods) {
            totalByteSize += previousPayload.byteSize
        }
        val wrapperBytes =
            if (previousPaylods.size > 0) previousPaylods.size + POST_WRAPPER_BYTES else 0
        return totalByteSize + wrapperBytes > byteLimit
    }
    // Request Builders
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
    // Setters, Getters and Checkers
    /**
     * @return the emitter status
     */
    val emitterStatus: Boolean
        get() = isRunning.get()

    fun setNamespace(namespace: String) {
        this.namespace = namespace
        if (eventStore == null) {
            eventStore = SQLiteEventStore(context, namespace)
        }
    }

    /**
     * Sets whether the buffer should send events instantly or after the buffer has reached
     * it's limit. By default, this is set to BufferOption Default.
     *
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    fun setBufferOption(option: BufferOption) {
        if (!isRunning.get()) {
            bufferOption = option
        }
    }

    /**
     * Sets the HttpMethod for the Emitter
     *
     * @param method the HttpMethod
     */
    fun setHttpMethod(method: HttpMethod) {
        if (!isCustomNetworkConnection) {
            httpMethod = method
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(uri, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .build()
            )
        }
    }

    /**
     * Sets the Protocol for the Emitter
     *
     * @param security the Protocol
     */
    fun setRequestSecurity(security: Protocol) {
        if (!isCustomNetworkConnection) {
            requestSecurity = security
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(uri, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .build()
            )
        }
    }

    /**
     * Updates the custom Post path for the Emitter
     *
     * @param customPostPath new Emitter custom Post path
     */
    fun setCustomPostPath(customPostPath: String?) {
        if (!isCustomNetworkConnection) {
            this.customPostPath = customPostPath
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(uri, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .build()
            )
        }
    }

    /**
     * Updates the timeout for the Emitter
     *
     * @param emitTimeout new Emitter timeout
     */
    fun setEmitTimeout(emitTimeout: Int) {
        if (!isCustomNetworkConnection) {
            this.emitTimeout = emitTimeout
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(uri, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .build()
            )
        }
    }

    /**
     * Updates the server anonymisation setting for the Emitter.
     * Ignored if using a custom network connection.
     *
     * @param serverAnonymisation whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    fun setServerAnonymisation(serverAnonymisation: Boolean) {
        if (!isCustomNetworkConnection) {
            this.serverAnonymisation = serverAnonymisation
            setNetworkConnection(
                OkHttpNetworkConnectionBuilder(uri, context)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .cookieJar(cookieJar)
                    .serverAnonymisation(serverAnonymisation)
                    .build()
            )
        }
    }
    /**
     * @return the emitter uri
     */
    /**
     * Updates the URI for the Emitter
     *
     * @param uri new Emitter URI
     */
    var emitterUri: String
        get() = getNetworkConnection()!!.uri.toString()
        set(uri) {
            if (!isCustomNetworkConnection) {
                this.uri = uri
                setNetworkConnection(
                    OkHttpNetworkConnectionBuilder(uri, context)
                        .method(httpMethod)
                        .tls(tlsVersions)
                        .emitTimeout(emitTimeout)
                        .customPostPath(customPostPath)
                        .client(client)
                        .cookieJar(cookieJar)
                        .serverAnonymisation(serverAnonymisation)
                        .build()
                )
            }
        }

    /**
     * @return the Emitters request method
     */
    fun getHttpMethod(): HttpMethod {
        return httpMethod
    }

    /**
     * @return the buffer option selected for the emitter
     */
    fun getBufferOption(): BufferOption {
        return bufferOption
    }

    /**
     * @return the request security selected for the emitter
     */
    fun getRequestSecurity(): Protocol {
        return requestSecurity
    }

    /**
     * @return the customPostPath
     */
    fun getCustomPostPath(): String? {
        return customPostPath
    }

    /**
     * @return the emitTimeout
     */
    fun getEmitTimeout(): Int {
        return emitTimeout
    }

    /**
     * @return whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    fun getServerAnonymisation(): Boolean {
        return serverAnonymisation
    }

    /**
     * @return the NetworkConnection if it exists
     */
    fun getNetworkConnection(): NetworkConnection? {
        return networkConnection.get()
    }

    private fun setNetworkConnection(networkConnection: NetworkConnection) {
        this.networkConnection.set(networkConnection)
    }

    fun getCustomRetryForStatusCodes(): Map<Int, Boolean> {
        return customRetryForStatusCodes.get()
    }

    fun setCustomRetryForStatusCodes(customRetryForStatusCodes: Map<Int, Boolean>?) {
        this.customRetryForStatusCodes.set(
            customRetryForStatusCodes ?: HashMap()
        )
    }

    companion object {
        private const val POST_WRAPPER_BYTES =
            88 // "schema":"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3","data":[]
    }
}
