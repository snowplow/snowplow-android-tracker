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
package com.snowplowanalytics.snowplow.internal.tracker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.emitter.Executor.shutdown
import com.snowplowanalytics.core.emitter.Executor.threadCount
import com.snowplowanalytics.core.tracker.ExceptionHandler
import com.snowplowanalytics.core.tracker.Subject
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.core.tracker.TrackerEvent
import com.snowplowanalytics.snowplow.TestUtils
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.event.Timing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@RunWith(AndroidJUnit4::class)
class TrackerTest {
    @Before
    @Synchronized
    @Throws(Exception::class)
    fun setUp() {
        try {
            if (Companion.tracker == null) return
            val emitter = Companion.tracker!!.emitter
            val eventStore = emitter.eventStore
            if (eventStore != null) {
                val isClean = eventStore.removeAllEvents()
                Log.i("TrackerTest", "EventStore cleaned: $isClean")
                Log.i("TrackerTest", "Events in the store: " + eventStore.size())
            } else {
                Log.i("TrackerTest", "EventStore null")
            }
            emitter.shutdown(30)
            Companion.tracker!!.close()
            Log.i("TrackerTest", "Tracker closed")
        } catch (e: IllegalStateException) {
            Log.i("TrackerTest", "Tracker already closed.")
        }
    }

    // Helper Methods
    private val tracker: Tracker?
        get() = getTracker(false)

