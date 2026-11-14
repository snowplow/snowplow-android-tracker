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

package com.snowplowanalytics.snowplow.signals

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.SignalsConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ServiceAttributesTest {

    private lateinit var server: MockWebServer
    private lateinit var endpoint: String

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        endpoint = "http://${server.hostName}:${server.port}"
    }

    @After
    fun tearDown() {
        server.close()
        removeAllTrackers()
        Executor.shutdown()
    }

    @Test
    fun readsAttributesForCurrentUser() {
        // Arrange
        enqueue(200, """{"attributes": {"page_views": 3, "last_page": null, "pages": ["a", "b"]}}""")
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(3, result.attributes?.get("page_views"))
        assertTrue(result.attributes!!.containsKey("last_page"))
        assertNull(result.attributes!!["last_page"])
        assertEquals(listOf("a", "b"), result.attributes!!["pages"])
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/services/my_service", request.url.encodedPath)
        assertEquals(tracker.session?.userId, request.url.queryParameter("client_session_user_id"))
        assertEquals(1, request.url.querySize)
    }

    @Test
    fun readsAttributesForCurrentSession() {
        // Arrange
        enqueue(200, """{"attributes": {"page_views": 1}}""")
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await {
            tracker.signals.getServiceAttributes("my_service", SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID, it)
        }

        // Assert
        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertNotNull(tracker.session?.sessionId)
        assertEquals(tracker.session?.sessionId, request.url.queryParameter("client_session_session_id"))
    }

    @Test
    fun readsAttributesForExplicitIdentifierAndEncodesServiceName() {
        // Arrange
        enqueue(200, """{"attributes": {}}""")
        val tracker = createTracker(SignalsConfiguration("$endpoint/prefix/"))
        val identifier = "7d5a2a8e-0b3c-4a43-a5a3-3b0f2b0c7f6e"

        // Act
        val result = await {
            tracker.signals.getServiceAttributes("my service/1", "custom_key", identifier, it)
        }

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(emptyMap<String, Any?>(), result.attributes)
        val request = server.takeRequest()
        assertEquals("/prefix/api/v1/services/my%20service%2F1", request.url.encodedPath)
        assertEquals(identifier, request.url.queryParameter("custom_key"))
    }

    @Test
    fun surfacesSignalsErrorResponse() {
        // Arrange
        enqueue(404, """{"error": "service not found"}""")
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("private_service", it) }

        // Assert
        assertFalse(result.isSuccess)
        assertNull(result.attributes)
        assertEquals(SignalsError(404, "service not found"), result.error)
    }

    @Test
    fun surfacesErrorMessageContainingPercentSignWithLoggingOn() {
        // Arrange
        enqueue(400, """{"error": "100% invalid identifier"}""")
        val tracker = createTracker(
            SignalsConfiguration(endpoint),
            TrackerConfiguration("app-id").logLevel(LogLevel.DEBUG)
        )
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(SignalsError(400, "100% invalid identifier"), result.error)
    }

    @Test
    fun surfacesStatusWhenErrorBodyIsNotJson() {
        // Arrange
        enqueue(502, "Bad gateway")
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(502, result.error?.status)
    }

    @Test
    fun failsWithoutResponse() {
        // Arrange
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))
        server.close()

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(0, result.error?.status)
    }

    @Test
    fun failsWithoutSignalsConfiguration() {
        // Arrange
        val tracker = createTracker()
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(0, result.error?.status)
        assertTrue(result.error!!.message.contains("SignalsConfiguration"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failsForSessionIdBeforeFirstEvent() {
        // Arrange
        val tracker = createTracker(SignalsConfiguration(endpoint))

        // Act
        val result = await {
            tracker.signals.getServiceAttributes("my_service", SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID, it)
        }

        // Assert
        assertEquals(0, result.error?.status)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failsWhenUserAnonymisationIsOn() {
        // Arrange
        val tracker = createTracker(
            SignalsConfiguration(endpoint),
            TrackerConfiguration("app-id").userAnonymisation(true)
        )
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(0, result.error?.status)
        assertTrue(result.error!!.message.contains("anonymisation"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failsWhenSessionIsDisabled() {
        // Arrange
        val tracker = createTracker(
            SignalsConfiguration(endpoint),
            TrackerConfiguration("app-id").sessionContext(false)
        )
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", it) }

        // Assert
        assertEquals(0, result.error?.status)
        assertTrue(result.error!!.message.contains("Session tracking is disabled"))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun failsForCustomKeyWithoutIdentifier() {
        // Arrange
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        val result = await { tracker.signals.getServiceAttributes("my_service", "custom_key", it) }

        // Assert
        assertEquals(0, result.error?.status)
        assertEquals(0, server.requestCount)
    }

    // --- PRIVATE

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(vararg configurations: Configuration): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return Snowplow.createTracker(
            context,
            namespace = "ns" + Math.random().toString(),
            network = networkConfig,
            configurations = configurations
        )
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build()
        )
    }

    private fun await(call: (androidx.core.util.Consumer<ServiceAttributesResult>) -> Unit): ServiceAttributesResult {
        val latch = CountDownLatch(1)
        var result: ServiceAttributesResult? = null
        call { result = it; latch.countDown() }
        assertTrue("Timed out waiting for the result", latch.await(10, TimeUnit.SECONDS))
        return result!!
    }
}
