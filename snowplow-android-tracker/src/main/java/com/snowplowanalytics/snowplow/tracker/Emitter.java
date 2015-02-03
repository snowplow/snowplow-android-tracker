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

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedList;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Request;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

import rx.Observable;

public class Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Uri.Builder uriBuilder;

    protected RequestCallback requestCallback;
    protected HttpMethod httpMethod;
    protected BufferOption option = BufferOption.Default;

    /**
     * Creates an emitter object
     * @param builder The builder that constructs an emitter
     */
    private Emitter(EmitterBuilder builder) {
        this.httpMethod = builder.httpMethod;
        this.requestCallback = builder.requestCallback;

        // Need to create URI Builder in this way to preserve port keys/characters that would
        // be incorrectly encoded by the uriBuilder.
        this.uriBuilder = Uri.parse("http://" + builder.uri).buildUpon();

        // Create URI based on request method
        if (httpMethod == HttpMethod.GET) {
            uriBuilder.scheme("http").appendPath("i");
        }
        else {
            uriBuilder.scheme("http").appendEncodedPath(TrackerConstants.PROTOCOL_VENDOR + "/" +
                            TrackerConstants.PROTOCOL_VERSION);
        }

        // Set buffer option based on request method
        if (httpMethod == HttpMethod.GET) {
            setBufferOption(BufferOption.Instant);
        }
    }

    public static class EmitterBuilder {
        private final String uri; // Required
        protected RequestCallback requestCallback = null; // Optional
        protected HttpMethod httpMethod = HttpMethod.POST; // Optional

        /**
         * @param uri The uri of the collector
         */
        public EmitterBuilder(String uri) {
            this.uri = uri;
        }

        /**
         * @param httpMethod The method by which requests are emitted
         */
        public EmitterBuilder httpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        /**
         * @param requestCallback Request callback function
         */
        public EmitterBuilder requestCallback(RequestCallback requestCallback) {
            this.requestCallback = requestCallback;
            return this;
        }

        /**
         * @return a new Emitter object
         */
        public Emitter build() {
            return new Emitter(this);
        }
    }

    /**
     * Emits all the events in the EmittableEvents
     * object.
     *
     * @return Observable that will emit once containing
     * the request result.
     */
    public Observable<LinkedList<RequestResult>> emitEvent(final EmittableEvents events) {
        return Observable.just(events).map(this::performEmit);
    }

    /**
     * Synchronously performs a request sending
     * operation for either GET or POST.
     *
     * @param events the events to be sent
     * @return a RequestResult
     */
    public LinkedList<RequestResult> performEmit(EmittableEvents events) {

        Logger.ifDebug(TAG, "Performing emit", events);

        ArrayList<Payload> payloads = events.getEvents();
        LinkedList<Long> eventIds = events.getEventIds();
        LinkedList<RequestResult> results = new LinkedList<>();

        // If the request method is GET...
        if (httpMethod == HttpMethod.GET) {
            Logger.ifDebug(TAG, "Performing GET requests");

            for (int i = 0; i < payloads.size(); i++) {

                // Build the request and send it...
                Request req = requestBuilderGet(events.getEvents().get(i));
                int code = requestSender(req);

                // Get the eventId for this particular event
                LinkedList<Long> eventId = new LinkedList<>();
                eventId.add(eventIds.get(i));

                if (code == -1) {
                    results.add(new RequestResult(false, eventId));
                }
                else {
                    // Figure out if it was a success and if we can retry if it failed...
                    boolean success = code >= 200 && code < 300;
                    results.add(new RequestResult(success, eventId));
                }
            }
        }
        else {
            Logger.ifDebug(TAG, "Performing POST requests");

            // Convert the ArrayList of Payloads into an ArrayList of Maps
            ArrayList<Map> eventMaps = new ArrayList<>();
            for (Payload event : events.getEvents()) {
                eventMaps.add(event.getMap());
            }

            // As we can send multiple events in a POST we need to create a wrapper
            SchemaPayload postPayload = new SchemaPayload();
            postPayload.setSchema(TrackerConstants.SCHEMA_PAYLOAD_DATA);
            postPayload.setData(eventMaps);

            // Build the request
            Request req = requestBuilderPost(postPayload);
            int code = requestSender(req);

            if (code == -1) {
                results.add(new RequestResult(false, eventIds));
            }
            else {
                // Figure out if it was a success and if we can retry if it failed...
                boolean success = code >= 200 && code < 300;
                results.add(new RequestResult(success, eventIds));
            }
        }
        return results;
    }

    /**
     * The function responsible for actually sending
     * the request to the collector.
     *
     * @param request The request to be sent
     * @return a RequestResult
     */
    private int requestSender(Request request) {
        try {
            Logger.ifDebug(TAG, "Sending request...", request);
            return client.newCall(request).execute().code();
        } catch (IOException e) {
            Logger.ifDebug(TAG, "Request sending failed exceptionally", e);
            return -1;
        }
    }

    // Request Builders

    /**
     * Builds an OkHttp GET request which is ready
     * to be executed.
     * @param payload The payload to be sent in the
     *                request.
     * @return an OkHttp request object
     */
    @SuppressWarnings("unchecked")
    private Request requestBuilderGet(Payload payload) {

        // Clear the previous query...
        uriBuilder.clearQuery();

        // Build the request query...
        HashMap hashMap = (HashMap) payload.getMap();
        Iterator<String> iterator = hashMap.keySet().iterator();

        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = (String) hashMap.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }

        // Build the request
        String reqUrl = uriBuilder.build().toString();
        Request req = new Request.Builder()
                .url(reqUrl)
                .get()
                .build();

        Logger.ifDebug(TAG, "New GET Request made", req);
        return req;
    }

    /**
     * Builds an OkHttp POST request which is ready
     * to be executed.
     * @param payload The payload to be sent in the
     *                request.
     * @return an OkHttp request object
     */
    private Request requestBuilderPost(Payload payload) {
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, payload.toString());
        Request req = new Request.Builder()
                .url(reqUrl)
                .post(reqBody)
                .build();

        Logger.ifDebug(TAG, "New POST Request made", req);
        return req;
    }

    // Setters and Getters

    /**
     * Sets whether the buffer should send events instantly or after the buffer has reached
     * it's limit. By default, this is set to BufferOption Default.
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    public void setBufferOption(BufferOption option) {
        this.option = option;
    }

    /**
     * @return the Emitters request method
     */
    public HttpMethod getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * @return the buffer option selected for the emitter
     */
    public BufferOption getBufferOption() {
        return this.option;
    }

    /**
     * @return the request callback method
     */
    public RequestCallback getRequestCallback() {
        return this.requestCallback;
    }
}
