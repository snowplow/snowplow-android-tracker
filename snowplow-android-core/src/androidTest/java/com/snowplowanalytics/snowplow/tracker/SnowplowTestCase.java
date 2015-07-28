/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class SnowplowTestCase extends AndroidTestCase {

    MockWebServer mockServer;
    
    public void setupMockServer() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
        }
        mockServer = new MockWebServer();
        mockServer.play();
    }

    public String mockServerName() {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }

    public EmittableEvents setupEmittableEvents(int count) {
        ArrayList<Payload> events = new ArrayList<Payload>();
        LinkedList<Long> eventIds = new LinkedList<Long>();
        for (int i = 0; i < count; i++) {
            TrackerPayload payload = new TrackerPayload();
            payload.add("a", String.valueOf(i));
            events.add(payload);
            eventIds.add(new Long(i));
            mockServer.enqueue(new MockResponse());
        }
        return new EmittableEvents(events, eventIds);
    }
    
    public void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
            mockServer = null;
        }
    }
}
