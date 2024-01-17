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

import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.emitter.EventStore
import com.snowplowanalytics.snowplow.network.RequestCallback
import kotlin.time.Duration

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
    var serverAnonymisation: Boolean

    /**
     * Whether to retry sending events that failed to be sent to the collector.
     *
     * If disabled, events that failed to be sent will be dropped regardless of other configuration (such as the customRetryForStatusCodes).
     */
    var retryFailedRequests: Boolean

    /**
     * Limit for the maximum duration of how long events should be kept in the event store if they fail to be sent.
     * Defaults to 30 days.
     */
    var maxEventStoreAge: Duration

    /**
     * Limit for the maximum number of unsent events to keep in the event store.
     * Defaults to 1000.
     */
    var maxEventStoreSize: Long
}
