package com.snowplowanalytics.snowplow.tracker;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.tracker.networkconnection.Request;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod.POST;
import static com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity.HTTP;

public class NetworkConnectionTest extends AndroidTestCase {

    public void testGetRequestWithSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
                        .security(HTTP)
                        .method(GET)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");
        Request request = new Request(payload, 1);
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);

        List<RequestResult> results = connection.sendRequests(requests);
        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertGETRequest(req);

        // Check successful result
        RequestResult result = results.get(0);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    public void testGetRequestWithNoSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(404);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
                        .security(HTTP)
                        .method(GET)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");
        Request request = new Request(payload, 1);
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);

        List<RequestResult> results = connection.sendRequests(requests);

        // Check unsuccessful result
        RequestResult result = results.get(0);
        assertFalse(result.getSuccess());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    public void testPostRequestWithSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
                        .security(HTTP)
                        .method(POST)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");
        Request request = new Request(payload, 1);
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);

        List<RequestResult> results = connection.sendRequests(requests);
        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        try {
            assertPOSTRequest(req);
        } catch (JSONException e) {
            fail("JSON body of request malformed: " + e);
        }

        // Check successful result
        RequestResult result = results.get(0);
        assertTrue(result.getSuccess());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    public void testPostRequestWithNoSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(404);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
                        .security(HTTP)
                        .method(POST)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");
        Request request = new Request(payload, 1);
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);

        List<RequestResult> results = connection.sendRequests(requests);
        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);

        // Check unsuccessful result
        RequestResult result = results.get(0);
        assertFalse(result.getSuccess());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    // Service methods

    private void assertGETRequest(RecordedRequest req) {
        assertNotNull(req);
        assertEquals("GET", req.getMethod());
        assertEquals("Keep-Alive", req.getHeader("Connection"));
        assertEquals("/i?", req.getPath().substring(0, 3));
    }

    private JSONObject assertPOSTRequest(RecordedRequest req) throws JSONException {
        assertNotNull(req);
        assertEquals("POST", req.getMethod());
        assertEquals("application/json; charset=utf-8", req.getHeader("Content-Type"));
        assertEquals("Keep-Alive", req.getHeader("Connection"));
        assertEquals("/com.snowplowanalytics.snowplow/tp2", req.getPath());

        JSONObject payload = new JSONObject(req.getBody().readUtf8());
        assertEquals(1, payload.length());
        assertEquals(
                "value",
                payload.getString("key")
        );
        return payload;
    }



    // Mock Server

    public MockWebServer getMockServer(int responseCode) throws IOException {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();
        MockResponse mockedResponse = new MockResponse();
        mockedResponse.setResponseCode(responseCode);
        mockedResponse.setBody("{}");
        mockServer.enqueue(mockedResponse);
        return mockServer;
    }

    @SuppressLint("DefaultLocale")
    public String getMockServerURI(MockWebServer mockServer) {
        if (mockServer != null) {
            return String.format("%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }
}
