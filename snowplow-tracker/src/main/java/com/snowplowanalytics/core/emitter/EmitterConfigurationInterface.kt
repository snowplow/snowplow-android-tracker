package com.snowplowanalytics.core.emitter

import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback

interface EmitterConfigurationInterface {
    /**
     * Custom component with full ownership for persisting events before to be sent to the collector.
     * If it's not set the tracker will use a SQLite database as default EventStore.
     */
    val eventStore: EventStore?
    
    /**
     * Whether the buffer should send events instantly or after the buffer
     * has reached it's limit. By default, this is set to BufferOption Default.
     */
    var bufferOption: BufferOption
    
    /**
     * Maximum number of events collected from the EventStore to be sent in a request.
     */
    var emitRange: Int

    /**
     * Maximum number of threads working in parallel in the tracker to send requests.
     */
    val threadPoolSize: Int
    
    /**
     * Maximum amount of bytes allowed to be sent in a payload in a GET request.
     */
    var byteLimitGet: Long
    
    /**
     * Maximum amount of bytes allowed to be sent in a payload in a POST request.
     */
    var byteLimitPost: Long
    
    /**
     * Callback called for each request performed by the tracker to the collector.
     */
    var requestCallback: RequestCallback?

    /**
     * Custom retry rules for HTTP status codes returned from the Collector.
     * The dictionary is a mapping of integers (status codes) to booleans (true for retry and false for not retry).
     */
    var customRetryForStatusCodes: Map<Int, Boolean>?
    
    /**
     * Whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    var isServerAnonymisation: Boolean
}
