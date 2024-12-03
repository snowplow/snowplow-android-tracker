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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.tracker.TrackerWebViewInterfaceV2
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.EventSink
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerWebViewInterfaceV2Test {
    private var webInterface: TrackerWebViewInterfaceV2? = null

    @Before
    fun setUp() {
        webInterface = TrackerWebViewInterfaceV2()
    }

    @After
    fun tearDown() {
        removeAllTrackers()
        Executor.shutdown()
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithAllOptions() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        createTracker(
            context,
            "ns${Math.random()}",
            NetworkConfiguration(networkConnection),
            TrackerConfiguration("appId").base64encoding(false)
        )
        
        val data = "{\"schema\":\"iglu:etc\",\"data\":{\"key\":\"val\"}}"
        val atomic = "{\"eventName\":\"ue\",\"trackerVersion\":\"webview\"," +
                "\"useragent\":\"Chrome\",\"pageUrl\":\"http://snowplow.com\"," +
                "\"pageTitle\":\"Snowplow\",\"referrer\":\"http://google.com\"," +
                "\"pingXOffsetMin\":10,\"pingXOffsetMax\":20,\"pingYOffsetMin\":30," +
                "\"pingYOffsetMax\":40,\"category\":\"cat\",\"action\":\"act\"," +
                "\"property\":\"prop\",\"label\":\"lbl\",\"value\":10.0}"
        
        webInterface!!.trackWebViewEvent(
            selfDescribingEventData = data,
            atomicProperties = atomic
        )

        var i = 0
        while (i < 10 && networkConnection.countRequests() == 0) {
            Thread.sleep(1000)
            i++
        }

        assertEquals(1, networkConnection.countRequests())

        val request = networkConnection.allRequests[0]
        val payload = request.payload.map

        assertEquals("ue", payload[Parameters.EVENT])
        assertEquals("webview", payload[Parameters.TRACKER_VERSION])
        assertEquals("Chrome", payload[Parameters.USERAGENT])
        assertEquals("http://snowplow.com", payload[Parameters.PAGE_URL])
        assertEquals("Snowplow", payload[Parameters.PAGE_TITLE])
        assertEquals("http://google.com", payload[Parameters.PAGE_REFR])
        assertEquals("10", payload[Parameters.PING_XOFFSET_MIN])
        assertEquals("20", payload[Parameters.PING_XOFFSET_MAX])
        assertEquals("30", payload[Parameters.PING_YOFFSET_MIN])
        assertEquals("40", payload[Parameters.PING_YOFFSET_MAX])
        assertEquals("cat", payload[Parameters.SE_CATEGORY])
        assertEquals("act", payload[Parameters.SE_ACTION])
        assertEquals("prop", payload[Parameters.SE_PROPERTY])
        assertEquals("lbl", payload[Parameters.SE_LABEL])
        assertEquals("10.0", payload[Parameters.SE_VALUE])
        
        assertTrue(payload.containsKey(Parameters.UNSTRUCTURED))
        val selfDescJson = JSONObject(payload[Parameters.UNSTRUCTURED] as String)
        assertEquals(TrackerConstants.SCHEMA_UNSTRUCT_EVENT, selfDescJson.getString("schema"))
        assertEquals(data, selfDescJson.getString("data"))
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithCorrectTracker() {
        val eventSink1 = EventSink()
        val eventSink2 = EventSink()

        createTracker("ns1", eventSink1)
        createTracker("ns2", eventSink2)
        Thread.sleep(200)

        // track an event using the second tracker
        webInterface!!.trackWebViewEvent(
            atomicProperties = "{}",
            trackers = arrayOf("ns2")
        )
        Thread.sleep(200)
        
        assertEquals(0, eventSink1.trackedEvents.size)
        assertEquals(1, eventSink2.trackedEvents.size)

        // tracks using default tracker if not specified
        webInterface!!.trackWebViewEvent(atomicProperties = "{}")
        Thread.sleep(200)

        assertEquals(1, eventSink1.trackedEvents.size)
        assertEquals(1, eventSink2.trackedEvents.size)
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithEntity() {
        val namespace = "ns" + Math.random().toString()
        val eventSink = EventSink()
        createTracker(namespace, eventSink)
        
        webInterface!!.trackWebViewEvent(
            atomicProperties = "{}",
            entities = "[{\"schema\":\"iglu:com.example/etc\",\"data\":{\"key\":\"val\"}}]",
            trackers = arrayOf(namespace)
        )
        Thread.sleep(200)
        val events = eventSink.trackedEvents
        assertEquals(1, events.size)
        
        val relevantEntities = events[0].entities.filter { it.map["schema"] == "iglu:com.example/etc" }
        assertEquals(1, relevantEntities.size)
        
        val entityData = relevantEntities[0].map["data"] as HashMap<*, *>?
        assertEquals("val", entityData?.get("key"))
    }
    
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun addsEventNameAndSchemaForInspection() {
        val namespace = "ns" + Math.random().toString()
        val eventSink = EventSink()
        createTracker(namespace, eventSink)

        webInterface!!.trackWebViewEvent(
            atomicProperties = "{\"eventName\":\"se\"}",
            selfDescribingEventData = "{\"schema\":\"iglu:etc\",\"data\":{\"key\":\"val\"}}",
            trackers = arrayOf(namespace)
        )

        Thread.sleep(200)
        val events = eventSink.trackedEvents
        
        assertEquals(1, events.size)
        assertEquals("se", events[0].name)
        assertEquals("iglu:etc", events[0].schema)
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(namespace: String, eventSink: EventSink): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return createTracker(
            context,
            namespace = namespace,
            network = networkConfig,
            configurations = arrayOf(eventSink)
        )
    }
}
