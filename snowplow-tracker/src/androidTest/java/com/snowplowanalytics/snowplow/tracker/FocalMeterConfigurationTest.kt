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
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.Structured
import com.snowplowanalytics.snowplow.network.HttpMethod
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class FocalMeterConfigurationTest {

    @After
    fun tearDown() {
        removeAllTrackers()
    }

    // --- TESTS
    @Test
    fun logsSuccessfulRequest() {
        withMockServer(200) { mockServer, endpoint ->
            val focalMeter = FocalMeterConfiguration(endpoint)
            val debugs = mutableListOf<String>()
            val loggerDelegate = createLoggerDelegate(debugs = debugs)
            val trackerConfig = TrackerConfiguration(appId = "app-id")
            trackerConfig.logLevel(LogLevel.DEBUG)
            trackerConfig.loggerDelegate(loggerDelegate)

            val tracker = createTracker(listOf(focalMeter, trackerConfig))
            tracker.track(Structured("cat", "act"))
            tracker.track(Structured("cat", "act"))
            tracker.track(Structured("cat", "act"))

            Thread.sleep(500)
            Assert.assertEquals(
                1,
                debugs.filter {
                    it.contains("Request to Kantar endpoint sent with user ID: ${tracker.session?.userId}")
                }.size
            )
        }
    }

    @Test
    fun logsSuccessfulRequestWithProcessedUserId() {
        withMockServer(200) { mockServer, endpoint ->
            val focalMeter = FocalMeterConfiguration(
                kantarEndpoint = endpoint,
                processUserId = { userId -> "processed-" + userId }
            )
            val debugs = mutableListOf<String>()
            val loggerDelegate = createLoggerDelegate(debugs = debugs)
            val trackerConfig = TrackerConfiguration(appId = "app-id")
            trackerConfig.logLevel(LogLevel.DEBUG)
            trackerConfig.loggerDelegate(loggerDelegate)

            val tracker = createTracker(listOf(focalMeter, trackerConfig))
            tracker.track(Structured("cat", "act"))

            Thread.sleep(500)
            Assert.assertEquals(
                1,
                debugs.filter {
                    it.contains("Request to Kantar endpoint sent with user ID: processed-${tracker.session?.userId}")
                }.size
            )
        }
    }

    @Test
    fun makesAnotherRequestWhenUserIdChanges() {
        withMockServer(200) { mockServer, endpoint ->
            val focalMeter = FocalMeterConfiguration(endpoint)
            val debugs = mutableListOf<String>()
            val loggerDelegate = createLoggerDelegate(debugs = debugs)
            val trackerConfig = TrackerConfiguration(appId = "app-id")
            trackerConfig.logLevel(LogLevel.DEBUG)
            trackerConfig.loggerDelegate(loggerDelegate)

            val tracker = createTracker(listOf(focalMeter, trackerConfig))
            tracker.track(Structured("cat", "act"))
            val firstUserId = tracker.session?.userId
            tracker.session?.startNewSession()
            tracker.track(Structured("cat", "act"))
            val secondUserId = tracker.session?.userId

            Thread.sleep(500)
            Assert.assertEquals(
                1,
                debugs.filter {
                    it.contains("Request to Kantar endpoint sent with user ID: ${firstUserId}")
                }.size
            )
            Assert.assertEquals(
                1,
                debugs.filter {
                    it.contains("Request to Kantar endpoint sent with user ID: ${secondUserId}")
                }.size
            )
        }
    }

    @Test
    fun logsFailedRequest() {
        withMockServer(500) { mockServer, endpoint ->
            val focalMeter = FocalMeterConfiguration(endpoint)
            val errors = mutableListOf<String>()
            val loggerDelegate = createLoggerDelegate(errors = errors)
            val trackerConfig = TrackerConfiguration(appId = "app-id")
            trackerConfig.logLevel(LogLevel.DEBUG)
            trackerConfig.loggerDelegate(loggerDelegate)

            val tracker = createTracker(listOf(focalMeter, trackerConfig))
            tracker.track(Structured("cat", "act"))

            Thread.sleep(500)
            Assert.assertEquals(
                1,
                errors.filter {
                    it.contains("Request to Kantar endpoint failed with code: 500")
                }.size
            )
        }
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

    private fun withMockServer(responseCode: Int, callback: (MockWebServer, String) -> Unit) {
        val mockServer = MockWebServer()
        mockServer.start()
        val mockResponse = MockResponse()
            .setResponseCode(responseCode)
            .setHeader("Content-Type", "application/json")
            .setBody("")
        mockServer.enqueue(mockResponse)
        val endpoint = String.format("http://%s:%d", mockServer.hostName, mockServer.port)
        callback(mockServer, endpoint)
        mockServer.shutdown()
    }

    private fun createLoggerDelegate(
        errors: MutableList<String> = mutableListOf(),
        debugs: MutableList<String> = mutableListOf(),
        verboses: MutableList<String> = mutableListOf()
    ): LoggerDelegate {
        return object : LoggerDelegate {

            override fun error(tag: String, msg: String) {
                errors.add(msg)
            }

            override fun debug(tag: String, msg: String) {
                debugs.add(msg)
            }

            override fun verbose(tag: String, msg: String) {
                verboses.add(msg)
            }
        }
    }
}
