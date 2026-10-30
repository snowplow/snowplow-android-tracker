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
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
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
        Executor.shutdown()
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

    @Test
    fun keepsTheScreenEntityAttachedByTheCaller() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink, TrackerConfiguration("appId").screenContextFromEvent(true)))

        val callerScreenId = UUID.randomUUID().toString()
        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(ScreenView(name = "Screen 2"))
        tracker.track(
            Timing(category = "c1", variable = "v1", timing = 1)
                .entities(listOf(callerScreenEntity("Screen 1", callerScreenId)))
        )

        Thread.sleep(200)

        val timing = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        Assert.assertNotNull(timing)
        val screens = getAllScreenEntityData(timing)
        Assert.assertEquals(1, screens.size)
        Assert.assertEquals("Screen 1", screens[0]["name"])
        Assert.assertEquals(callerScreenId, screens[0]["id"])
    }

    @Test
    fun doesNotDuplicateTheScreenEntityAttachedByTheCaller() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink, TrackerConfiguration("appId").screenContextFromEvent(true)))

        tracker.track(
            Structured(category = "before", action = "a")
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )
        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(
            Structured(category = "during", action = "a")
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )
        tracker.track(
            Timing(category = "c1", variable = "v1", timing = 1)
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )

        Thread.sleep(200)

        val eventsWithCallerScreen = eventSink.trackedEvents.filter {
            it.schema == TrackerConstants.SCHEMA_USER_TIMINGS || it.name == TrackerConstants.EVENT_STRUCTURED
        }
        Assert.assertEquals(3, eventsWithCallerScreen.size)
        for (event in eventsWithCallerScreen) {
            val screens = getAllScreenEntityData(event)
            Assert.assertEquals(1, screens.size)
            Assert.assertEquals("Screen A", screens[0]["name"])
        }
    }

    @Test
    fun attachesTheCurrentScreenWhenTheCallerDoesNotAttachOne() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink, TrackerConfiguration("appId").screenContextFromEvent(true)))

        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(ScreenView(name = "Screen 2"))
        tracker.track(Timing(category = "c1", variable = "v1", timing = 1))

        Thread.sleep(200)

        val timing = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        Assert.assertNotNull(timing)
        val screens = getAllScreenEntityData(timing)
        Assert.assertEquals(1, screens.size)
        Assert.assertEquals("Screen 2", screens[0]["name"])
    }

    @Test
    fun screenEndKeepsTheEndingScreenAfterEventsWithCallerScreenEntities() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink, TrackerConfiguration("appId").screenContextFromEvent(true)))

        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(
            Timing(category = "c1", variable = "v1", timing = 1)
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )
        tracker.track(ScreenView(name = "Screen 2"))
        tracker.track(ScreenEnd())

        Thread.sleep(200)

        val screenEnds = eventSink.trackedEvents.filter { it.schema == TrackerConstants.SCHEMA_SCREEN_END }
            .map { getAllScreenEntityData(it) }
        Assert.assertEquals(2, screenEnds.size)
        for (screens in screenEnds) {
            Assert.assertEquals(1, screens.size)
        }
        Assert.assertEquals(setOf("Screen 1", "Screen 2"), screenEnds.map { it[0]["name"] }.toSet())
    }

    @Test
    fun addsTheCurrentScreenToEventsWithACallerScreenEntityByDefault() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(
            Timing(category = "c1", variable = "v1", timing = 1)
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )

        Thread.sleep(200)

        val timing = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        Assert.assertNotNull(timing)
        val screens = getAllScreenEntityData(timing)
        Assert.assertEquals(setOf("Screen A", "Screen 1"), screens.map { it["name"] }.toSet())
    }

    @Test
    fun keepsTheCallerScreenEntityAfterEnablingScreenContextFromEventAtRuntime() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))
        tracker.screenContextFromEvent = true

        tracker.track(ScreenView(name = "Screen 1"))
        tracker.track(
            Timing(category = "c1", variable = "v1", timing = 1)
                .entities(listOf(callerScreenEntity("Screen A", UUID.randomUUID().toString())))
        )

        Thread.sleep(200)

        Assert.assertTrue(tracker.screenContextFromEvent)
        val timing = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        Assert.assertNotNull(timing)
        val screens = getAllScreenEntityData(timing)
        Assert.assertEquals(1, screens.size)
        Assert.assertEquals("Screen A", screens[0]["name"])
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getScreenEntityData(event: InspectableEvent): Map<*, *>? {
        val entity = event.entities.find { it.map["schema"] == TrackerConstants.SCHEMA_SCREEN }
        return entity?.map?.get("data") as? Map<*, *>
    }

    private fun getAllScreenEntityData(event: InspectableEvent?): List<Map<*, *>> {
        return event?.entities.orEmpty().filter { it.map["schema"] == TrackerConstants.SCHEMA_SCREEN }
            .mapNotNull { it.map["data"] as? Map<*, *> }
    }

    private fun callerScreenEntity(name: String, id: String): SelfDescribingJson {
        return SelfDescribingJson(TrackerConstants.SCHEMA_SCREEN, mapOf("name" to name, "id" to id))
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
