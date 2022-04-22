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

package com.snowplowanalytics.snowplow.tracker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.network.HttpMethod;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LoggingTest {

    static class MockLoggerDelegate implements LoggerDelegate {
        String capturedLogs = "";

        @Override
        public void error(@NonNull String tag, @NonNull String msg) {
            capturedLogs += tag + " " + msg + " (error)\n";
        }

        @Override
        public void debug(@NonNull String tag, @NonNull String msg) {
            capturedLogs += tag + " " + msg + " (debug)\n";
        }

        @Override
        public void verbose(@NonNull String tag, @NonNull String msg) {
            capturedLogs += tag + " " + msg + " (verbose)\n";
        }
    }

    MockLoggerDelegate mockLoggerDelegate;
    Emitter emitter;
    Tracker tracker;
    NetworkConfiguration networkConfig;

    @Before
    public void setUp() throws Exception {
        mockLoggerDelegate = new MockLoggerDelegate();
        emitter = new Emitter(ApplicationProvider.getApplicationContext(), "http://localhost", new Emitter.EmitterBuilder()
                .option(BufferOption.Single)
        );
        networkConfig = new NetworkConfiguration("http://localhost", HttpMethod.POST);
    }

    // Tests

    // The Emitter logs at error level during failed attempts to send, but it's difficult to delay JUnit long enough to reach that point
    // Therefore these tests look at verbose and debug logging only

    @Test
    public void VerboseLogsShownWhenVerboseSet() {
        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "myAppId", ApplicationProvider.getApplicationContext())
                .sessionContext(true)
                .level(LogLevel.VERBOSE)
                .loggerDelegate(mockLoggerDelegate)
        );

        assertTrue(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertTrue(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }

    @Test
    public void VerboseLogsWithTrackerConfig() {
        TrackerConfiguration trackerConfig = new TrackerConfiguration("appId")
                .logLevel(LogLevel.VERBOSE)
                .loggerDelegate(mockLoggerDelegate)
                .sessionContext(true);

        Snowplow.createTracker(ApplicationProvider.getApplicationContext(),
                "appTracker",
                networkConfig,
                trackerConfig);

        assertTrue(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertTrue(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }

    @Test
    public void DebugLogsShownWhenDebugSet() {
        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "myAppId", ApplicationProvider.getApplicationContext())
                .sessionContext(true)
                .level(LogLevel.DEBUG)
                .loggerDelegate(mockLoggerDelegate)
        );

        assertTrue(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertFalse(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }

    @Test
    public void DebugLogsWithTrackerConfig() {
        TrackerConfiguration trackerConfig = new TrackerConfiguration("appId")
                .logLevel(LogLevel.DEBUG)
                .loggerDelegate(mockLoggerDelegate)
                .sessionContext(true);

        Snowplow.createTracker(ApplicationProvider.getApplicationContext(),
                "appTracker",
                networkConfig,
                trackerConfig);

        assertTrue(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertFalse(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }

    @Test
    public void LoggingOffByDefault() {
        tracker = new Tracker(new Tracker.TrackerBuilder(emitter, "namespace", "myAppId", ApplicationProvider.getApplicationContext())
                .sessionContext(true)
                .loggerDelegate(mockLoggerDelegate)
        );

        assertFalse(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertFalse(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }

    @Test
    public void LoggingOffByDefaultWithConfig() {
        TrackerConfiguration trackerConfig = new TrackerConfiguration("appId")
                .loggerDelegate(mockLoggerDelegate)
                .sessionContext(true);

        Snowplow.createTracker(ApplicationProvider.getApplicationContext(),
                "appTracker",
                networkConfig,
                trackerConfig);

        assertFalse(mockLoggerDelegate.capturedLogs.contains("Session checking has been resumed. (debug)"));
        assertFalse(mockLoggerDelegate.capturedLogs.contains("Tracker created successfully. (verbose)"));
    }
}
