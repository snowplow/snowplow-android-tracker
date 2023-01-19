package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface
import com.snowplowanalytics.snowplow.emitter.EmitterDefaults
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

/**
 * It allows the tracker configuration from the emission perspective.
 * The EmitterConfiguration can be used to setup details about how the tracker should treat the events
 * to emit to the collector.     
 * 
 * Default values:
 * bufferOption = BufferOption.DefaultGroup;
 * emitRange = 150;
 * threadPoolSize = 15;
 * byteLimitGet = 40000;
 * byteLimitPost = 40000;
 * serverAnonymisation = false;
 */
open class EmitterConfiguration : Configuration, EmitterConfigurationInterface {
    /**
     * @see .bufferOption
     */
    override var bufferOption: BufferOption = EmitterDefaults.bufferOption

    /**
     * @see .emitRange
     */
    override var emitRange: Int = EmitterDefaults.emitRange

    /**
     * @see .threadPoolSize
     */
    override var threadPoolSize: Int = EmitterDefaults.threadPoolSize

    /**
     * @see .byteLimitGet
     */
    override var byteLimitGet: Long = EmitterDefaults.byteLimitGet

    /**
     * @see .byteLimitPost
     */
    override var byteLimitPost: Long = EmitterDefaults.byteLimitPost

    /**
     * @see .requestCallback
     */
    override var requestCallback: RequestCallback? = null

    /**
     * @see .eventStore
     */
    override var eventStore: EventStore? = null

    /**
     * @see .customRetryForStatusCodes
     */
    override var customRetryForStatusCodes: Map<Int, Boolean>? = null

    /**
     * @see .serverAnonymisation
     */
    override var serverAnonymisation: Boolean = EmitterDefaults.serverAnonymisation
    
        
    // Builders
    
    /**
     * Sets whether the buffer should send events instantly or after the buffer
     * has reached it's limit. By default, this is set to BufferOption Default.
     */
    fun bufferOption(bufferOption: BufferOption): EmitterConfiguration {
        this.bufferOption = bufferOption
        return this
    }

    /**
     * Maximum number of events collected from the EventStore to be sent in a request.
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
     * If it's not set the tracker will use a SQLite database as default EventStore.
     */
    fun eventStore(eventStore: EventStore?): EmitterConfiguration {
        this.eventStore = eventStore
        return this
    }

    /**
     * Callback called for each request performed by the tracker to the collector.
     */
    fun requestCallback(requestCallback: RequestCallback?): EmitterConfiguration {
        this.requestCallback = requestCallback
        return this
    }

    /**
     * Custom retry rules for HTTP status codes returned from the Collector.
     * The dictionary is a mapping of integers (status codes) to booleans (true for retry and false for not retry).
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
