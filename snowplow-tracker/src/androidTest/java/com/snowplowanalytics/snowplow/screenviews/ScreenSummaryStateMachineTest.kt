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
import com.snowplowanalytics.core.screenviews.ScreenSummaryState
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.EventSink
import com.snowplowanalytics.snowplow.util.TimeTraveler
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
class ScreenSummaryStateMachineTest {

    var timeTraveler = TimeTraveler()

    @Before
    fun setUp() {
        ScreenSummaryState.dateGenerator = { timeTraveler.generateTimestamp() }
    }

    @After
    fun tearDown() {
        removeAllTrackers()
        Executor.shutdown()
    }

    // --- TESTS

    @Test
    fun tracksTransitionToBackgroundAndForeground() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(ScreenView(name = "Screen 1"))
        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        tracker.track(Background())
        Thread.sleep(200)

        timeTraveler.travelBy(5.toDuration(DurationUnit.SECONDS))
        tracker.track(Foreground())
        Thread.sleep(200)

        val events = eventSink.trackedEvents
        Assert.assertEquals(3, events.size)

        val backgroundSummary = getScreenSummary(events.find { it.schema == Background.schema })
        Assert.assertEquals(10.0, backgroundSummary?.get("foreground_sec"))
        Assert.assertEquals(0.0, backgroundSummary?.get("background_sec"))

        val foregroundSummary = getScreenSummary(events.find { it.schema == Foreground.schema })
        Assert.assertEquals(10.0, foregroundSummary?.get("foreground_sec"))
        Assert.assertEquals(5.0, foregroundSummary?.get("background_sec"))
    }

    @Test
    fun tracksScreenEndEventWithScreenSummary() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(ScreenView(name = "Screen 1"))
        Thread.sleep(200)
        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        tracker.track(ScreenView(name = "Screen 2"))
        Thread.sleep(200)

        val events = eventSink.trackedEvents
        Assert.assertEquals(3, events.size)

        val screenEnd = events.find { it.schema == TrackerConstants.SCHEMA_SCREEN_END }
        val screenSummary = getScreenSummary(screenEnd)
        Assert.assertEquals(10.0, screenSummary?.get("foreground_sec"))
        Assert.assertEquals(0.0, screenSummary?.get("background_sec"))

        // should have the screen name of the first screen view
        val screenEndScreen = screenEnd?.entities?.find { it.map["schema"] == TrackerConstants.SCHEMA_SCREEN }
        Assert.assertEquals("Screen 1", (screenEndScreen?.map?.get("data") as? Map<*, *>)?.get("name"))
    }

    @Test
    fun updatesListMetrics() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(ScreenView(name = "Screen 1"))
        Thread.sleep(200)
        tracker.track(ListItemView(index = 1, itemsCount = 10))
        Thread.sleep(200)
        tracker.track(ListItemView(index = 3, itemsCount = 10))
        Thread.sleep(200)
        tracker.track(ListItemView(index = 2, itemsCount = 10))
        Thread.sleep(200)
        tracker.track(ScreenView(name = "Screen 2"))
        Thread.sleep(200)

        val events = eventSink.trackedEvents
        Assert.assertEquals(3, events.size)

        val screenSummary = getScreenSummary(events.find { it.schema == TrackerConstants.SCHEMA_SCREEN_END })
        Assert.assertEquals(3, screenSummary?.get("last_item_index"))
        Assert.assertEquals(10, screenSummary?.get("items_count"))
    }

    @Test
    fun updatesScrollMetrics() {
        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))

        tracker.track(ScreenView(name = "Screen 1"))
        Thread.sleep(200)
        tracker.track(ScrollChanged(yOffset = 10, viewHeight = 20, contentHeight = 100))
        Thread.sleep(200)
        tracker.track(ScrollChanged(xOffset = 15, yOffset = 30, viewWidth = 15, viewHeight = 20, contentWidth = 150, contentHeight = 100))
        Thread.sleep(200)
        tracker.track(ScrollChanged(yOffset = 20, viewHeight = 20, contentHeight = 100))
        Thread.sleep(200)
        tracker.track(ScreenView(name = "Screen 2"))
        Thread.sleep(200)

        val events = eventSink.trackedEvents
        Assert.assertEquals(3, events.size)

        val screenSummary = getScreenSummary(events.find { it.schema == TrackerConstants.SCHEMA_SCREEN_END })
        Assert.assertEquals(10, screenSummary?.get("min_y_offset"))
        Assert.assertEquals(15, screenSummary?.get("min_x_offset"))
        Assert.assertEquals(50, screenSummary?.get("max_y_offset"))
        Assert.assertEquals(30, screenSummary?.get("max_x_offset"))
        Assert.assertEquals(150, screenSummary?.get("content_width"))
        Assert.assertEquals(100, screenSummary?.get("content_height"))
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun getScreenSummary(event: InspectableEvent?): Map<String, Any?>? {
        val entity = event?.entities?.find { it.map["schema"] == TrackerConstants.SCHEMA_SCREEN_SUMMARY }
        return entity?.map?.get("data") as? Map<String, Any?>
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
