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
}
