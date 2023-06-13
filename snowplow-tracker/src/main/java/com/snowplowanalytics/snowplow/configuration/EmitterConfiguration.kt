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
package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface
import com.snowplowanalytics.core.emitter.EmitterDefaults
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

/**
 * Configure how the tracker should send the events to the collector.     
 * 
 * Default values:
 *   - bufferOption: [BufferOption.DefaultGroup]
 *   - serverAnonymisation: false
 *   - emitRange: 150 - maximum number of events to process at a time
 *   - threadPoolSize: 15
 *   - byteLimitGet: 40000 bytes
 *   - byteLimitPost: 40000 bytes
 */
open class EmitterConfiguration : Configuration, EmitterConfigurationInterface {

    private var _isPaused: Boolean? = null
    internal var isPaused: Boolean
        get() = _isPaused ?: sourceConfig?.isPaused ?: false
        set(value) { _isPaused = value }

    /**
     * Fallback configuration to read from in case requested values are not present in this configuration.
     */
    internal var sourceConfig: EmitterConfiguration? = null

    private var _bufferOption: BufferOption? = null
    override var bufferOption: BufferOption
        get() = _bufferOption ?: sourceConfig?.bufferOption ?: BufferOption.DefaultGroup
        set(value) { _bufferOption = value }

    private var _emitRange: Int? = null
    override var emitRange: Int
        get() = _emitRange ?: sourceConfig?.emitRange ?: EmitterDefaults.emitRange
        set(value) { _emitRange = value }

    private var _threadPoolSize: Int? = null
    override var threadPoolSize: Int
        get() = _threadPoolSize ?: sourceConfig?.threadPoolSize ?: EmitterDefaults.threadPoolSize
        set(value) { _threadPoolSize = value }

    private var _byteLimitGet: Long? = null
    override var byteLimitGet: Long
        get() = _byteLimitGet ?: sourceConfig?.byteLimitGet ?: EmitterDefaults.byteLimitGet
        set(value) { _byteLimitGet = value }

    private var _byteLimitPost: Long? = null
    override var byteLimitPost: Long
        get() = _byteLimitPost ?: sourceConfig?.byteLimitPost ?: EmitterDefaults.byteLimitPost
        set(value) { _byteLimitPost = value }

    private var _requestCallback: RequestCallback? = null
    override var requestCallback: RequestCallback?
        get() = _requestCallback ?: sourceConfig?.requestCallback
        set(value) { _requestCallback = value }

    private var _eventStore: EventStore? = null
    override var eventStore: EventStore?
        get() = _eventStore ?: sourceConfig?.eventStore
        set(value) { _eventStore = value }

    private var _customRetryForStatusCodes: Map<Int, Boolean>? = null
    override var customRetryForStatusCodes: Map<Int, Boolean>?
        get() = _customRetryForStatusCodes ?: sourceConfig?.customRetryForStatusCodes
        set(value) { _customRetryForStatusCodes = value }

    private var _serverAnonymisation: Boolean? = null
    override var serverAnonymisation: Boolean
        get() = _serverAnonymisation ?: sourceConfig?.serverAnonymisation ?: EmitterDefaults.serverAnonymisation
        set(value) { _serverAnonymisation = value }
    
    // Builders
    
    /**
     * How many events to send in each request. By default, this is set to [BufferOption.DefaultGroup], 
     * a maximum of 10 events per request.
     */
    fun bufferOption(bufferOption: BufferOption): EmitterConfiguration {
        this.bufferOption = bufferOption
        return this
    }

    /**
     * Maximum number of events collected from the EventStore to be processed into requests at one time. 
     * The number of events per request is set using [EmitterConfiguration.bufferOption].
     */
    fun emitRange(emitRange: Int): EmitterConfiguration {
        this.emitRange = emitRange
        return this
    }

    /**
     * Maximum number of threads working in parallel in the tracker to send requests.
     */
    fun threadPoolSize(threadPoolSize: Int): EmitterConfiguration {
        this.threadPoolSize = threadPoolSize
        return this
    }

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a GET request.
     */
    fun byteLimitGet(byteLimitGet: Int): EmitterConfiguration {
        this.byteLimitGet = byteLimitGet.toLong()
        return this
    }

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a POST request.
     */
    fun byteLimitPost(byteLimitPost: Int): EmitterConfiguration {
        this.byteLimitPost = byteLimitPost.toLong()
        return this
    }

    /**
     * Custom component with full ownership for persisting events before to be sent to the collector.
     * If it's not set the tracker will use a SQLite database as 
     * [default EventStore](com.snowplowanalytics.core.emitter.storage.SQLiteEventStore).
     */
    fun eventStore(eventStore: EventStore?): EmitterConfiguration {
        this.eventStore = eventStore
        return this
    }

    /**
     * Callback called for each request performed by the tracker to the event collector.
     */
    fun requestCallback(requestCallback: RequestCallback?): EmitterConfiguration {
        this.requestCallback = requestCallback
        return this
    }

    /**
     * Custom retry rules for HTTP status codes returned from the collector.
     * The dictionary is a mapping of integers (status codes) to booleans (true for retry and false 
     * for not retry). By default, events in requests that return codes 400, 401, 403, 410, or 422 are
     * not retried and are deleted from the EventStore.
     */
    fun customRetryForStatusCodes(customRetryForStatusCodes: Map<Int, Boolean>?): EmitterConfiguration {
        this.customRetryForStatusCodes = customRetryForStatusCodes
        return this
    }

    /**
     * Whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    fun serverAnonymisation(serverAnonymisation: Boolean): EmitterConfiguration {
        this.serverAnonymisation = serverAnonymisation
        return this
    }

    // Copyable
    override fun copy(): EmitterConfiguration {
        return EmitterConfiguration()
            .bufferOption(bufferOption)
            .emitRange(emitRange)
            .threadPoolSize(threadPoolSize)
            .byteLimitGet(byteLimitGet.toInt())
            .byteLimitPost(byteLimitPost.toInt())
            .eventStore(eventStore)
            .requestCallback(requestCallback)
            .customRetryForStatusCodes(customRetryForStatusCodes)
            .serverAnonymisation(serverAnonymisation)
    }
}
