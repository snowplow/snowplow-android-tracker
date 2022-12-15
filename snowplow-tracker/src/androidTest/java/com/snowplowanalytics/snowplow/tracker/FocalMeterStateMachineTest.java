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

import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.FocalMeterConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.Structured;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(AndroidJUnit4.class)
public class FocalMeterStateMachineTest {

    TrackerController tracker;
    MockWebServer mockServer;

    @Before
    public void setUp() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        String uri = String.format("http://%s:%d", mockServer.getHostName(), mockServer.getPort());

        NetworkConfiguration networkConfig = new NetworkConfiguration(new MockNetworkConnection(GET,200));
        FocalMeterConfiguration focalMeterConfig = new FocalMeterConfiguration(uri);

        String trackerNamespace = String.valueOf(Math.random());
        Snowplow.removeAllTrackers();
        tracker = Snowplow.createTracker(getContext(), trackerNamespace, networkConfig, focalMeterConfig);
    }

    @After
    public void tearDown() throws IOException {
        Snowplow.removeAllTrackers();
        mockServer.shutdown();
    }

    // --- TESTS

    @Test
    public void makesRequestToKantarEndpointWithUserId() throws InterruptedException {
        tracker.track(new Structured("cat", "act"));

        String userId = tracker.getSession().getUserId();

        RecordedRequest request = mockServer.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request);

        String path = request.getPath();
        assertTrue(path.contains("cs_fpid=" + userId));
    }

    @Test
    public void makesRequestToKantarEndpointWhenUserIdChanges() throws InterruptedException {
        // enable user anonymisation, should trigger request with anonymous user id
        tracker.setUserAnonymisation(true);
        tracker.track(new Structured("cat", "act"));

        RecordedRequest request1 = mockServer.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request1);
        assertTrue(request1.getPath().contains("cs_fpid=00000000-0000-0000-0000-000000000000"));

        // track another event with the same user ID – should not trigger request
        tracker.track(new Structured("cat", "act"));

        // disable user anonymisation, should trigger new request
        tracker.setUserAnonymisation(false);
        tracker.track(new Structured("cat", "act"));

        String userId = tracker.getSession().getUserId();
        RecordedRequest request2 = mockServer.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request2);
        assertTrue(request2.getPath().contains("cs_fpid=" + userId));
    }

    // --- PRIVATE

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

}
