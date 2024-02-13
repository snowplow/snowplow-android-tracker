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
import com.snowplowanalytics.core.utils.NotificationCenter
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.EventSink
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ScreenViewAutotrackingTest {

    @After
    fun tearDown() {
        removeAllTrackers()
        Executor.shutdown()
    }

    // --- TESTS
    @Test
    fun doesntTrackTheSameScreenViewMultipleTimes() {
        val eventSink = EventSink()
        createTracker(listOf(eventSink))
        Thread.sleep(200)

        NotificationCenter.postNotification("SnowplowScreenView", mapOf(
            "event" to ScreenView(name = "Screen1").activityClassName("Screen1")
        ))
        Thread.sleep(200)

        NotificationCenter.postNotification("SnowplowScreenView", mapOf(
            "event" to ScreenView(name = "Screen1").activityClassName("Screen1")
        ))
        Thread.sleep(200)

        NotificationCenter.postNotification("SnowplowScreenView", mapOf(
            "event" to ScreenView(name = "Screen2").activityClassName("Screen2")
        ))
        Thread.sleep(2000)

        val numberOfScreenViews = eventSink.trackedEvents.filter { it.schema == TrackerConstants.SCHEMA_SCREEN_VIEW }
        Assert.assertEquals(2, numberOfScreenViews.size)
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(configurations: List<Configuration>): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return Snowplow.createTracker(
            context,
            namespace = "testScreenView" + Math.random().toString(),
            network = networkConfig,
            configurations = configurations.toTypedArray()
        )
    }
}
