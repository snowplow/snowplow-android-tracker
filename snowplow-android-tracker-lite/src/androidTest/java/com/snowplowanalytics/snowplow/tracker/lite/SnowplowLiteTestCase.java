package com.snowplowanalytics.snowplow.tracker.lite;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.*;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class SnowplowLiteTestCase extends AndroidTestCase {
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

    com.snowplowanalytics.snowplow.tracker.Emitter getEmitter(HttpMethod method, BufferOption option, RequestSecurity security) {
        return new com.snowplowanalytics.snowplow.tracker.Emitter.EmitterBuilder(mockServerName(), getContext())
                .option(option)
                .method(method)
                .security(security)
                .build();
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
