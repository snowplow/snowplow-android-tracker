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
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.session.AppStateProvider
import com.snowplowanalytics.core.utils.NotificationCenter
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.configuration.SignalsConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class InterventionsTest {

    private lateinit var server: MockWebServer
    private lateinit var endpoint: String

    @Before
    fun setUp() {
        // The test process has no Activity, so mark the app as in the foreground.
        AppStateProvider.onStart(ProcessLifecycleOwner.get())
        AppStateProvider.markSeededForTests()
        server = MockWebServer()
        server.start()
        endpoint = "http://${server.hostName}:${server.port}"
    }

    @After
    fun tearDown() {
        removeAllTrackers()
        server.close()
        Executor.shutdown()
    }

    @Test
    fun subscribesWithSessionIdentifiersAndDeliversIntervention() {
        // Arrange
        enqueueStream(interventionMessage("int-1"))
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))
        val received = LinkedBlockingQueue<Intervention>()

        // Act
        tracker.signals.addInterventionHandler("handler") { received.add(it) }

        // Assert
        val intervention = received.poll(5, TimeUnit.SECONDS)
        assertEquals(
            Intervention(
                interventionId = "int-1",
                name = "discount",
                version = 2,
                attributes = mapOf("page_views" to 5, "last_page" to null),
                targetAttributeKey = InterventionAttributeKey("client_session_user_id", TARGET_ID)
            ),
            intervention
        )
        val request = takeRequest()!!
        assertEquals("/api/v1/interventions", request.url.encodedPath)
        assertEquals(tracker.session?.userId, request.url.queryParameter("client_session_user_id"))
        assertEquals(tracker.session?.sessionId, request.url.queryParameter("client_session_session_id"))
        assertEquals("text/event-stream", request.headers["Accept"])
    }

    @Test
    fun tracksReceiveAndHandleEvents() {
        // Arrange
        enqueueStream(interventionMessage("int-1"))
        val events = recordEvents()
        val tracker = createTracker(SignalsConfiguration(endpoint), events.plugin)
        val handled = CountDownLatch(2)
        tracker.signals.addInterventionHandler("ok") { handled.countDown() }
        tracker.signals.addInterventionHandler("failing") {
            handled.countDown()
            throw IllegalStateException("boom")
        }

        // Act: the first event starts the session, which starts the subscription
        tracker.track(Structured("cat", "act"))

        // Assert
        assertTrue(handled.await(5, TimeUnit.SECONDS))
        val receive = events.await(RECEIVE)
        assertEquals(listOf("ok", "failing"), receive.payload["handlers"])
        val instance = receive.entities.first { it.map["schema"] == INSTANCE }.map["data"] as Map<*, *>
        assertEquals("int-1", instance["intervention_id"])
        assertEquals("discount", instance["name"])
        assertEquals(2, instance["version"])
        assertEquals(
            mapOf("name" to "client_session_user_id", "id" to TARGET_ID),
            instance["attributeKey"]
        )
        assertEquals(
            setOf(mapOf("attribute" to "page_views", "value" to 5), mapOf("attribute" to "last_page", "value" to null)),
            (instance["attributes"] as List<*>).toSet()
        )
        assertEquals("ok", events.await(HANDLE).payload["handler"])
        val error = events.await(HANDLE_ERROR)
        assertEquals("failing", error.payload["handler"])
        assertEquals("java.lang.IllegalStateException: boom", error.payload["error"])
    }

    @Test
    fun waitsForFirstEventBeforeSubscribing() {
        // Arrange
        enqueueOpenStream()
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.signals.addInterventionHandler("handler") {}
        assertNull(takeRequest(1))

        // Act
        tracker.track(Structured("cat", "act"))

        // Assert
        val request = takeRequest()
        assertNotNull(request)
        assertEquals(tracker.session?.sessionId, request!!.url.queryParameter("client_session_session_id"))
    }

    @Test
    fun doesNotSubscribeWhenUserAnonymisationIsOn() {
        // Arrange
        enqueueOpenStream()
        val tracker = createTracker(
            SignalsConfiguration(endpoint),
            TrackerConfiguration("app-id").userAnonymisation(true)
        )
        tracker.track(Structured("cat", "act"))

        // Act
        tracker.signals.addInterventionHandler("handler") {}
        tracker.track(Structured("cat", "act"))

        // Assert
        assertNull(takeRequest(2))
    }

    @Test
    fun resubscribesWhenSessionChanges() {
        // Arrange
        enqueueOpenStream()
        enqueueOpenStream()
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))
        tracker.signals.addInterventionHandler("handler") {}
        val first = takeRequest()!!

        // Act
        tracker.session!!.startNewSession()
        tracker.track(Structured("cat", "act"))

        // Assert
        val second = takeRequest()!!
        assertNotEquals(
            first.url.queryParameter("client_session_session_id"),
            second.url.queryParameter("client_session_session_id")
        )
        assertEquals(tracker.session?.sessionId, second.url.queryParameter("client_session_session_id"))
    }

    @Test
    fun reconnectsAfterStreamCloses() {
        // Arrange
        enqueueStream(": connected")
        enqueueOpenStream()
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))

        // Act
        tracker.signals.addInterventionHandler("handler") {}

        // Assert
        assertNotNull(takeRequest())
        assertNotNull(takeRequest())
    }

    @Test
    fun disconnectsInBackgroundAndReconnectsInForeground() {
        // Arrange
        enqueueOpenStream()
        enqueueOpenStream()
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))
        tracker.signals.addInterventionHandler("handler") {}
        assertNotNull(takeRequest())

        // Act
        postLifecycle(isForeground = false)

        // Assert: no reconnect while in the background
        assertNull(takeRequest(2))

        // Act
        postLifecycle(isForeground = true)

        // Assert
        assertNotNull(takeRequest())
    }

    @Test
    fun unsubscribesWhenLastHandlerIsRemoved() {
        // Arrange
        enqueueStream(": connected")
        enqueueOpenStream()
        val tracker = createTracker(SignalsConfiguration(endpoint))
        tracker.track(Structured("cat", "act"))
        tracker.signals.addInterventionHandler("handler") {}
        assertNotNull(takeRequest())

        // Act
        tracker.signals.removeInterventionHandler("handler")

        // Assert: the closed stream isn't reopened
        assertNull(takeRequest(3))
    }

    @Test
    fun ignoresHandlersWithoutSignalsConfiguration() {
        // Arrange
        val tracker = createTracker()
        tracker.track(Structured("cat", "act"))

        // Act
        tracker.signals.addInterventionHandler("handler") {}

        // Assert
        assertNull(takeRequest(1))
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

    private fun interventionMessage(id: String): String {
        return "data: " + """{"intervention_id": "$id", "name": "discount", "version": 2, """ +
            """"attributes": {"page_views": 5, "last_page": null}, """ +
            """"target_attribute_key": {"name": "client_session_user_id", "id": "$TARGET_ID"}}"""
    }

    /** A stream that sends the given lines and then closes. */
    private fun enqueueStream(vararg lines: String) {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "text/event-stream")
                .body(lines.joinToString("") { "$it\n\n" })
                .build()
        )
    }

    /** A stream that stays open without sending anything. */
    private fun enqueueOpenStream() {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "text/event-stream")
                .body(": connected\n\n")
                .bodyDelay(60, TimeUnit.SECONDS)
                .build()
        )
    }

    private fun takeRequest(timeoutSeconds: Long = 5): RecordedRequest? {
        return server.takeRequest(timeoutSeconds, TimeUnit.SECONDS)
    }

    private fun postLifecycle(isForeground: Boolean) {
        NotificationCenter.postNotification(
            "SnowplowLifecycleTracking",
            mapOf("isForeground" to isForeground)
        )
    }

    private class RecordedEvents {
        val events: MutableList<InspectableEvent> = Collections.synchronizedList(mutableListOf())
        val plugin = PluginConfiguration("recorder")
            .afterTrack(listOf(RECEIVE, HANDLE, HANDLE_ERROR)) { events.add(it) }

        fun await(schema: String): InspectableEvent {
            repeat(50) {
                synchronized(events) { events.firstOrNull { it.schema == schema } }?.let { return it }
                Thread.sleep(100)
            }
            throw AssertionError("No $schema event was tracked")
        }
    }

    private fun recordEvents() = RecordedEvents()

    companion object {
        private const val RECEIVE = "iglu:com.snowplowanalytics.signals/intervention_receive/jsonschema/1-0-0"
        private const val HANDLE = "iglu:com.snowplowanalytics.signals/intervention_handle/jsonschema/1-0-0"
        private const val HANDLE_ERROR = "iglu:com.snowplowanalytics.signals/intervention_handle_error/jsonschema/1-0-0"
        private const val TARGET_ID = "7d5a2a8e-0b3c-4a43-a5a3-3b0f2b0c7f6e"
        private const val INSTANCE = "iglu:com.snowplowanalytics.signals/intervention_instance/jsonschema/1-0-0"
    }
}
