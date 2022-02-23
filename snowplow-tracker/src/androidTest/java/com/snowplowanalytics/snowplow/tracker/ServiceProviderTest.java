/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationUpdate;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;

import java.util.ArrayList;
import java.util.List;

public class ServiceProviderTest extends AndroidTestCase {

    public void testUpdatingConfigurationRetainsPausedEmitter() throws InterruptedException {
        NetworkConfiguration networkConfig = new NetworkConfiguration("com.acme", POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("appId");
        trackerConfig.installAutotracking = false;
        trackerConfig.lifecycleAutotracking = false;
        trackerConfig.screenViewAutotracking = false;
        trackerConfig.diagnosticAutotracking = false;
        MockNetworkConnection networkConnection = new MockNetworkConnection(POST, true);
        networkConfig.networkConnection = networkConnection;
        List<Configuration> configurations = new ArrayList<>();
        configurations.add(trackerConfig);
        ServiceProvider provider = new ServiceProvider(getContext(), "ns", networkConfig, configurations);

        // pause emitter
        provider.getEmitterController().pause();

        // refresh configuration
        List<Configuration> configurationUpdates = new ArrayList<Configuration>();
        configurationUpdates.add(new EmitterConfigurationUpdate());
        provider.reset(configurationUpdates);

        // track event and check that emitter is paused
        provider.getTrackerController().track(new Structured("cat", "act"));
        Thread.sleep(1000);
        assertFalse(provider.getEmitter().getEmitterStatus());
        assertEquals(0, networkConnection.sendingCount());

        provider.getEmitter().flush();
    }
}
