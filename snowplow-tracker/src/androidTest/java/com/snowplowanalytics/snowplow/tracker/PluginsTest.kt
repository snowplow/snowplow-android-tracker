/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class PluginsTest {

    @After
    fun tearDown() {
        removeAllTrackers()
    }

    // --- TESTS
    @Test
    fun addsEntitiesToEvent() {
        val plugin = PluginConfiguration("plugin")
        plugin.entities { Collections.singletonList(
            SelfDescribingJson("schema", Collections.singletonMap("val", it.payload["se_ca"]))
        ) }

        val testPlugin = PluginConfiguration("test")
        var expectation = false
        testPlugin.afterTrack {
            expectation = it.entities.filter {
                val data = it.map["data"] as Map<*, *>?
                it.map["schema"] == "schema" && data?.get("val") == "cat"
            }.isNotEmpty()
        }

        val tracker = createTracker(listOf(plugin, testPlugin))
        tracker.track(Structured("cat", "act"))

        Thread.sleep(100)
        Assert.assertTrue(expectation)
    }

    @Test
    fun addsEntitiesFromMultiplePlugins() {
        val plugin1 = PluginConfiguration("plugin1")
        plugin1.entities { listOf(SelfDescribingJson("schema1", emptyMap<String, String>())) }

        val plugin2 = PluginConfiguration("plugin2")
        plugin2.entities { listOf(SelfDescribingJson("schema2", emptyMap<String, String>())) }

        val testPlugin = PluginConfiguration("test")
        var expectation = false
        testPlugin.afterTrack {
            expectation = it.entities.filter { it.map["schema"] == "schema1" }.size == 1 &&
                    it.entities.filter { it.map["schema"] == "schema2" }.size == 1
        }

        val tracker = createTracker(listOf(plugin1, plugin2, testPlugin))
        tracker.track(ScreenView("sv"))

        Thread.sleep(100)
        Assert.assertTrue(expectation)
    }

    @Test
    fun addsEntitiesOnlyForEventsMatchingSchema() {
        val plugin = PluginConfiguration("plugin")
        plugin.entities(listOf("schema1")) {
            listOf(SelfDescribingJson("xx", emptyMap<String, String>()))
        }

        var event1HasEntity: Boolean? = null
        var event2HasEntity: Boolean? = null

        val testPlugin = PluginConfiguration("test")
        testPlugin.afterTrack {
            if (it.schema == "schema1") {
                event1HasEntity = it.entities.filter { it.map["schema"] == "xx" }.isNotEmpty()
            }
            if (it.schema == "schema2") {
                event2HasEntity = it.entities.filter { it.map["schema"] == "xx" }.isNotEmpty()
            }
        }

        val tracker = createTracker(listOf(plugin, testPlugin))
        tracker.track(SelfDescribing("schema1", emptyMap()))
        tracker.track(SelfDescribing("schema2", emptyMap()))

        Thread.sleep(100)
        Assert.assertTrue(event1HasEntity!!)
        Assert.assertFalse(event2HasEntity!!)
    }

    @Test
    fun callsAfterTrackOnlyForEventsMatchingSchema() {
        var event1Called = false
        var event2Called = false
        var event3Called = false

        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack(listOf("schema1")) {
            if (it.schema == "schema1") { event1Called = true }
            if (it.schema == "schema2") { event2Called = true }
            if (it.schema == null) { event3Called = true }
        }

        val tracker = createTracker(listOf(plugin))
        tracker.track(SelfDescribing("schema1", emptyMap()))
        tracker.track(SelfDescribing("schema2", emptyMap()))
        tracker.track(Structured("cat", "act"))

        Thread.sleep(100)
        Assert.assertTrue(event1Called)
        Assert.assertFalse(event2Called)
        Assert.assertFalse(event3Called)
    }

    @Test
    fun callsAfterTrackOnlyForStructuredEvent() {
        var selfDescribingCalled = false
        var structuredCalled = false

        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack(listOf("se")) {
            if (it.schema == "schema1") { selfDescribingCalled = true }
            if (it.schema == null) { structuredCalled = true }
        }

        val tracker = createTracker(listOf(plugin))
        tracker.track(SelfDescribing("schema1", emptyMap()))
        tracker.track(Structured("cat", "act"))

        Thread.sleep(100)
        Assert.assertTrue(structuredCalled)
        Assert.assertFalse(selfDescribingCalled)
    }

    @Test
    fun addsPluginToTracker() {
        val tracker = createTracker(emptyList())

        val plugin = PluginConfiguration("plugin")
        var expectation = false
        plugin.afterTrack { expectation = true }
        tracker.plugins.addPlugin(plugin)

        tracker.track(ScreenView("sv"))

        Thread.sleep(100)
        Assert.assertTrue(expectation)
    }

    @Test
    fun removesPluginFromTracker() {
        var pluginCalled = false
        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack { pluginCalled = true }

        val tracker = createTracker(listOf(plugin))
        tracker.plugins.removePlugin("plugin")

        tracker.track(ScreenView("sv"))

        Thread.sleep(100)
        Assert.assertFalse(pluginCalled)
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

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
