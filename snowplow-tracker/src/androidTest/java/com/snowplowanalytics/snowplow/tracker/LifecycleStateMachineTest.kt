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
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.screenviews.ScreenSummaryState
import com.snowplowanalytics.core.session.AppStateProvider
import com.snowplowanalytics.core.utils.NotificationCenter.postNotification
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.entity.LifecycleEntity
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.EventSink
import com.snowplowanalytics.snowplow.util.TimeTraveler
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Regression tests for AISP-1708: the `application_lifecycle` entity's `isVisible` (and the
 * knock-on session/screen_summary state) must reflect the actual app state instead of assuming
 * foreground, which was wrong for a background-only process launch (e.g. WorkManager, FCM).
 */
@RunWith(AndroidJUnit4::class)
class LifecycleStateMachineTest {

    var timeTraveler = TimeTraveler()

    @After
    fun tearDown() {
        removeAllTrackers()
        Executor.shutdown()
        // Restore the default (foreground) app state so other tests aren't affected by this one.
        forceAppState(isForeground = true)
    }

    // --- TESTS

    @Test
    fun tracksIsVisibleFalseForBackgroundOnlyProcessLaunch() {
        forceAppState(isForeground = false)

        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))
        tracker.track(Timing(category = "c", variable = "v", timing = 1))
        Thread.sleep(200)

        val event = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        val lifecycle = getLifecycleEntityData(event)
        Assert.assertEquals(false, lifecycle?.get(LifecycleEntity.PARAM_LIFECYCLEENTITY_ISVISIBLE))
    }

    @Test
    fun tracksIsVisibleTrueForNormalForegroundLaunch() {
        forceAppState(isForeground = true)

        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))
        tracker.track(Timing(category = "c", variable = "v", timing = 1))
        Thread.sleep(200)

        val event = eventSink.trackedEvents.find { it.schema == TrackerConstants.SCHEMA_USER_TIMINGS }
        val lifecycle = getLifecycleEntityData(event)
        Assert.assertEquals(true, lifecycle?.get(LifecycleEntity.PARAM_LIFECYCLEENTITY_ISVISIBLE))
    }

    @Test
    fun tracksApplicationForegroundWhenBackgroundLaunchedAppComesToForeground() {
        forceAppState(isForeground = false)

        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))
        // The session must be seeded as backgrounded, matching the background-only launch.
        Assert.assertEquals(true, tracker.session?.isInBackground)
        Assert.assertEquals(0, tracker.session?.foregroundIndex)

        val notificationData: MutableMap<String, Any> = HashMap()
        notificationData["isForeground"] = true
        postNotification("SnowplowLifecycleTracking", notificationData)
        Thread.sleep(200)

        // Without the fix, the dedup guard in Tracker.receiveLifecycleNotification wrongly
        // believed the session was already foregrounded and swallowed this transition.
        Assert.assertEquals(false, tracker.session?.isInBackground)
        Assert.assertEquals(1, tracker.session?.foregroundIndex)

        val foregroundEvent = eventSink.trackedEvents.find { it.schema == Foreground.schema }
        Assert.assertNotNull(
            "A real application_foreground event should be tracked when a background-launched app is opened",
            foregroundEvent
        )
    }

    @Test
    fun tracksScreenSummaryBackgroundSecondsAfterBackgroundLaunchIsOpened() {
        ScreenSummaryState.dateGenerator = { timeTraveler.generateTimestamp() }
        forceAppState(isForeground = false)

        val eventSink = EventSink()
        val tracker = createTracker(listOf(eventSink))
        tracker.track(ScreenView(name = "Screen 1"))
        Thread.sleep(200)

        timeTraveler.travelBy(7.toDuration(DurationUnit.SECONDS))
        val notificationData: MutableMap<String, Any> = HashMap()
        notificationData["isForeground"] = true
        postNotification("SnowplowLifecycleTracking", notificationData)
        Thread.sleep(200)

        val foregroundEvent = eventSink.trackedEvents.find { it.schema == Foreground.schema }
        Assert.assertNotNull(foregroundEvent)
        val summary = getScreenSummary(foregroundEvent)
        Assert.assertEquals(0.0, summary?.get("foreground_sec"))
        Assert.assertEquals(7.0, summary?.get("background_sec"))
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun forceAppState(isForeground: Boolean) {
        val owner = ProcessLifecycleOwner.get()
        if (isForeground) {
            AppStateProvider.onStart(owner)
        } else {
            AppStateProvider.onStop(owner)
        }
    }

    private fun getLifecycleEntityData(event: InspectableEvent?): Map<*, *>? {
        val entity = event?.entities?.find { it.map["schema"] == LifecycleEntity.SCHEMA_LIFECYCLEENTITY }
        return entity?.map?.get("data") as? Map<*, *>
    }

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
