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

import android.content.Context;
import android.net.Uri;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.ReadyRequest;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestSecurity;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
public abstract class Emitter {

    private final String TAG = Emitter.class.getSimpleName();
    protected final OkHttpClient client = new OkHttpClient();
    protected final MediaType JSON = MediaType.parse(TrackerConstants.POST_CONTENT_TYPE);
    protected Context context;
    protected Uri.Builder uriBuilder;
    protected RequestCallback requestCallback;
    protected HttpMethod httpMethod;
    protected BufferOption bufferOption;
    protected RequestSecurity requestSecurity;
    protected String uri;
    protected int emitterTick;
    protected int emptyLimit;
    protected int sendLimit;
    protected long byteLimitGet;
    protected long byteLimitPost;

    protected AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Builder for the Emitter.
     */
    @SuppressWarnings("unchecked")
    public static class EmitterBuilder {

        protected static Class<? extends Emitter> defaultEmitterClass;

        /* Prefer Rx, then Classic versions of our emitters */
        static {
            try {
                defaultEmitterClass = (Class<? extends Emitter>)Class.forName("com.snowplowanalytics.snowplow.tracker.rx.Emitter");
            } catch (ClassNotFoundException e) {
                try {
                    defaultEmitterClass = (Class<? extends Emitter>)Class.forName("com.snowplowanalytics.snowplow.tracker.classic.Emitter");
                } catch (ClassNotFoundException e1) {
                    defaultEmitterClass = null;
                }
            }
        }

        private Class<? extends Emitter> emitterClass;
        protected final String uri; // Required
        protected final Context context; // Required
        protected RequestCallback requestCallback = null; // Optional
        protected HttpMethod httpMethod = HttpMethod.POST; // Optional
        protected BufferOption bufferOption = BufferOption.DefaultGroup; // Optional
        protected RequestSecurity requestSecurity = RequestSecurity.HTTP; // Optional
        protected int emitterTick = 5; // Optional
        protected int sendLimit = 250; // Optional
        protected int emptyLimit = 5; // Optional
        protected long byteLimitGet = 40000; // Optional
        protected long byteLimitPost = 40000; // Optional

        /**
         * @param uri The uri of the collector
         * @param context the android context
         */
        public EmitterBuilder(String uri, Context context) {
            this(uri, context, defaultEmitterClass);
        }

        /**
         *
         * @param uri The collector uri to send events to
         * @param context The android context
         * @param emitterClass The emitter class to use
         */
        public EmitterBuilder(String uri, Context context, Class<? extends Emitter> emitterClass) {
            this.uri = uri;
            this.context = context;
            this.emitterClass = emitterClass;
        }

