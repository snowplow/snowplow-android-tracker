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
package com.snowplowanalytics.snowplow.network

import android.net.Uri

/**
 * Interface for the component that
 * sends events to the collector.
 */
interface NetworkConnection {
    /**
     * Send requests to the collector.
     * @param requests to send.
     * @return results of the sending operation.
     */
    fun sendRequests(requests: List<Request>): List<RequestResult>

    /**
     * @return http method used to send requests to the collector.
     */
    val httpMethod: HttpMethod

    /**
     * @return URI of the collector.
     */
    val uri: Uri
}
