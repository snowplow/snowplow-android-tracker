package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

/**
 * It allows the tracker configuration from the emission perspective.
 * The EmitterConfiguration can be used to setup details about how the tracker should treat the events
 * to emit to the collector.
 */
open class EmitterConfiguration : Configuration, EmitterConfigurationInterface {
    /**
     * @see .bufferOption
     */
    @JvmField
    var bufferOption: BufferOption

    /**
     * @see .emitRange
     */
    @JvmField
    var emitRange: Int

    /**
     * @see .threadPoolSize
     */
    @JvmField
    var threadPoolSize: Int

    /**
     * @see .byteLimitGet
     */
    @JvmField
    var byteLimitGet: Long

    /**
     * @see .byteLimitPost
     */
    @JvmField
    var byteLimitPost: Long

    /**
     * @see .requestCallback
     */
    @JvmField
    var requestCallback: RequestCallback? = null

    /**
     * @see .eventStore
     */
    @JvmField
    var eventStore: EventStore? = null

    /**
     * @see .customRetryForStatusCodes
     */
    @JvmField
    var customRetryForStatusCodes: Map<Int, Boolean>? = null

    /**
     * @see .serverAnonymisation
     */
    @JvmField
    var serverAnonymisation: Boolean
    
    // Constructor
    /**
     * It sets a default EmitterConfiguration.
     * Default values:
     * bufferOption = BufferOption.Single;
     * emitRange = 150;
     * threadPoolSize = 15;
     * byteLimitGet = 40000;
     * byteLimitPost = 40000;
     * serverAnonymisation = false;
     */
    init {
        bufferOption = BufferOption.Single
        emitRange = 150
        threadPoolSize = 15
        byteLimitGet = 40000
        byteLimitPost = 40000
        serverAnonymisation = false
    }

    // Getters and Setters
    override fun getEventStore(): EventStore? {
        return eventStore
    }

    override fun getBufferOption(): BufferOption {
        return bufferOption
    }

    override fun setBufferOption(bufferOption: BufferOption) {
        this.bufferOption = bufferOption
    }

    override fun getEmitRange(): Int {
        return emitRange
    }

    override fun setEmitRange(emitRange: Int) {
        this.emitRange = emitRange
    }

    override fun getThreadPoolSize(): Int {
        return threadPoolSize
    }

    fun setThreadPoolSize(threadPoolSize: Int) {
        this.threadPoolSize = threadPoolSize
    }

    override fun getByteLimitGet(): Long {
        return byteLimitGet
    }

    override fun setByteLimitGet(byteLimitGet: Long) {
        this.byteLimitGet = byteLimitGet
    }

    override fun getByteLimitPost(): Long {
        return byteLimitPost
    }

    override fun setByteLimitPost(byteLimitPost: Long) {
        this.byteLimitPost = byteLimitPost
    }

    override fun getRequestCallback(): RequestCallback? {
        return requestCallback
    }

    override fun setRequestCallback(requestCallback: RequestCallback?) {
        this.requestCallback = requestCallback
    }

    override fun getCustomRetryForStatusCodes(): Map<Int, Boolean>? {
        return customRetryForStatusCodes
    }

    override fun setCustomRetryForStatusCodes(customRetryForStatusCodes: Map<Int, Boolean>?) {
        this.customRetryForStatusCodes = customRetryForStatusCodes
    }

    override fun isServerAnonymisation(): Boolean {
        return serverAnonymisation
    }

    override fun setServerAnonymisation(serverAnonymisation: Boolean) {
        this.serverAnonymisation = serverAnonymisation
    }
    
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
