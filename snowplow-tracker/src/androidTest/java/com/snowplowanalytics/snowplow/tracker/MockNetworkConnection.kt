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
package com.snowplowanalytics.snowplow.tracker

import android.net.Uri
import com.snowplowanalytics.core.tracker.Logger.v
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.NetworkConnection
import com.snowplowanalytics.snowplow.network.Request
import com.snowplowanalytics.snowplow.network.RequestResult
import kotlin.Int

class MockNetworkConnection(override var httpMethod: HttpMethod, var statusCode: Int) :
    NetworkConnection {
    private val previousRequests: MutableList<List<Request>> = ArrayList()
    val previousResults: MutableList<List<RequestResult>> = ArrayList()
    fun sendingCount(): Int {
        return previousResults.size
    }

    override fun sendRequests(requests: List<Request>): List<RequestResult> {
        val requestResults: MutableList<RequestResult> = ArrayList(requests.size)
        for (request in requests) {
            val result = RequestResult(statusCode, request.oversize, request.emitterEventIds)
            v(
                "MockNetworkConnection",
                "Sent: %s with success: %s",
                request.emitterEventIds,
                result.isSuccessful.toString().toBoolean()
            )
            requestResults.add(result)
        }
        previousRequests.add(requests)
        previousResults.add(requestResults)
        return requestResults
    }

    override val uri: Uri
        get() = Uri.parse("http://fake-url.com")
    val allRequests: List<Request>
        get() {
            val flattened: MutableList<Request> = ArrayList()
            for (requests in previousRequests) {
                flattened.addAll(requests)
            }
            return flattened
        }

    fun countRequests(): Int {
        return allRequests.size
    }

    fun clear() {
        previousRequests.clear()
        previousResults.clear()
    }
}
