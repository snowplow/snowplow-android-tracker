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

import static junit.framework.TestCase.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerWebViewInterface;
import com.snowplowanalytics.snowplow.network.Request;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class TrackerWebViewInterfaceTest {

    private TrackerWebViewInterface webInterface;
    private MockNetworkConnection networkConnection;

    @Before
    public void setUp() throws Exception {
        webInterface = new TrackerWebViewInterface();
        networkConnection = new MockNetworkConnection(GET,200);

        NetworkConfiguration networkConfig = new NetworkConfiguration(networkConnection);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("app1");
        trackerConfig.sessionContext = false;
        trackerConfig.platformContext = false;
        trackerConfig.base64encoding = false;

        String trackerNamespace = String.valueOf(Math.random());
        Snowplow.removeAllTrackers();
        Snowplow.createTracker(getContext(), trackerNamespace, networkConfig, trackerConfig);
    }

    @After
    public void tearDown() {
        Snowplow.removeAllTrackers();
    }

    // --- TESTS

    @Test
    public void tracksStructuredEventWithAllProperties() throws JSONException, InterruptedException {
        webInterface.trackStructEvent(
                "cat", "act", "lbl", "prop", 10.0, null, null
        );

        for (int i = 0; i < 10 && (networkConnection.countRequests() == 0); i++) {
            Thread.sleep(1000);
        }

        assertEquals(1, networkConnection.countRequests());
        Request request = networkConnection.getAllRequests().get(0);
        Map payload = request.payload.getMap();
        assertEquals("cat", payload.get("se_ca"));
        assertEquals("act", payload.get("se_ac"));
        assertEquals("prop", payload.get("se_pr"));
        assertEquals("lbl", payload.get("se_la"));
        assertEquals("10.0", payload.get("se_va"));
    }

    @Test
    public void tracksEventWithCorrectTracker() throws JSONException, InterruptedException {
        // create the second tracker
        MockNetworkConnection networkConnection2 = new MockNetworkConnection(GET,200);
        NetworkConfiguration networkConfig = new NetworkConfiguration(networkConnection2);
        Snowplow.createTracker(getContext(), "ns2", networkConfig);

        // track an event using the second tracker
        webInterface.trackPageView("http://localhost", null, null, null, new String[]{"ns2"});

        // wait and check for the event
        for (int i = 0; i < 10 && (networkConnection2.countRequests() == 0); i++) {
            Thread.sleep(1000);
        }
        assertEquals(0, networkConnection.countRequests());
        assertEquals(1, networkConnection2.countRequests());
    }

    @Test
    public void tracksEventWithContext() throws JSONException, InterruptedException {
        webInterface.trackSelfDescribingEvent(
                "http://schema.com",
                "{\"key\":\"val\"}",
                "[{\"schema\":\"http://context-schema.com\",\"data\":{\"a\":\"b\"}}]",
                null);

        for (int i = 0; i < 10 && (networkConnection.countRequests() == 0); i++) {
            Thread.sleep(1000);
        }

        assertEquals(1, networkConnection.countRequests());
        Request request = networkConnection.getAllRequests().get(0);
        JSONObject parsedEntity = new JSONObject((String) request.payload.getMap().get("co"))
                .getJSONArray("data")
                .getJSONObject(0);
        assertEquals("http://context-schema.com", parsedEntity.getString("schema"));
        assertEquals("b", parsedEntity.getJSONObject("data").getString("a"));
    }

    // --- PRIVATE

    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

}
