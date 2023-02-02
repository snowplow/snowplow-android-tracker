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
package com.snowplowanalytics.snowplow.internal.tracker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor.shutdown
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.event.Timing
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.network.Protocol
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.BuildConfig
import com.snowplowanalytics.snowplow.tracker.MockEventStore
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.Basis
import com.snowplowanalytics.snowplow.util.TimeMeasure
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ConfigurationTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val es = shutdown()
        es?.awaitTermination(60, TimeUnit.SECONDS)
    }

    // Tests
    @Test
    fun basicInitialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
        trackerConfiguration.platformContext = true
        val tracker =
            createTracker(context, "namespace", networkConfiguration, trackerConfiguration)
        Assert.assertNotNull(tracker)
        val uri = URI.create(tracker.network!!.endpoint)
        Assert.assertNotNull(uri)
        val host = uri.host
        val scheme = uri.scheme
        val protocol = if (networkConfiguration.protocol === Protocol.HTTP) "http" else "https"
        Assert.assertEquals(networkConfiguration.endpoint, "$scheme://$host")
        Assert.assertEquals(protocol, scheme)
        Assert.assertEquals(trackerConfiguration.appId, tracker.appId)
        Assert.assertEquals("namespace", tracker.namespace)
    }

    @Test
    fun sessionInitialization() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedForeground = TimeMeasure(42, TimeUnit.SECONDS)
        val expectedBackground = TimeMeasure(24, TimeUnit.SECONDS)
        val networkConfig = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfig = TrackerConfiguration("appId")
        val sessionConfig = SessionConfiguration(expectedForeground, expectedBackground)
        val tracker =
            createTracker(context, "namespace", networkConfig, trackerConfig, sessionConfig)
        val foreground = tracker.session!!.foregroundTimeout
        val background = tracker.session!!.backgroundTimeout
        Assert.assertEquals(expectedForeground, foreground)
        Assert.assertEquals(expectedBackground, background)
    }

    @Test
    fun sessionControllerUnavailableWhenContextTurnedOff() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
        trackerConfiguration.sessionContext = true
        var tracker =
            createTracker(context, "namespace", networkConfiguration, trackerConfiguration)
        Assert.assertNotNull(tracker.session)
        trackerConfiguration.sessionContext = false
        tracker = createTracker(context, "namespace", networkConfiguration, trackerConfiguration)
        Assert.assertNull(tracker.session)
    }

    @Test
    @Throws(InterruptedException::class)
    fun sessionConfigurationCallback() {
        val expectation = Any() as Object
        val callbackExecuted = AtomicBoolean(false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Remove stored session data
        val sharedPreferences = context.getSharedPreferences(
            TrackerConstants.SNOWPLOW_SESSION_VARS + "_namespace",
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().remove(TrackerConstants.SESSION_STATE).commit()

        // Configure tracker
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
            .sessionContext(true)
        val sessionConfiguration = SessionConfiguration(
            TimeMeasure(100, TimeUnit.SECONDS),
            TimeMeasure(100, TimeUnit.SECONDS)
        )
            .onSessionUpdate { sessionState: SessionState ->
                Assert.assertEquals(1, sessionState.sessionIndex.toLong())
                Assert.assertNull(sessionState.previousSessionId)
                callbackExecuted.set(true)
                synchronized(expectation) { expectation.notify() }
            }
        val tracker = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            sessionConfiguration
        )
        tracker.track(Timing("cat", "var", 123))
        synchronized(expectation) { expectation.wait(10000) }
        Assert.assertTrue(callbackExecuted.get())
    }

    @Test
    @Throws(InterruptedException::class)
    fun sessionConfigurationCallbackAfterNewSession() {
        val expectation1 = Any() as Object
        val expectation2 = Any() as Object
        val callbackExecuted = AtomicBoolean(false)
        val sessionId = AtomicReference<String?>(null)
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Remove stored session data
        val sharedPreferences = context.getSharedPreferences(
            TrackerConstants.SNOWPLOW_SESSION_VARS + "_namespace",
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().remove(TrackerConstants.SESSION_STATE).commit()

        // Configure tracker
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
            .sessionContext(true)
        val sessionConfiguration = SessionConfiguration(
            TimeMeasure(100, TimeUnit.SECONDS),
            TimeMeasure(100, TimeUnit.SECONDS)
        )
            .onSessionUpdate { sessionState: SessionState ->
                if (sessionState.sessionIndex == 1) {
                    Assert.assertNull(sessionState.previousSessionId)
                    sessionId.set(sessionState.sessionId)
                    synchronized(expectation1) { expectation1.notify() }
                } else {
                    Assert.assertEquals(2, sessionState.sessionIndex.toLong())
                    Assert.assertEquals(sessionId.get(), sessionState.previousSessionId)
                    callbackExecuted.set(true)
                    synchronized(expectation2) { expectation2.notify() }
                }
            }
        val tracker = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            sessionConfiguration
        )
        tracker.track(Timing("cat", "var", 123))
        synchronized(expectation1) {
            // This delay is needed because the session manager doesn't manage correclty the sequence of the events
            // in a multithreading model when the throughput is high.
            // TODO: To fix this issue we have to refactor the session manager to work like ScreenStateMachine where
            // it correctly manage the state attached to the tracked events.
            expectation1.wait(3000)
        }
        tracker.session!!.startNewSession()
        tracker.track(Timing("cat", "var", 123))
        synchronized(expectation2) { expectation2.wait(3000) }
        Assert.assertTrue(callbackExecuted.get())
    }

    // TODO: Flaky test to fix
    /*
    @Test
    public void emitterConfiguration() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid");
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .bufferOption(BufferOption.DefaultGroup)
                .byteLimitGet(10000)
                .emitRange(10);
        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, emitterConfiguration);
        tracker.pause(); // To block the flush operation that would turn isRunning flag on.
        EmitterController emitterController = tracker.getEmitter();

        assertEquals(BufferOption.DefaultGroup, emitterController.getBufferOption());
        assertEquals(10000, emitterController.getByteLimitGet());
        assertEquals(10, emitterController.getEmitRange());

        // Thread.sleep(1000);  // To allow the tracker completing the flush operation and turning isRunning off

        emitterController.setBufferOption(BufferOption.HeavyGroup);
        emitterController.setByteLimitGet(100);
        emitterController.setEmitRange(1);
        assertEquals(BufferOption.HeavyGroup, emitterController.getBufferOption());
        assertEquals(100, emitterController.getByteLimitGet());
        assertEquals(1, emitterController.getEmitRange());
    }
    */
    @Test
    @Throws(InterruptedException::class)
    fun trackerVersionSuffix() {
        val trackerConfiguration = TrackerConfiguration("appId")
            .base64encoding(false)
            .installAutotracking(false)
            .trackerVersionSuffix("test With Space 1-2-3")

        // Setup tracker
        val eventStore = MockEventStore()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val emitterConfiguration = EmitterConfiguration()
            .eventStore(eventStore)
            .threadPoolSize(10)
        val trackerController = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            emitterConfiguration
        )

        // Track fake event
        trackerController.track(Structured("category", "action"))
        var i = 0
        while (eventStore.size() < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        val events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        val payload = events[0]!!.payload

        // Check v_tracker field
        val versionTracker = payload.map["tv"] as String?
        val expected = BuildConfig.TRACKER_LABEL + " testWithSpace1-2-3"
        Assert.assertEquals(expected, versionTracker)
    }

    @Test
    @Throws(InterruptedException::class)
    fun gdprConfiguration() {
        val eventStore = MockEventStore()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
            .base64encoding(false)
        val emitterConfiguration = EmitterConfiguration()
            .eventStore(eventStore)
            .threadPoolSize(10)
        val gdprConfiguration = GdprConfiguration(Basis.CONSENT, "id", "ver", "desc")
        val trackerController = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            gdprConfiguration,
            emitterConfiguration
        )
        val gdprController = trackerController.gdpr

        // Check gdpr settings
        Assert.assertEquals(Basis.CONSENT, gdprController.basisForProcessing)
        Assert.assertEquals("id", gdprController.documentId)

        // Check gdpr settings reset
        gdprController.reset(Basis.CONTRACT, "id1", "ver1", "desc1")
        Assert.assertEquals(Basis.CONTRACT, gdprController.basisForProcessing)
        Assert.assertEquals("id1", gdprController.documentId)
        Assert.assertTrue(gdprController.isEnabled)

        // Check gdpr context added
        trackerController.track(Structured("category", "action"))
        run {
            var i = 0
            while (eventStore.size() < 1 && i < 10) {
                Thread.sleep(1000)
                i++
            }
        }
        var events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        var payload = events[0]!!.payload
        var contexts = payload.map["co"] as String?
        Assert.assertTrue(contexts!!.contains("\"basisForProcessing\":\"contract\""))
        Assert.assertTrue(contexts.contains("\"documentId\":\"id1\""))

        // Check gdpr disabled
        gdprController.disable()
        Assert.assertFalse(gdprController.isEnabled)
        Assert.assertEquals(Basis.CONTRACT, gdprController.basisForProcessing)
        Assert.assertEquals("id1", gdprController.documentId)

        // Check gdpr context not added
        trackerController.track(Structured("category", "action"))
        var i = 0
        while (eventStore.size() < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        payload = events[0]!!.payload
        contexts = payload.map["co"] as String?
        Assert.assertFalse(contexts!!.contains("\"basisForProcessing\":\"contract\""))
        Assert.assertFalse(contexts.contains("\"documentId\":\"id1\""))

        // Check gdpr enabled again
        gdprController.enable()
        Assert.assertTrue(gdprController.isEnabled)
    }

    @Test
    @Throws(InterruptedException::class)
    fun withoutGdprConfiguration() {
        val eventStore = MockEventStore()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
            .base64encoding(false)
        val emitterConfiguration = EmitterConfiguration()
            .eventStore(eventStore)
            .threadPoolSize(10)
        val trackerController = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            emitterConfiguration
        )
        val gdprController = trackerController.gdpr

        // Check gdpr settings
        Assert.assertFalse(gdprController.isEnabled)

        // Check gdpr context not added
        trackerController.track(Structured("category", "action"))
        run {
            var i = 0
            while (eventStore.size() < 1 && i < 10) {
                Thread.sleep(1000)
                i++
            }
        }
        var events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        var payload = events[0]!!.payload
        var contexts = payload.map["co"] as String?
        Assert.assertFalse(contexts!!.contains("\"basisForProcessing\""))

        // Check gdpr can be enabled again
        gdprController.reset(Basis.CONTRACT, "id2", "1", "desc")
        gdprController.enable()
        Assert.assertEquals(Basis.CONTRACT, gdprController.basisForProcessing)
        Assert.assertEquals("id2", gdprController.documentId)
        Assert.assertTrue(gdprController.isEnabled)

        // Check gdpr context added
        trackerController.track(Structured("category", "action"))
        var i = 0
        while (eventStore.size() < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        payload = events[0]!!.payload
        contexts = payload.map["co"] as String?
        Assert.assertTrue(contexts!!.contains("\"basisForProcessing\":\"contract\""))
        Assert.assertTrue(contexts.contains("\"documentId\":\"id2\""))
    }

    @Test
    @Throws(InterruptedException::class)
    fun globalContextsConfiguration() {
        val eventStore = MockEventStore()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val trackerConfiguration = TrackerConfiguration("appid")
            .base64encoding(false)
        val emitterConfiguration = EmitterConfiguration()
            .eventStore(eventStore)
        val gcConfiguration = GlobalContextsConfiguration(null)
        gcConfiguration.add(
            "k1",
            GlobalContext(listOf(SelfDescribingJson("schema", object : HashMap<String?, Any?>() {
                init {
                    put("key", "value1")
                }
            })))
        )
        val trackerController = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            gcConfiguration,
            emitterConfiguration
        )
        val gcController = trackerController.globalContexts

        // Check global contexts settings
        Assert.assertEquals(mutableSetOf("k1"), gcController.tags)

        // Add new global context
        gcController.add(
            "k2",
            GlobalContext(listOf(SelfDescribingJson("schema", object : HashMap<String?, Any?>() {
                init {
                    put("key", "value2")
                }
            })))
        )
        Assert.assertEquals(mutableSetOf("k1", "k2"), gcController.tags)

        // Check global context added to event
        trackerController.track(Structured("category", "action"))
        var i = 0
        while (eventStore.size() < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        val events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        val payload = events[0]!!.payload
        val contexts = payload.map["co"] as String?
        Assert.assertTrue(contexts!!.contains("value1"))
        Assert.assertTrue(contexts.contains("value2"))
    }

    @Test
    fun activatesServerAnonymisationInEmitter() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfig = NetworkConfiguration("example.com")
        val emitterConfig = EmitterConfiguration()
        emitterConfig.serverAnonymisation(true)
        val tracker = createTracker(context, Math.random().toString(), networkConfig, emitterConfig)
        Assert.assertTrue(tracker.emitter.serverAnonymisation)
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun anonymisesUserIdentifiersIfAnonymousUserTracking() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfig = NetworkConfiguration(networkConnection)
        val trackerConfig = TrackerConfiguration("app1")
        trackerConfig.userAnonymisation = true
        trackerConfig.sessionContext = true
        trackerConfig.platformContext = true
        trackerConfig.base64encoding = false
        removeAllTrackers()
        val tracker = createTracker(context, Math.random().toString(), networkConfig, trackerConfig)
        Assert.assertTrue(tracker.userAnonymisation)
        tracker.track(Structured("category", "action"))
        run {
            var i = 0
            while (i < 10 && networkConnection.countRequests() == 0) {
                Thread.sleep(1000)
                i++
            }
        }
        TestCase.assertEquals(1, networkConnection.countRequests())
        val request = networkConnection.allRequests[0]
        val entities = JSONObject(request.payload.map["co"] as String)
            .getJSONArray("data")
        var sessionContext: JSONObject? = null
        var platformContext: JSONObject? = null
        for (i in 0 until entities.length()) {
            if (entities.getJSONObject(i).getString("schema") == TrackerConstants.SESSION_SCHEMA) {
                sessionContext = entities.getJSONObject(i).getJSONObject("data")
            } else if (entities.getJSONObject(i)
                    .getString("schema") == TrackerConstants.MOBILE_SCHEMA
            ) {
                platformContext = entities.getJSONObject(i).getJSONObject("data")
            }
        }
        Assert.assertEquals(
            "00000000-0000-0000-0000-000000000000",
            sessionContext!!.getString("userId")
        )
        Assert.assertFalse(platformContext!!.has("androidIdfa"))
    }

    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun trackerReturnsTrackedEventId() {
        // Setup tracker
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val networkConfig = NetworkConfiguration(networkConnection)
        removeAllTrackers()
        val tracker = createTracker(context, Math.random().toString(), networkConfig)

        // Track event
        val eventId = tracker.track(Structured("category", "action"))
        var i = 0
        while (i < 100 && networkConnection.countRequests() == 0) {
            Thread.sleep(1000)
            i++
        }
        Assert.assertEquals(1, networkConnection.countRequests())
        val request = networkConnection.allRequests[0]

        // Check eid field
        val trackedEventId = request.payload.map["eid"] as String?
        Assert.assertEquals(eventId.toString(), trackedEventId)
    }
}
