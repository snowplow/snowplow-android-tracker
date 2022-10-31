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

import static com.snowplowanalytics.snowplow.network.HttpMethod.POST;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ServiceProviderTest {

    @Test
    public void testUpdatingConfigurationRetainsPausedEmitter() throws InterruptedException {
        NetworkConfiguration networkConfig = new NetworkConfiguration("com.acme", POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("appId");
        trackerConfig.installAutotracking = false;
        trackerConfig.lifecycleAutotracking = false;
        trackerConfig.screenViewAutotracking = false;
        trackerConfig.diagnosticAutotracking = false;
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, 200);
        networkConfig.networkConnection = networkConnection;
        List<Configuration> configurations = new ArrayList<>();
        configurations.add(trackerConfig);
        ServiceProvider provider = new ServiceProvider(getContext(), "ns", networkConfig, configurations);

        // pause emitter
        provider.getOrMakeEmitterController().pause();

        // refresh configuration
        List<Configuration> configurationUpdates = new ArrayList<Configuration>();
        configurationUpdates.add(new EmitterConfigurationUpdate());
        provider.reset(configurationUpdates);

        // track event and check that emitter is paused
        provider.getOrMakeTrackerController().track(new Structured("cat", "act"));
        Thread.sleep(1000);
        assertFalse(provider.getOrMakeEmitter().getEmitterStatus());
        assertEquals(0, networkConnection.sendingCount());

        // resume emitting
        provider.getOrMakeEmitterController().resume();
        for (int i = 0; i < 10 && networkConnection.sendingCount() < 1; i++) {
            Thread.sleep(600);
        }
        assertEquals(1, networkConnection.sendingCount());
        provider.getOrMakeEmitter().flush();
    }

    @Test
    public void testLogsErrorWhenAccessingShutDownTracker() {
        NetworkConfiguration networkConfig = new NetworkConfiguration("com.acme", POST);
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, 200);
        networkConfig.networkConnection = networkConnection;
        ServiceProvider provider = new ServiceProvider(getContext(), "ns", networkConfig, new ArrayList<>());

        // listen for the error log
        TrackerController tracker = provider.getOrMakeTrackerController();
        final boolean[] loggedError = {false};
        tracker.setLoggerDelegate(new LoggerDelegate() {
            @Override
            public void error(@NonNull String tag, @NonNull String msg) {
                if (msg.contains("Recreating tracker instance")) {
                    loggedError[0] = true;
                }
            }

            @Override
            public void debug(@NonNull String tag, @NonNull String msg) {
            }

            @Override
            public void verbose(@NonNull String tag, @NonNull String msg) {
            }
        });

        // shutting down and accessing the tracker should log the error
        provider.shutdown();
        tracker.getNamespace();
        assertTrue(loggedError[0]);
    }

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