    @Synchronized
    private fun getTracker(installTracking: Boolean): Tracker? {
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val builder = Consumer { emitter: Emitter ->
            emitter.emitterTick = 0
            emitter.emptyLimit = 0
        }
        val emitter = Emitter(
            context, "testUrl", builder
        )
        val subject = Subject(
            context, null
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.subject = subject
            tracker.platform = DevicePlatform.InternetOfThings
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.threadCount = 1
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.geoLocationContext = false
            tracker.foregroundTimeout = 5
            tracker.backgroundTimeout = 5
            tracker.timeUnit = TimeUnit.SECONDS
            tracker.exceptionAutotracking = false
            tracker.lifecycleAutotracking = true
            tracker.installAutotracking = installTracking
            tracker.applicationContext = true
        }
        Companion.tracker = Tracker(emitter, "myNamespace", "myAppId", context, trackerBuilder)
        return Companion.tracker
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    // Tests
    @Test
    fun testSetValues() {
        val tracker = getTracker(true)
        Assert.assertEquals("myNamespace", tracker!!.namespace)
        Assert.assertEquals("myAppId", tracker.appId)
        Assert.assertEquals(DevicePlatform.InternetOfThings, tracker.platform)
        Assert.assertFalse(tracker.base64Encoded)
        Assert.assertNotNull(tracker.emitter)
        Assert.assertNotNull(tracker.subject)
        Assert.assertEquals(LogLevel.VERBOSE, tracker.logLevel)
        Assert.assertEquals(2, tracker.threadCount.toLong())
        Assert.assertFalse(tracker.exceptionAutotracking)
        Assert.assertTrue(tracker.lifecycleAutotracking)
        Assert.assertTrue(tracker.installAutotracking)
        Assert.assertTrue(tracker.applicationContext)
    }

    @Test
    fun testEmitterUpdate() {
        val tracker = tracker
        Assert.assertNotNull(tracker!!.emitter)
        tracker.emitter = Emitter(context, "test", null)
        Assert.assertNotNull(tracker.emitter)
    }

    @Test
    fun testSubjectUpdate() {
        val tracker = tracker
        Assert.assertNotNull(tracker!!.subject)
        tracker.subject = null
        Assert.assertNull(tracker.subject)
    }

    @Test
    fun testPlatformUpdate() {
        val tracker = tracker
        Assert.assertEquals(DevicePlatform.InternetOfThings, tracker!!.platform)
        tracker.platform = DevicePlatform.Mobile
        Assert.assertEquals(DevicePlatform.Mobile, tracker.platform)
    }

    @Test
    fun testDataCollectionSwitch() {
        val tracker = tracker
        Assert.assertTrue(tracker!!.dataCollection)
        tracker.pauseEventTracking()
        Assert.assertFalse(tracker.dataCollection)
        tracker.pauseEventTracking()
        Assert.assertFalse(tracker.dataCollection)
        tracker.resumeEventTracking()
        Assert.assertTrue(tracker.dataCollection)
        tracker.resumeEventTracking()
        Assert.assertTrue(tracker.dataCollection)
    }

    @Test
    fun testTrackEventMultipleTimes() {
        val event = Timing("category", "variable", 100)
        val id1 = TrackerEvent(event).eventId
        val id2 = TrackerEvent(event).eventId
        Assert.assertNotEquals(id1, id2)
    }

    @Test
    @Throws(JSONException::class, IOException::class, InterruptedException::class)
    fun testTrackSelfDescribingEvent() {
//        Executor.setThreadCount(30);
//        Executor.shutdown();
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val mockWebServer = getMockServer(1)
        var emitter: Emitter? = null
        val builder =
            Consumer { emitterArg: Emitter -> emitterArg.bufferOption = BufferOption.Single }
        try {
            emitter = Emitter(context, getMockServerURI(mockWebServer)!!, builder)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Exception on Emitter creation")
        }
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.screenContext = false
            tracker.geoLocationContext = false
            tracker.installAutotracking = false
            tracker.screenViewAutotracking = false
        }
        Companion.tracker =
            Tracker(emitter!!, namespace, "testTrackWithNoContext", context, trackerBuilder)
        val eventStore = emitter.eventStore
        if (eventStore != null) {
            val isClean = eventStore.removeAllEvents()
            Log.i("testTrackSelfDescribingEvent", "EventStore clean: $isClean")
        }
        Log.i("testTrackSelfDescribingEvent", "Send SelfDescribing event")
        val sdj = SelfDescribingJson("iglu:foo/bar/jsonschema/1-0-0")
        val sdEvent = SelfDescribing(sdj)
        val eventId = Companion.tracker!!.track(sdEvent)
        val req = mockWebServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertNotNull(req)
        val reqCount = mockWebServer.requestCount
        Assert.assertEquals(1, reqCount.toLong())
        val payload = JSONObject(req!!.body.readUtf8())
        Assert.assertEquals(2, payload.length().toLong())
        Assert.assertEquals(
            "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
            payload.getString("schema")
        )
        val data = payload.getJSONArray("data")
        Assert.assertEquals(1, data.length().toLong())
        val event = data.getJSONObject(0)
        Assert.assertEquals("ue", event.getString(Parameters.EVENT))
        Assert.assertFalse(event.has(Parameters.UNSTRUCTURED_ENCODED))
        Assert.assertEquals(eventId.toString(), event.getString(Parameters.EID))
        mockWebServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testTrackWithNoContext() {
        threadCount = 30
        shutdown()
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val mockWebServer = getMockServer(1)
        var emitter: Emitter? = null
        val builder =
            Consumer { emitterArg: Emitter -> emitterArg.bufferOption = BufferOption.Single }
        try {
            emitter = Emitter(context, getMockServerURI(mockWebServer)!!, builder)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Exception on Emitter creation")
        }
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.screenContext = false
            tracker.geoLocationContext = false
            tracker.installAutotracking = false
            tracker.screenViewAutotracking = false
        }
        Companion.tracker =
            Tracker(emitter!!, namespace, "testTrackWithNoContext", context, trackerBuilder)
        Log.i("testTrackWithNoContext", "Send ScreenView event")
        Companion.tracker!!.track(ScreenView("name"))
        val req = mockWebServer.takeRequest(60, TimeUnit.SECONDS)
        Assert.assertNotNull(req)
        val reqCount = mockWebServer.requestCount
        Assert.assertEquals(1, reqCount.toLong())
        val payload = JSONObject(req!!.body.readUtf8())
        Assert.assertEquals(2, payload.length().toLong())
        Assert.assertEquals(
            "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4",
            payload.getString("schema")
        )
        val data = payload.getJSONArray("data")
        Assert.assertEquals(1, data.length().toLong())
        val event = data.getJSONObject(0)
        var found = true
        try {
            val co = event.getString(Parameters.CONTEXT)
            Log.e("testTrackWithNoContext", "Unexpected event: $event")
            Assert.fail(co)
        } catch (e: Exception) {
            found = false
        }
        Assert.assertFalse(found)
        mockWebServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testTrackWithoutDataCollection() {
        threadCount = 30
        shutdown()
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val mockWebServer = getMockServer(1)
        val builder = Consumer { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        val emitter = Emitter(
            context, getMockServerURI(mockWebServer)!!, builder
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.geoLocationContext = false
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = false
            tracker.screenViewAutotracking = false
        }
        Companion.tracker = Tracker(emitter, namespace, "myAppId", context, trackerBuilder)
        Companion.tracker!!.pauseEventTracking()
        val eventId = Companion.tracker!!.track(ScreenView("name"))
        Assert.assertNull(eventId)
        val req = mockWebServer.takeRequest(2, TimeUnit.SECONDS)
        Assert.assertEquals(0, Companion.tracker!!.emitter.eventStore!!.size())
        Assert.assertNull(req)
        mockWebServer.shutdown()
    }

    @Test
    @Throws(Exception::class)
    fun testTrackWithSession() {
        threadCount = 30
        shutdown()
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val mockWebServer = getMockServer(1)
        val builder = Consumer { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        val emitter = Emitter(
            context, getMockServerURI(mockWebServer)!!, builder
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = true
            tracker.platformContextEnabled = false
            tracker.geoLocationContext = false
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = false
            tracker.screenViewAutotracking = false
            tracker.foregroundTimeout = 5
            tracker.backgroundTimeout = 5
        }
        Companion.tracker = Tracker(emitter, namespace, "myAppId", context, trackerBuilder)
        Assert.assertNotNull(Companion.tracker!!.session)
        Companion.tracker!!.resumeSessionChecking()
        Thread.sleep(2000)
        Companion.tracker!!.pauseSessionChecking()
        mockWebServer.shutdown()
    }

    @Test
    fun testTrackScreenView() {
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val builder = Consumer { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        val emitter = Emitter(
            context, "fake-uri", builder
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.screenContext = true
            tracker.sessionContext = false
            tracker.platformContextEnabled = false
            tracker.geoLocationContext = false
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = false
            tracker.screenViewAutotracking = false
            tracker.foregroundTimeout = 5
            tracker.backgroundTimeout = 5
        }
        Companion.tracker = Tracker(emitter, namespace, "myAppId", context, trackerBuilder)
        val screenState = Companion.tracker!!.getScreenState()
        Assert.assertNotNull(screenState)
        var screenStateMapWrapper: Map<String, Any?> = screenState!!.getCurrentScreen(true).map
        var screenStateMap = screenStateMapWrapper[Parameters.DATA] as Map<String?, Any?>?
        Assert.assertEquals("Unknown", screenStateMap!![Parameters.SCREEN_NAME])

        // Send screenView
        var screenView = ScreenView("screen1")
        val screenId = screenView.dataPayload["id"] as String?
        val eventId1 = Companion.tracker!!.track(screenView)
        screenStateMapWrapper = Companion.tracker!!.getScreenState()!!.getCurrentScreen(true).map
        screenStateMap = screenStateMapWrapper[Parameters.DATA] as Map<String?, Any?>?
        Assert.assertEquals("screen1", screenStateMap!![Parameters.SCREEN_NAME])
        Assert.assertEquals(screenId, screenStateMap[Parameters.SCREEN_ID])

        // Send another screenView
        screenView = ScreenView("screen2")
        val screenId1 = screenView.dataPayload["id"] as String?
        val eventId2 = Companion.tracker!!.track(screenView)
        Assert.assertNotEquals(eventId1.toString(), eventId2.toString())
    }

    @Test
    fun testTrackUncaughtException() {
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        Thread.setDefaultUncaughtExceptionHandler(
            TestExceptionHandler("Illegal State Exception has been thrown!")
        )
        Assert.assertEquals(
            TestExceptionHandler::class.java,
            Thread.getDefaultUncaughtExceptionHandler().javaClass
        )
        val emitter = Emitter(
            context, "com.acme", null
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = true
            tracker.screenViewAutotracking = false
        }
        Companion.tracker = Tracker(emitter, namespace, "myAppId", context, trackerBuilder)
        Assert.assertTrue(Companion.tracker!!.exceptionAutotracking)
        Assert.assertEquals(
            ExceptionHandler::class.java,
            Thread.getDefaultUncaughtExceptionHandler().javaClass
        )
    }

    @Test
    fun testExceptionHandler() {
        val namespace = "myNamespace"
        TestUtils.createSessionSharedPreferences(context, namespace)
        val handler = TestExceptionHandler("Illegal State Exception has been thrown!")
        Thread.setDefaultUncaughtExceptionHandler(handler)
        Assert.assertEquals(
            TestExceptionHandler::class.java,
            Thread.getDefaultUncaughtExceptionHandler().javaClass
        )
        val emitter = Emitter(
            context, "com.acme", null
        )
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = false
            tracker.screenViewAutotracking = false
        }
        Companion.tracker = Tracker(emitter, namespace, "myAppId", context, trackerBuilder)
        val handler1 = ExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(handler1)
        Assert.assertEquals(
            ExceptionHandler::class.java,
            Thread.getDefaultUncaughtExceptionHandler().javaClass
        )
        handler1.uncaughtException(
            Thread.currentThread(),
            Throwable("Illegal State Exception has been thrown!")
        )
    }

    @Test
    fun testStartsNewSessionWhenChangingAnonymousTracking() {
        val builder = Consumer { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        val emitter = Emitter(
            context, "fake-uri", builder
        )
        emitter.pauseEmit()
        val trackerBuilder = Consumer { tracker: Tracker ->
            tracker.base64Encoded = false
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = true
            tracker.installAutotracking = false
            tracker.exceptionAutotracking = false
            tracker.screenViewAutotracking = false
            tracker.foregroundTimeout = 5
            tracker.backgroundTimeout = 5
        }
        Companion.tracker = Tracker(emitter, "ns", "myAppId", context, trackerBuilder)
        Companion.tracker!!.track(Structured("c", "a"))
        val sessionIdStart = Companion.tracker!!.session!!.state!!.sessionId
        Companion.tracker!!.userAnonymisation = true
        Companion.tracker!!.track(Structured("c", "a"))
        val sessionIdAnonymous = Companion.tracker!!.session!!.state!!.sessionId
        Assert.assertNotEquals(sessionIdStart, sessionIdAnonymous)
        Companion.tracker!!.userAnonymisation = false
        Companion.tracker!!.track(Structured("c", "a"))
        val sessionIdNotAnonymous = Companion.tracker!!.session!!.state!!.sessionId
        Assert.assertNotEquals(sessionIdAnonymous, sessionIdNotAnonymous)
    }

    class TestExceptionHandler(private val expectedMessage: String) :
        Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            Assert.assertEquals(expectedMessage, e.message)
        }
    }

    // Mock Server
    @Throws(IOException::class)
    fun getMockServer(count: Int): MockWebServer {
        val mockServer = MockWebServer()
        mockServer.start()
        val mockResponse = MockResponse().setResponseCode(200)
        for (i in 0 until count) {
            mockServer.enqueue(mockResponse)
        }
        return mockServer
    }

    @SuppressLint("DefaultLocale")
    fun getMockServerURI(mockServer: MockWebServer?): String? {
        return if (mockServer != null) {
            String.format("%s:%d", mockServer.hostName, mockServer.port)
        } else null
    }

    companion object {
        private var tracker: Tracker? = null
    }
}
