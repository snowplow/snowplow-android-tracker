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
package com.snowplowanalytics.snowplow.tracker

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.emitter.Emitter
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.network.HttpMethod
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoggingTest {
    class MockLoggerDelegate : LoggerDelegate {
        var capturedLogs = ""
        override fun error(tag: String, msg: String) {
            capturedLogs += "$tag $msg (error)\n"
        }

        override fun debug(tag: String, msg: String) {
            capturedLogs += "$tag $msg (debug)\n"
        }

        override fun verbose(tag: String, msg: String) {
            capturedLogs += "$tag $msg (verbose)\n"
        }
    }

    private var mockLoggerDelegate: MockLoggerDelegate? = null
    var emitter: Emitter? = null
    var tracker: Tracker? = null
    private var networkConfig: NetworkConfiguration? = null
    
    @Before
    @Throws(Exception::class)
    fun setUp() {
        mockLoggerDelegate = MockLoggerDelegate()
        val builder = { emitter: Emitter -> emitter.bufferOption = BufferOption.Single }
        emitter = Emitter(ApplicationProvider.getApplicationContext(), "http://localhost", builder)
        networkConfig = NetworkConfiguration("http://localhost", HttpMethod.POST)
    }

    // Tests
    // The Emitter logs at error level during failed attempts to send, but it's difficult to delay JUnit long enough to reach that point
    // Therefore these tests look at verbose and debug logging only
    @Test
    fun verboseLogsShownWhenVerboseSet() {
        val trackerBuilder = { tracker: Tracker ->
            tracker.logLevel = LogLevel.VERBOSE
            tracker.sessionContext = true
            tracker.loggerDelegate = mockLoggerDelegate
        }
        tracker = Tracker(
            emitter!!,
            "namespace",
            "myAppId",
            null,
            ApplicationProvider.getApplicationContext(),
            trackerBuilder
        )
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }

    @Test
    fun verboseLogsWithTrackerConfig() {
        val trackerConfig = TrackerConfiguration("appId")
            .logLevel(LogLevel.VERBOSE)
            .loggerDelegate(mockLoggerDelegate)
            .sessionContext(true)
        createTracker(
            ApplicationProvider.getApplicationContext(),
            "appTracker",
            networkConfig!!,
            trackerConfig
        )
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }

    @Test
    fun debugLogsShownWhenDebugSet() {
        val trackerBuilder = { tracker: Tracker ->
            tracker.logLevel = LogLevel.DEBUG
            tracker.sessionContext = true
            tracker.loggerDelegate = mockLoggerDelegate
        }
        tracker = Tracker(
            emitter!!,
            "namespace",
            "myAppId",
            null,
            ApplicationProvider.getApplicationContext(),
            trackerBuilder
        )
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }

    @Test
    fun debugLogsWithTrackerConfig() {
        val trackerConfig = TrackerConfiguration("appId")
            .logLevel(LogLevel.DEBUG)
            .loggerDelegate(mockLoggerDelegate)
            .sessionContext(true)
        createTracker(
            ApplicationProvider.getApplicationContext(),
            "appTracker",
            networkConfig!!,
            trackerConfig
        )
        Assert.assertTrue(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }

    @Test
    fun loggingOffByDefault() {
        val trackerBuilder = { tracker: Tracker ->
            tracker.sessionContext = true
            tracker.loggerDelegate = mockLoggerDelegate
        }
        tracker = Tracker(
            emitter!!,
            "namespace",
            "myAppId",
            null,
            ApplicationProvider.getApplicationContext(),
            trackerBuilder
        )
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }

    @Test
    fun loggingOffByDefaultWithConfig() {
        val trackerConfig = TrackerConfiguration("appId")
            .loggerDelegate(mockLoggerDelegate)
            .sessionContext(true)
        createTracker(
            ApplicationProvider.getApplicationContext(),
            "appTracker",
            networkConfig!!,
            trackerConfig
        )
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Session checking has been resumed. (debug)"))
        Assert.assertFalse(mockLoggerDelegate!!.capturedLogs.contains("Tracker created successfully. (verbose)"))
    }
}
