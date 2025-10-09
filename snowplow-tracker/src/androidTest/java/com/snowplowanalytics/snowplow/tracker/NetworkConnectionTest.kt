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

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.emitter.TLSVersion
import com.snowplowanalytics.snowplow.network.*
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder
import com.snowplowanalytics.snowplow.payload.Payload
import com.snowplowanalytics.snowplow.payload.TrackerPayload
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class NetworkConnectionTest {
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testGetRequestWithSuccess() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.GET)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        val request = Request(payload, 1)
        val requests: MutableList<Request> = ArrayList(1)
        requests.add(request)
        val results = connection.sendRequests(requests)
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        assertGETRequest(req)

        // Check successful result
        val result = results[0]
        Assert.assertTrue(result.isSuccessful)
        Assert.assertEquals(1, result.eventIds[0]!!.toLong())
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testGetRequestWithNoSuccess() {
        val mockServer = getMockServer(404)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.GET)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        val request = Request(payload, 1)
        val requests: MutableList<Request> = ArrayList(1)
        requests.add(request)
        val results = connection.sendRequests(requests)

        // Check unsuccessful result
        val result = results[0]
        Assert.assertFalse(result.isSuccessful)
        Assert.assertEquals(1, result.eventIds[0]!!.toLong())
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testPostRequestWithSuccess() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        val request = Request(payload, 1)
        val requests: MutableList<Request> = ArrayList(1)
        requests.add(request)
        val results = connection.sendRequests(requests)
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        try {
            assertPOSTRequest(req)
        } catch (e: JSONException) {
            Assert.fail("JSON body of request malformed: $e")
        }

        // Check successful result
        val result = results[0]
        Assert.assertTrue(result.isSuccessful)
        Assert.assertEquals(1, result.eventIds[0]!!.toLong())
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testPostRequestWithNoSuccess() {
        val mockServer = getMockServer(404)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        val request = Request(payload, 1)
        val requests: MutableList<Request> = ArrayList(1)
        requests.add(request)
        val results = connection.sendRequests(requests)
        mockServer.takeRequest(60, TimeUnit.SECONDS)

        // Check unsuccessful result
        val result = results[0]
        Assert.assertFalse(result.isSuccessful)
        Assert.assertEquals(1, result.eventIds[0]!!.toLong())
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testCustomClientIsUsed() {
        val hasClientBeenUsed = AtomicBoolean(false)
        val client: OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(Interceptor { chain ->
                hasClientBeenUsed.set(true)
                chain.proceed(chain.request())
            })
            .build()
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.GET)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .client(client)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        val request = Request(payload, 1)
        val requests: MutableList<Request> = ArrayList(1)
        requests.add(request)
        connection.sendRequests(requests)
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertNotNull(req)
        Assert.assertTrue(hasClientBeenUsed.get())
        mockServer.close()
    }

    @Test
    fun testFreeEndpoint_GetHttpsUrl() {
        val connection = OkHttpNetworkConnectionBuilder("acme.test.url.com", context)
            .method(HttpMethod.POST)
            .build()
        Assert.assertTrue(connection.uri.toString().startsWith("https://acme.test.url.com"))
    }

    @Test
    fun testHttpsEndpoint_GetHttpsUrl() {
        val connection = OkHttpNetworkConnectionBuilder("https://acme.test.url.com", context)
            .method(HttpMethod.POST)
            .build()
        Assert.assertTrue(connection.uri.toString().startsWith("https://acme.test.url.com"))
    }

    @Test
    fun testHttpEndpoint_GetHttpUrl() {
        val connection = OkHttpNetworkConnectionBuilder("http://acme.test.url.com", context)
            .method(HttpMethod.POST)
            .build()
        Assert.assertTrue(connection.uri.toString().startsWith("http://acme.test.url.com"))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testRequestWithCookies() {
        val mockServer = MockWebServer()
        mockServer.start()
        mockServer.enqueue(
            MockResponse.Builder().addHeader("Set-Cookie", "sp=test").build()
        )
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
            .emitTimeout(10)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 1)))
        mockServer.takeRequest(60, TimeUnit.SECONDS)
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertEquals("sp=test", req!!.headers["Cookie"])
        mockServer.close()
        CollectorCookieJar(context).clear()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testDoesntAddHeaderWithoutServerAnonymisation() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .serverAnonymisation(false)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertNull(req!!.headers["SP-Anonymous"])
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testAddsHeaderForServerAnonymisationForPostRequest() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .serverAnonymisation(true)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertEquals("*", req!!.headers["SP-Anonymous"])
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testAddsHeaderForServerAnonymisationForGetRequest() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.GET)
            .serverAnonymisation(true)
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertEquals("*", req!!.headers["SP-Anonymous"])
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testAddsCustomRequestHeadersForPostRequest() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.POST)
            .requestHeaders(mapOf("foo" to "bar"))
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertEquals("bar", req!!.headers["foo"])
        mockServer.close()
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun testAddsCustomRequestHeadersForGetRequest() {
        val mockServer = getMockServer(200)
        val connection = OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer)!!, context)
            .method(HttpMethod.GET)
            .requestHeaders(mapOf("foo" to "bar"))
            .build()
        val payload: Payload = TrackerPayload()
        payload.add("key", "value")
        connection.sendRequests(listOf(Request(payload, 2)))
        val req = mockServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertEquals("bar", req!!.headers["foo"])
        mockServer.close()
    }

    // Service methods
    private fun assertGETRequest(req: RecordedRequest?) {
        Assert.assertNotNull(req)
        Assert.assertEquals("GET", req!!.method)
        Assert.assertEquals("Keep-Alive", req.headers["Connection"])
        val path = req.url!!.encodedPath + "?" + (req.url!!.encodedQuery ?: "")
        Assert.assertEquals("/i?", path.substring(0, 3))
    }

    @Throws(JSONException::class)
    private fun assertPOSTRequest(req: RecordedRequest?): JSONObject {
        Assert.assertNotNull(req)
        Assert.assertEquals("POST", req!!.method)
        Assert.assertEquals("application/json; charset=utf-8", req.headers["Content-Type"])
        Assert.assertEquals("Keep-Alive", req.headers["Connection"])
        Assert.assertEquals("/com.snowplowanalytics.snowplow/tp2", req.url!!.encodedPath)
        val payload = JSONObject(req.body!!.utf8())
        Assert.assertEquals(1, payload.length().toLong())
        Assert.assertEquals(
            "value",
            payload.getString("key")
        )
        return payload
    }

    // Mock Server
    @Throws(IOException::class)
    fun getMockServer(responseCode: Int): MockWebServer {
        val mockServer = MockWebServer()
        mockServer.start()
        val mockedResponse = MockResponse.Builder()
            .code(responseCode)
            .body("{}")
            .build()
        mockServer.enqueue(mockedResponse)
        return mockServer
    }

    @SuppressLint("DefaultLocale")
    fun getMockServerURI(mockServer: MockWebServer?): String? {
        return if (mockServer != null) {
            String.format("http://%s:%d", mockServer.hostName, mockServer.port)
        } else null
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
