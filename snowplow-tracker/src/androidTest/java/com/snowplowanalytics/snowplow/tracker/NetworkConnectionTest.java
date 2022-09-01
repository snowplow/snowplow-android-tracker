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

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.network.CollectorCookieJar;
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection;
import com.snowplowanalytics.snowplow.network.RequestResult;
import com.snowplowanalytics.snowplow.internal.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

@RunWith(AndroidJUnit4.class)
public class NetworkConnectionTest {

    @Test
    public void testGetRequestWithSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
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
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    @Test
    public void testGetRequestWithNoSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(404);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
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
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    @Test
    public void testPostRequestWithSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
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
        assertTrue(result.isSuccessful());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    @Test
    public void testPostRequestWithNoSuccess() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(404);
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
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
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getEventIds().get(0).longValue());

        mockServer.shutdown();
    }

    @Test
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
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
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

    @Test
    public void testFreeEndpoint_GetHttpsUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("acme.test.url.com", getContext())
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("https://acme.test.url.com"));
    }

    @Test
    public void testHttpsEndpoint_GetHttpsUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("https://acme.test.url.com", getContext())
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("https://acme.test.url.com"));
    }

    @Test
    public void testHttpEndpoint_GetHttpUrl() {
        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder("http://acme.test.url.com", getContext())
                        .method(POST)
                        .build();
        assertTrue(connection.getUri().toString().startsWith("http://acme.test.url.com"));
    }

    @Test
    public void testRequestWithCookies() throws IOException, InterruptedException {
        MockWebServer mockServer = new MockWebServer();
        mockServer.start();

        mockServer.enqueue(
                new MockResponse().addHeader("Set-Cookie", "sp=test")
        );

        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
                        .method(POST)
                        .tls(EnumSet.of(TLSVersion.TLSv1_1, TLSVersion.TLSv1_2))
                        .emitTimeout(10)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");

        connection.sendRequests(Arrays.asList(new Request(payload, 1)));
        mockServer.takeRequest(60, TimeUnit.SECONDS);

        connection.sendRequests(Arrays.asList(new Request(payload, 2)));

        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertEquals("sp=test", req.getHeader("Cookie"));

        mockServer.shutdown();
        new CollectorCookieJar(getContext()).clear();
    }

    @Test
    public void testDoesntAddHeaderWithoutServerAnonymisation() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);

        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
                        .method(POST)
                        .serverAnonymisation(false)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");

        connection.sendRequests(Arrays.asList(new Request(payload, 2)));

        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertNull(req.getHeader("SP-Anonymous"));

        mockServer.shutdown();
    }

    @Test
    public void testAddsHeaderForServerAnonymisationForPostRequest() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);

        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
                        .method(POST)
                        .serverAnonymisation(true)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");

        connection.sendRequests(Arrays.asList(new Request(payload, 2)));

        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertEquals("*", req.getHeader("SP-Anonymous"));

        mockServer.shutdown();
    }

    @Test
    public void testAddsHeaderForServerAnonymisationForGetRequest() throws IOException, InterruptedException {
        MockWebServer mockServer = getMockServer(200);

        OkHttpNetworkConnection connection =
                new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(getMockServerURI(mockServer), getContext())
                        .method(GET)
                        .serverAnonymisation(true)
                        .build();

        Payload payload = new TrackerPayload();
        payload.add("key", "value");

        connection.sendRequests(Arrays.asList(new Request(payload, 2)));

        RecordedRequest req = mockServer.takeRequest(60, TimeUnit.SECONDS);
        assertEquals("*", req.getHeader("SP-Anonymous"));

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
            return String.format("http://%s:%d", mockServer.getHostName(), mockServer.getPort());
        }
        return null;
    }


    private Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
}
