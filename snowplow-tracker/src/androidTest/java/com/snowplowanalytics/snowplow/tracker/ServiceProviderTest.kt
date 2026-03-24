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
import com.snowplowanalytics.core.tracker.ServiceProvider
import com.snowplowanalytics.snowplow.configuration.Configuration
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class ServiceProviderTest {
    @Test
    @Throws(InterruptedException::class)
    fun testUpdatingConfigurationRetainsPausedEmitter() {
        val networkConfig = NetworkConfiguration("com.acme", HttpMethod.POST)
        val trackerConfig = TrackerConfiguration("appId")
        trackerConfig.installAutotracking = false
        trackerConfig.lifecycleAutotracking = false
        trackerConfig.screenViewAutotracking = false
        trackerConfig.diagnosticAutotracking = false
        val networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        networkConfig.networkConnection = networkConnection
        val configurations: MutableList<Configuration> = ArrayList()
        configurations.add(trackerConfig)
        val provider = ServiceProvider(
            context, "ns", networkConfig, configurations
        )

        // pause emitter
        provider.getOrMakeEmitterController().pause()

        // refresh configuration
        val configurationUpdates: MutableList<Configuration> = ArrayList()
        configurationUpdates.add(EmitterConfiguration())
        provider.reset(configurationUpdates)

        // track event and check that emitter is paused
        provider.getOrMakeTrackerController().track(Structured("cat", "act"))
        Thread.sleep(1000)
        Assert.assertFalse(provider.getOrMakeEmitter().emitterStatus)
        Assert.assertEquals(0, networkConnection.sendingCount().toLong())

        // resume emitting
        provider.getOrMakeEmitterController().resume()
        var i = 0
        while (i < 10 && networkConnection.sendingCount() < 1) {
            Thread.sleep(600)
            i++
        }
        Assert.assertEquals(1, networkConnection.sendingCount().toLong())
        provider.getOrMakeEmitter().flush()
    }

    @Test
    fun testLogsErrorWhenAccessingShutDownTracker() {
        val networkConfig = NetworkConfiguration("com.acme", HttpMethod.POST)
        val networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        networkConfig.networkConnection = networkConnection
        val provider = ServiceProvider(
            context, "ns", networkConfig, ArrayList()
        )

        // listen for the error log
        val tracker: TrackerController = provider.getOrMakeTrackerController()
        val loggedError = booleanArrayOf(false)
        tracker.loggerDelegate = object : LoggerDelegate {
            override fun error(tag: String, msg: String) {
                if (msg.contains("Recreating tracker instance")) {
                    loggedError[0] = true
                }
            }

            override fun debug(tag: String, msg: String) {}
            override fun verbose(tag: String, msg: String) {}
        }

        // shutting down and accessing the tracker should log the error
        provider.shutdown()
        tracker.namespace
        Assert.assertTrue(loggedError[0])
    }

    @Test
    fun testConcurrentResetDoesNotCreateDuplicateEmitters() {
        val networkConfig = NetworkConfiguration("com.acme", HttpMethod.POST)
        networkConfig.networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        val trackerConfig = TrackerConfiguration("appId").apply {
            installAutotracking = false
            lifecycleAutotracking = false
            screenViewAutotracking = false
            diagnosticAutotracking = false
        }
        val provider = ServiceProvider(context, "ns-concurrent", networkConfig, listOf(trackerConfig))

        // The race: reset() sets emitter = null, then two threads both see null and
        // each call makeEmitter(), producing two live emitters sharing the same SQLite queue.
        // We reproduce this by calling reset() to null the emitter, then firing two threads
        // simultaneously at getOrMakeEmitter(). Without the fix they return different instances;
        // with @Synchronized they must return the same one.
        val iterations = 500
        val duplicatesDetected = AtomicBoolean(false)

        repeat(iterations) {
            // Force emitter = null via reset so both threads race to (re)create it
            provider.reset(listOf(TrackerConfiguration("appId")))

            val startGate = CountDownLatch(1)
            val doneLatch = CountDownLatch(2)
            val emitter1Ref = arrayOfNulls<com.snowplowanalytics.core.emitter.Emitter>(1)
            val emitter2Ref = arrayOfNulls<com.snowplowanalytics.core.emitter.Emitter>(1)

            Thread {
                startGate.await()
                emitter1Ref[0] = provider.getOrMakeEmitter()
                doneLatch.countDown()
            }.start()

            Thread {
                startGate.await()
                emitter2Ref[0] = provider.getOrMakeEmitter()
                doneLatch.countDown()
            }.start()

            startGate.countDown() // release both threads simultaneously
            doneLatch.await(5, TimeUnit.SECONDS)

            if (emitter1Ref[0] !== emitter2Ref[0]) {
                duplicatesDetected.set(true)
            }
        }

        Assert.assertFalse(
            "Two different emitter instances returned by concurrent getOrMakeEmitter() calls — race condition present",
            duplicatesDetected.get()
        )
        provider.shutdown()
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
}