        /**
         * @param httpMethod The method by which requests are emitted
         * @return itself
         */
        public EmitterBuilder method(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        /**
         * @param option the buffer option for the emitter
         * @return itself
         */
        public EmitterBuilder option(BufferOption option) {
            this.bufferOption = option;
            return this;
        }

        /**
         * @param requestSecurity the security chosen for requests
         * @return itself
         */
        public EmitterBuilder security(RequestSecurity requestSecurity) {
            this.requestSecurity = requestSecurity;
            return this;
        }

        /**
         * @param requestCallback Request callback function
         * @return itself
         */
        public EmitterBuilder callback(RequestCallback requestCallback) {
            this.requestCallback = requestCallback;
            return this;
        }

        /**
         * @param emitterTick The tick count between emitter attempts
         * @return itself
         */
        public EmitterBuilder tick(int emitterTick) {
            this.emitterTick = emitterTick;
            return this;
        }

        /**
         * @param sendLimit The maximum amount of events to grab for an emit attempt
         * @return itself
         */
        public EmitterBuilder sendLimit(int sendLimit) {
            this.sendLimit = sendLimit;
            return this;
        }

        /**
         * @param emptyLimit The amount of emitter ticks that are performed before we shut down
         *                   due to the database being empty.
         * @return itself
         */
        public EmitterBuilder emptyLimit(int emptyLimit) {
            this.emptyLimit = emptyLimit;
            return this;
        }

        /**
         * @param byteLimitGet The maximum amount of bytes allowed to be sent in a payload
         *                     in a GET request.
         * @return itself
         */
        public EmitterBuilder byteLimitGet(long byteLimitGet) {
            this.byteLimitGet = byteLimitGet;
            return this;
        }

        /**
         * @param byteLimitPost The maximum amount of bytes allowed to be sent in a payload
         *                      in a POST request.
         * @return itself
         */
        public EmitterBuilder byteLimitPost(long byteLimitPost) {
            this.byteLimitPost = byteLimitPost;
            return this;
        }

        /**
         * Builds a new Emitter object
         *
         * @return a new Emitter object
         */
        public Emitter build() {
            if (emitterClass == null) {
                throw new IllegalStateException("No emitter class found or defined");
            }

            String err = "Can’t create emitter";
            try {
                Constructor<? extends Emitter> c =  emitterClass.getDeclaredConstructor(EmitterBuilder.class);
                return c.newInstance(this);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(err, e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(err, e);
            } catch (InstantiationException e) {
                throw new IllegalStateException(err, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(err, e);
            }
        }
    }

    /**
     * Creates an emitter object
     *
     * @param builder The builder that constructs an emitter
     */
    public Emitter(EmitterBuilder builder) {
        this.httpMethod = builder.httpMethod;
        this.requestCallback = builder.requestCallback;
        this.context = builder.context;
        this.bufferOption = builder.bufferOption;
        this.requestSecurity = builder.requestSecurity;
        this.emitterTick = builder.emitterTick;
        this.emptyLimit = builder.emptyLimit;
        this.sendLimit = builder.sendLimit;
        this.byteLimitGet = builder.byteLimitGet;
        this.byteLimitPost = builder.byteLimitPost;
        this.uri = builder.uri;
        buildEmitterUri();

        client.setConnectTimeout(15, TimeUnit.SECONDS); // connect timeout
        client.setReadTimeout(15, TimeUnit.SECONDS);    // socket timeout

        Logger.v(TAG, "Emitter created successfully!");
    }

    /**
     * Sets the Emitter URI
     */
    private void buildEmitterUri() {
        if (this.requestSecurity == RequestSecurity.HTTP) {
            this.uriBuilder = Uri.parse("http://" + this.uri).buildUpon();
        }
        else {
            this.uriBuilder = Uri.parse("https://" + this.uri).buildUpon();
        }
        if (this.httpMethod == HttpMethod.GET) {
            uriBuilder.appendPath("i");
        }
        else {
            uriBuilder.appendEncodedPath(TrackerConstants.PROTOCOL_VENDOR + "/" +
                    TrackerConstants.PROTOCOL_VERSION);
        }
    }

    /**
     * @param payload the payload to be added to
     *                the EventStore
     */
    public abstract void add(Payload payload);

    /**
     * Shuts the emitter down!
     */
    public abstract void shutdown();

    /**
     * Sends everything in the database to the endpoint.
     */
    public abstract void flush();

    /**
     * Performs a synchronous sending of a list of
     * ReadyRequests.
     *
     * @param requests the requests to send
     * @return the request results.
     */
    protected LinkedList<RequestResult> performSyncEmit(LinkedList<ReadyRequest> requests) {
        LinkedList<RequestResult> results = new LinkedList<>();
        for (ReadyRequest request : requests) {
            int code = requestSender(request.getRequest());
            if (request.isOversize()) {
                results.add(new RequestResult(true, request.getEventIds()));
            } else {
                results.add(new RequestResult(isSuccessfulSend(code), request.getEventIds()));
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
    protected int requestSender(Request request) {
        try {
            Logger.d(TAG, "Sending request: %s", request);
            return client.newCall(request).execute().code();
        } catch (IOException e) {
            Logger.e(TAG, "Request sending failed: %s", e.toString());
            return -1;
        }
    }

    /**
     * Returns a list of ReadyRequests which can
     * all be sent regardless of if it is GET or POST.
     * - Checks if the event is over-sized.
     * - Stores all of the relevant event ids.
     *
     * @param events a list of EmittableEvents pulled
     *               from the database.
     * @return a list of ready to send requests
     */
    protected LinkedList<ReadyRequest> buildRequests(EmittableEvents events) {

        int payloadCount = events.getEvents().size();
        LinkedList<Long> eventIds = events.getEventIds();
        LinkedList<ReadyRequest> requests = new LinkedList<>();

        if (httpMethod == HttpMethod.GET) {
            for (int i = 0; i < payloadCount; i++) {

                // Get the eventId for this request
                LinkedList<Long> reqEventId = new LinkedList<>();
                reqEventId.add(eventIds.get(i));

                // Build and store the request
                Payload payload = events.getEvents().get(i);
                addStmToEvent(payload, "");
                boolean oversize = payload.getByteSize() > byteLimitGet;
                Request request = requestBuilderGet(payload);
                requests.add(new ReadyRequest(oversize, request, reqEventId));
            }
        } else {
            for (int i = 0; i < payloadCount; i += bufferOption.getCode()) {

                // Get STM Timestamp
                String timestamp = Util.getTimestamp();

                // Collections
                LinkedList<Long> reqEventIds = new LinkedList<>();
                ArrayList<Map> postPayloadMaps = new ArrayList<>();
                long totalByteSize = 0;

                for (int j = i; j < (i + bufferOption.getCode()) && j < payloadCount; j++) {
                    Payload payload = events.getEvents().get(j);
                    addStmToEvent(payload, timestamp);
                    long payloadByteSize = payload.getByteSize();

                    if (payloadByteSize > byteLimitPost) {
                        ArrayList<Map> singlePayloadMap = new ArrayList<>();
                        LinkedList<Long> reqEventId = new LinkedList<>();

                        // Build and store the request
                        addStmToEvent(payload, "");
                        singlePayloadMap.add(payload.getMap());
                        reqEventId.add(eventIds.get(j));
                        Request request = requestBuilderPost(singlePayloadMap);
                        requests.add(new ReadyRequest(true, request, reqEventId));
                    }
                    else if (totalByteSize + payloadByteSize > byteLimitPost) {
                        Request request = requestBuilderPost(postPayloadMaps);
                        requests.add(new ReadyRequest(false, request, reqEventIds));

                        // Clear collections and build a new POST
                        postPayloadMaps = new ArrayList<>();
                        reqEventIds = new LinkedList<>();

                        // Build and store the request
                        timestamp = Util.getTimestamp();
                        addStmToEvent(payload, timestamp);
                        postPayloadMaps.add(payload.getMap());
                        reqEventIds.add(eventIds.get(j));
                        totalByteSize = payloadByteSize;
                    }
                    else {
                        totalByteSize += payloadByteSize;
                        postPayloadMaps.add(payload.getMap());
                        reqEventIds.add(eventIds.get(j));
                    }
                }

                // Check if all payloads have been processed
                if (!postPayloadMaps.isEmpty()) {
                    Request request = requestBuilderPost(postPayloadMaps);
                    requests.add(new ReadyRequest(false, request, reqEventIds));
                }
            }
        }
        return requests;
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

        // Clear the previous query
        uriBuilder.clearQuery();

        // Build the request query
        HashMap hashMap = (HashMap) payload.getMap();

        for (String key : (Iterable<String>) hashMap.keySet()) {
            String value = (String) hashMap.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }

        // Build the request
        String reqUrl = uriBuilder.build().toString();
        return new Request.Builder()
                .url(reqUrl)
                .get()
                .build();
    }

    /**
     * Builds an OkHttp POST request which is ready
     * to be executed.
     * @param payload The payload to be sent in the
     *                request.
     * @return an OkHttp request object
     */
    private Request requestBuilderPost(ArrayList<Map> payload) {
        SelfDescribingJson postPayload =
                new SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, payload);
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, postPayload.toString());
        return new Request.Builder()
                .url(reqUrl)
                .post(reqBody)
                .build();
    }

    /**
     * Adds the Sending Time (stm) field
     * to each event payload.
     *
     * @param payload The payload to append the field to
     * @param timestamp An optional timestamp String
     */
    private void addStmToEvent(Payload payload, String timestamp) {
        payload.add(Parameters.SENT_TIMESTAMP,
                timestamp.equals("") ? Util.getTimestamp() : timestamp);
    }

    // Setters, Getters and Checkers

    /**
     * @return the emitter event store
     */
    public abstract EventStore getEventStore();

    /**
     * @return the emitter status
     */
    public abstract boolean getEmitterStatus();

    /**
     * Returns truth on if the request
     * was sent successfully.
     *
     * @param code the response code
     * @return the truth as to the success
     */
    protected boolean isSuccessfulSend(int code) {
        return code >= 200 && code < 300;
    }

    /**
     * Sets whether the buffer should send events instantly or after the buffer has reached
     * it's limit. By default, this is set to BufferOption Default.
     *
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    public void setBufferOption(BufferOption option) {
        if (!isRunning.get()) {
            this.bufferOption = option;
        }
    }

    /**
     * Sets the HttpMethod for the Emitter
     *
     * @param method the HttpMethod
     */
    public void setHttpMethod(HttpMethod method) {
        if (!isRunning.get()) {
            this.httpMethod = method;
            buildEmitterUri();
        }
    }

    /**
     * Sets the RequestSecurity for the Emitter
     *
     * @param security the RequestSecurity
     */
    public void setRequestSecurity(RequestSecurity security) {
        if (!isRunning.get()) {
            this.requestSecurity = security;
            buildEmitterUri();
        }
    }

    /**
     * Updates the URI for the Emitter
     *
     * @param uri new Emitter URI
     */
    public void setEmitterUri(String uri) {
        if (!isRunning.get()) {
            this.uri = uri;
            buildEmitterUri();
        }
    }

    /**
     * @return the emitter uri
     */
    public String getEmitterUri() {
        return this.uriBuilder.clearQuery().build().toString();
    }

    /**
     * @return the request callback method
     */
    public RequestCallback getRequestCallback() {
        return this.requestCallback;
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
        return this.bufferOption;
    }

    /**
     * @return the request security selected for the emitter
     */
    public RequestSecurity getRequestSecurity() {
        return this.requestSecurity;
    }

    /**
     * @return the emitter tick
     */
    public int getEmitterTick() {
        return this.emitterTick;
    }

    /**
     * @return the amount of times the event store can be empty
     *         before it is shutdown.
     */
    public int getEmptyLimit() {
        return this.emptyLimit;
    }

    /**
     * @return the emitter send limit
     */
    public int getSendLimit() {
        return this.sendLimit;
    }

    /**
     * @return the GET byte limit
     */
    public long getByteLimitGet() {
        return this.byteLimitGet;
    }

    /**
     * @return the POST byte limit
     */
    public long getByteLimitPost() {
        return this.byteLimitPost;
    }
}
