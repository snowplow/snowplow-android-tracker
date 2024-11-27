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
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod
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
    private var networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
    private var tracker: TrackerController? = null

    @Before
    fun setUp() {
        webInterface = TrackerWebViewInterfaceV2()
        tracker = createTracker()
    }

    @After
    fun tearDown() {
        tracker?.pause()
        tracker = null
        removeAllTrackers()
        Executor.shutdown()
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithAllOptions() {
        val data = "{\"schema\":\"iglu:etc\",\"data\":{\"key\":\"val\"}}"
        
        webInterface!!.trackWebViewEvent(
            eventName = "ue",
            trackerVersion = "webview",
            useragent = "Chrome",
            selfDescribingEventData = data,
            pageUrl = "http://snowplow.com",
            pageTitle = "Snowplow",
            referrer = "http://google.com",
            pingXOffsetMin = 10,
            pingXOffsetMax = 20,
            pingYOffsetMin = 30,
            pingYOffsetMax = 40,
            category = "cat",
            action = "act",
            property = "prop",
            label = "lbl",
            value = 10.0
        )

        Thread.sleep(200)
        waitForEvents(networkConnection, 1)

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
        // create the second tracker
        val networkConnection2 = MockNetworkConnection(HttpMethod.GET, 200)
        createTracker(
            context,
            namespace = "ns2",
            NetworkConfiguration(networkConnection2),
            TrackerConfiguration("appId")
        )

        // track an event using the second tracker
        webInterface!!.trackWebViewEvent(
            eventName = "pv",
            trackerVersion = "webview",
            useragent = "Chrome",
            pageUrl = "http://snowplow.com",
            trackers = arrayOf("ns2")
        )
        Thread.sleep(200)
        waitForEvents(networkConnection2, 1)

        assertEquals(0, networkConnection.countRequests())
        assertEquals(1, networkConnection2.countRequests())

        assertEquals("pv", networkConnection2.allRequests[0].payload.map[Parameters.EVENT])

        // tracks using default tracker if not specified
        webInterface!!.trackWebViewEvent(
            eventName = "pp",
            trackerVersion = "webview",
            useragent = "Chrome",
            pageUrl = "http://snowplow.com",
        )
        Thread.sleep(200)
        waitForEvents(networkConnection, 1)

        assertEquals(1, networkConnection.countRequests())
        assertEquals(1, networkConnection2.countRequests())
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithEntity() {
        val entities = "[{\"schema\":\"iglu:com.example/etc\",\"data\":{\"key\":\"val\"}}]"
        webInterface!!.trackWebViewEvent(
            eventName = "pp",
            trackerVersion = "webview",
            useragent = "Chrome",
            pageUrl = "http://snowplow.com",
            entities = entities
        )
        Thread.sleep(200)
        waitForEvents(networkConnection, 1)

        assertEquals(1, networkConnection.countRequests())
        
        val relevantEntities = ArrayList<JSONObject>()
        val allEntities = JSONObject(networkConnection.allRequests[0].payload.map[Parameters.CONTEXT] as String)
            .getJSONArray("data")
        for (i in 0 until allEntities.length()) {
            if (allEntities.getJSONObject(i).getString("schema") == "iglu:com.example/etc") {
                relevantEntities.add(allEntities.getJSONObject(i).getJSONObject("data"))
            }
        }
        assertEquals(1, relevantEntities.size)
        assertEquals("val", relevantEntities[0].get("key") as? String)
    }
    
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun addsEventNameAndSchemaForInspection() {
        val trackedEvents: MutableList<InspectableEvent> = mutableListOf()
        
        val namespace = "ns" + Math.random().toString()
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))

        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack { trackedEvents.add(it) }

        createTracker(
            context,
            namespace,
            networkConfig,
            TrackerConfiguration("appId"),
            plugin
        )

        val data = "{\"schema\":\"iglu:etc\",\"data\":{\"key\":\"val\"}}"
        webInterface!!.trackWebViewEvent(
            eventName = "se",
            trackerVersion = "webview",
            useragent = "Chrome",
            selfDescribingEventData = data,
            trackers = arrayOf(namespace)
        )

        Thread.sleep(200)
        assertEquals(1, trackedEvents.size)
        assertEquals("se", trackedEvents[0].name)
        assertEquals("iglu:etc", trackedEvents[0].schema)
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(): TrackerController {
        val trackerConfig = TrackerConfiguration("appId")
            .installAutotracking(false)
            .lifecycleAutotracking(false)
            .platformContext(false)
            .base64encoding(false)

        return createTracker(
            context,
            "ns${Math.random()}",
            NetworkConfiguration(networkConnection),
            trackerConfig
        )
    }

    @Throws(Exception::class)
    fun waitForEvents(networkConnection: MockNetworkConnection, eventsExpected: Int) {
        var i = 0
        while (i < 10 && networkConnection.countRequests() == eventsExpected - 1) {
            Thread.sleep(1000)
            i++
        }
    }
}
