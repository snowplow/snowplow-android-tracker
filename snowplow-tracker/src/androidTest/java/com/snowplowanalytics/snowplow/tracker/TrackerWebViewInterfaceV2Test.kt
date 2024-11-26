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
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.media.MediaSchemata.eventSchema
import com.snowplowanalytics.core.media.MediaSchemata.playerSchema
import com.snowplowanalytics.core.media.MediaSchemata.sessionSchema
import com.snowplowanalytics.core.media.controller.MediaPingInterval
import com.snowplowanalytics.core.media.controller.MediaSessionTracking
import com.snowplowanalytics.core.media.controller.MediaTrackingImpl
import com.snowplowanalytics.core.media.controller.TimerInterface
import com.snowplowanalytics.core.tracker.TrackerWebViewInterface
import com.snowplowanalytics.core.tracker.TrackerWebViewInterfaceV2
import com.snowplowanalytics.core.utils.Util.objectMapToString
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.media.configuration.MediaTrackingConfiguration
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import com.snowplowanalytics.snowplow.util.TimeTraveler
import junit.framework.TestCase
import org.json.JSONException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
class TrackerWebViewInterfaceV2Test {
    private var webInterface: TrackerWebViewInterfaceV2? = null
    private val trackedEvents: MutableList<InspectableEvent> = mutableListOf()
    private val firstEvent: InspectableEvent
        get() = trackedEvents.first()
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
        trackedEvents.clear()
        Executor.shutdown()
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksPagePingEvent() {
        webInterface!!.trackWebViewEvent(
            eventName = "pp",
            trackerVersion = "webview",
            useragent = "Firefox",
            pageUrl = "http://snowplow.com",
            pageTitle = "Snowplow",
            referrer = "http://google.com",
            pingXOffsetMin = 10,
            pingXOffsetMax = 20,
            pingYOffsetMin = 30,
            pingYOffsetMax = 40
        )

        Thread.sleep(200)

        assertEquals(1, trackedEvents.size)
        val payload = firstEvent.payload
        assertEquals("pp", payload[Parameters.EVENT])
        assertEquals("webview", payload[Parameters.TRACKER_VERSION])
        assertEquals("Firefox", payload[Parameters.USERAGENT])
        assertEquals("http://snowplow.com", payload[Parameters.PAGE_URL])
        assertEquals("Snowplow", payload[Parameters.PAGE_TITLE])
        assertEquals("http://google.com", payload[Parameters.PAGE_REFR])
        assertEquals(10, payload[Parameters.PING_XOFFSET_MIN])
        assertEquals(20, payload[Parameters.PING_XOFFSET_MAX])
        assertEquals(30, payload[Parameters.PING_YOFFSET_MIN])
        assertEquals(40, payload[Parameters.PING_YOFFSET_MAX])
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksStructuredEvent() {
        webInterface!!.trackWebViewEvent(
            eventName = "se",
            trackerVersion = "webview2",
            useragent = "Firefox",
            category = "cat",
            action = "act",
            property = "prop",
            label = "lbl",
            value = 10.0
        )

        Thread.sleep(200)

        assertEquals(1, trackedEvents.size)
        val payload = firstEvent.payload
        assertEquals("se", payload[Parameters.EVENT])
        assertEquals("webview2", payload[Parameters.TRACKER_VERSION])
        assertEquals("Firefox", payload[Parameters.USERAGENT])
        assertEquals("cat", payload[Parameters.SE_CATEGORY])
        assertEquals("act", payload[Parameters.SE_ACTION])
        assertEquals("prop", payload[Parameters.SE_PROPERTY])
        assertEquals("lbl", payload[Parameters.SE_LABEL])
        assertEquals(10.0, payload[Parameters.SE_VALUE])
    }

    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun tracksEventWithCorrectTracker() {
        // create the second tracker
        val trackedEvents2: MutableList<InspectableEvent> = mutableListOf()
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        val plugin2 = PluginConfiguration("plugin2")
        plugin2.afterTrack {
                trackedEvents2.add(it)
        }
        createTracker(
            context,
            namespace = "ns2",
            network = networkConfig,
            TrackerConfiguration("appId"),
            plugin2
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

        assertEquals(0, trackedEvents.size)
        assertEquals(1, trackedEvents2.size)

        // track an event using default tracker if not specified
        webInterface!!.trackWebViewEvent(
            eventName = "pp",
            trackerVersion = "webview",
            useragent = "Chrome",
            pageUrl = "http://snowplow.com",
        )
        Thread.sleep(200)

        assertEquals(1, trackedEvents.size)
        assertEquals(1, trackedEvents2.size)
    }


    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(): TrackerController {
        val namespace = "ns" + Math.random().toString()
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        val trackerConfig = TrackerConfiguration("appId")
            .installAutotracking(false)
            .lifecycleAutotracking(false)

        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack {
            if (namespace == this.tracker?.namespace) {
                trackedEvents.add(it)
            }
        }

        return createTracker(
            context,
            namespace = namespace,
            network = networkConfig,
            trackerConfig,
            plugin
        )
    }
}
