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
package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.emitter.Executor.shutdown
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.MockEventStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeepLinkReceivedTest {
    
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val es = shutdown()
        es?.awaitTermination(60, TimeUnit.SECONDS)
    }

    @Test
    fun testExpectedForm() {
        val event = DeepLinkReceived("url")
            .referrer("referrer")
        val payload = event.dataPayload
        Assert.assertNotNull(payload)
        Assert.assertEquals("url", payload[DeepLinkReceived.PARAM_URL])
        Assert.assertEquals("referrer", payload[DeepLinkReceived.PARAM_REFERRER])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testWorkaroundForCampaignAttributionEnrichment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Prepare DeepLinkReceived event
        val event = DeepLinkReceived("url")
            .referrer("referrer")

        // Setup tracker
        val trackerConfiguration = TrackerConfiguration("appId")
            .base64encoding(false)
            .installAutotracking(false)
        val eventStore = MockEventStore()
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

        // Track event
        trackerController.track(event)
        var i = 0
        while (eventStore.size() < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        val events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        val payload = events[0]!!.payload

        // Check url and referrer fields for atomic table
        val url = payload.map[Parameters.PAGE_URL] as String?
        val referrer = payload.map[Parameters.PAGE_REFR] as String?
        Assert.assertEquals("url", url)
        Assert.assertEquals("referrer", referrer)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testDeepLinkContextAndAtomicPropertiesAddedToScreenView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Prepare DeepLinkReceived event
        val event = DeepLinkReceived("the_url")
            .referrer("the_referrer")

        // Prepare Screen View event
        val screenView = ScreenView("SV")

        // Setup tracker
        val trackerConfiguration = TrackerConfiguration("appId")
            .base64encoding(false)
            .installAutotracking(false)
        val eventStore = MockEventStore()
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

        // Track events
        trackerController.track(event)
        val screenViewEventId = trackerController.track(screenView)
        var i = 0
        while (eventStore.size() < 2 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        val events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(2, events.size.toLong())
        var screenViewPayload: Map<*, *>? = null
        for (emitterEvent in events) {
            if (emitterEvent!!.payload.map["eid"] == screenViewEventId.toString()) {
                screenViewPayload = emitterEvent.payload.map
                break
            }
        }
        Assert.assertNotNull(screenViewPayload)

        // Check the DeepLink context entity properties
        val screenViewContext = screenViewPayload!![Parameters.CONTEXT] as String?
        Assert.assertNotNull(screenViewContext)
        Assert.assertTrue(screenViewContext!!.contains("\"referrer\":\"the_referrer\""))
        Assert.assertTrue(screenViewContext.contains("\"url\":\"the_url\""))

        // Check url and referrer fields for atomic table
        val url = screenViewPayload[Parameters.PAGE_URL] as String?
        val referrer = screenViewPayload[Parameters.PAGE_REFR] as String?
        Assert.assertEquals("the_url", url)
        Assert.assertEquals("the_referrer", referrer)
    }
}
