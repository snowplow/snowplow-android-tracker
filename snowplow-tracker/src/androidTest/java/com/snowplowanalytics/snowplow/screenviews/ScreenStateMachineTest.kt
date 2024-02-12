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
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.EventSink
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ScreenStateMachineTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        removeAllTrackers()
    }

    // --- TESTS

    @Test
    fun tracksEventsWithTheCorrectScreenEntityInfo() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(Timing(category = "c1", variable = "v1", timing = 1))
        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(Timing(category = "c2", variable = "v2", timing = 2))
        tracker.track(ScreenView(name = "Screen 2"))
        tracker.track(Timing(category = "c3", variable = "v3", timing = 3))
        tracker.track(ScreenView(name = "Screen 3"))
        tracker.track(Timing(category = "c4", variable = "v4", timing = 4))

        Thread.sleep(200)

        val events = eventSink.trackedEvents

        val timingEvents = events.filter { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
            .sortedBy { it.payload["timing"] as Int }
        Assert.assertEquals(4, timingEvents.size)

        val screen0 = getScreenEntityData(timingEvents[0])
        Assert.assertNull(screen0)

        val screen1 = getScreenEntityData(timingEvents[1])
        Assert.assertEquals("Screen 1", screen1?.get("name"))

        val screen2 = getScreenEntityData(timingEvents[2])
        Assert.assertEquals("Screen 2", screen2?.get("name"))

        val screen3 = getScreenEntityData(timingEvents[3])
        Assert.assertEquals("Screen 3", screen3?.get("name"))
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getScreenEntityData(event: InspectableEvent): Map<*, *>? {
        val entity = event.entities.find { it.map["schema"] == TrackerConstants.SCHEMA_SCREEN }
        return entity?.map?.get("data") as? Map<*, *>
    }

    private fun createTracker(configurations: List<Configuration>): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return Snowplow.createTracker(
            context,
            namespace = "ns" + Math.random().toString(),
            network = networkConfig,
            configurations = configurations.toTypedArray()
        )
    }
}
