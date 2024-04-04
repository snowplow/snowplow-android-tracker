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
package com.snowplowanalytics.snowplow.event

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationInstallEventTest {

    @Before
    fun setUp() {
        cleanSharedPreferences()
        Executor.shutdown()
    }

    // Tests
    @Test
    fun testTracksInstallEventOnFirstLaunch() {
        // plugin to check if event was tracked
        var eventTracked = false
        val plugin = PluginConfiguration("testPlugin")
        plugin.afterTrack { eventTracked = true }

        // create tracker with install autotracking
        val trackerConfiguration = TrackerConfiguration("appId")
            .installAutotracking(true)
        createTracker(listOf(trackerConfiguration, plugin))

        Thread.sleep(1500)

        // check if event was tracked
        Assert.assertTrue(eventTracked)
    }

    @Test
    fun testDoesntTrackInstallEventIfPreviouslyTracked() {
        // plugin to check if event was tracked
        var eventTracked = false
        val plugin = PluginConfiguration("testPlugin")
        plugin.afterTrack { eventTracked = true }

        // create tracker with install autotracking
        val trackerConfiguration = TrackerConfiguration("appId")
            .installAutotracking(true)
        createTracker(listOf(trackerConfiguration, plugin))

        Thread.sleep(1500)

        // check if event was tracked
        Assert.assertTrue(eventTracked)

        // reset flag
        eventTracked = false

        // create tracker again
        createTracker(listOf(trackerConfiguration, plugin))

        Thread.sleep(1500)

        // check if event was tracked
        Assert.assertFalse(eventTracked)
    }

    private fun cleanSharedPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().clear().commit()
    }

    private fun createTracker(configurations: List<Configuration>): TrackerController {
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        return createTracker(
            context,
            namespace = "ns" + Math.random().toString(),
            network = networkConfig,
            configurations = configurations.toTypedArray()
        )
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
