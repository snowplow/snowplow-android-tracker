/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection;
import com.snowplowanalytics.snowplow.network.RequestResult;
import com.snowplowanalytics.snowplow.internal.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.network.HttpMethod.POST;
import static com.snowplowanalytics.snowplow.network.Protocol.HTTP;

public class NetworkConnectionTest extends AndroidTestCase {

    public void testGetRequestWithSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
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

    public void testCustomClientIsUsed() throws IOException, InterruptedException {
        AtomicBoolean hasClientBeenUsed = new AtomicBoolean(false);
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        hasClientBeenUsed.set(true);
                        return chain.proceed(chain.request());
                    }
                })
                .build();

        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer))
                        .method(GET)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .client(client)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");
        Request request = new Request(payload, 1);
        List<Request> requests = new ArrayList<>(1);
        requests.add(request);

        List<RequestResult> results = connection.sendRequests(requests);
        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertNotNull(req);
        assertTrue(hasClientBeenUsed.get());

        mockServer.shutdown();
    }

    public void testFreeEndpoint_GetHttpsUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("acme.test.url.com")
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("https://acme.test.url.com"));
    }

    public void testHttpsEndpoint_GetHttpsUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("https://acme.test.url.com")
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("https://acme.test.url.com"));
    }

    public void testHttpEndpoint_GetHttpUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("http://acme.test.url.com")
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("http://acme.test.url.com"));
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
            return String.format("http://%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }
}
