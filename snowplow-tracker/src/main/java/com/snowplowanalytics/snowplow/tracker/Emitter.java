/*
 * Copyright (c) 2015-2017 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.tracker.emitter.TLSArguments;
import com.snowplowanalytics.snowplow.tracker.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
public class Emitter {

    private static final int POST_WRAPPER_BYTES = 88; // "schema":"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3","data":[]
    private static final int POST_STM_BYTES = 22;     // "stm":"1443452851000",

    private final String TAG = Emitter.class.getSimpleName();
    private final OkHttpClient client;
    private final MediaType JSON = MediaType.parse(TrackerConstants.POST_CONTENT_TYPE);

    private Context context;
    private Uri.Builder uriBuilder;
    private RequestCallback requestCallback;
    private HttpMethod httpMethod;
    private BufferOption bufferOption;
    private RequestSecurity requestSecurity;
    private EnumSet<TLSVersion> tlsVersions;
    private String uri;
    private int emitterTick;
    private int emptyLimit;
    private int sendLimit;
    private long byteLimitGet;
    private long byteLimitPost;
    private int emitTimeout;
    private TimeUnit timeUnit;

    private EventStore eventStore;
    private Future eventStoreFuture;
    private int emptyCount;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Builder for the Emitter.
     */
    public static class EmitterBuilder {

        final String uri; // Required
        final Context context; // Required
        RequestCallback requestCallback = null; // Optional
        HttpMethod httpMethod = HttpMethod.POST; // Optional
        BufferOption bufferOption = BufferOption.DefaultGroup; // Optional
        RequestSecurity requestSecurity = RequestSecurity.HTTP; // Optional
        EnumSet<TLSVersion> tlsVersions = EnumSet.of(TLSVersion.TLSv1_2); // Optional
        int emitterTick = 5; // Optional
        int sendLimit = 250; // Optional
        int emptyLimit = 5; // Optional
        long byteLimitGet = 40000; // Optional
        long byteLimitPost = 40000; // Optional
        private int emitTimeout = 5; // Optional
        TimeUnit timeUnit = TimeUnit.SECONDS;
        OkHttpClient client = null; //Optional

        /**
         * @param uri The uri of the collector
         * @param context the android context
         */
        public EmitterBuilder(String uri, Context context) {
            this.uri = uri;
            this.context = context;
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
         * @param version the TLS version allowed for requests
         * @return itself
         */
        public EmitterBuilder tls(TLSVersion version) {
            this.tlsVersions = EnumSet.of(version);
            return this;
        }

        /**
         * @param versions the TLS versions allowed for requests
         * @return itself
         */
        public EmitterBuilder tls(EnumSet<TLSVersion> versions) {
            this.tlsVersions = versions;
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
         * @param emitTimeout The maximum timeout for emitting events. If emit time exceeds this value
         *                    TimeOutException will be thrown
         * @return itself
         */
        public EmitterBuilder emitTimeout(int emitTimeout){
            this.emitTimeout = emitTimeout;
            return this;
        }

        /**
         * @param timeUnit a valid TimeUnit
         * @return itself
         */
        public EmitterBuilder timeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * @param client An OkHttp client that will be used in the emitter, you can provide your
         *               own if you want to share your Singleton client's interceptors, connection pool etc..
         *               ,otherwise a new one is created.
         * @return itself
         */
        public EmitterBuilder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Creates a new Emitter
         *
         * @return a new Emitter object
         */
        public Emitter build() {
            return new Emitter(this);
        }
    }

    /**
     * Creates an emitter object
     *
     * @param builder The builder that constructs an emitter
     */
    private Emitter(EmitterBuilder builder) {
        this.httpMethod = builder.httpMethod;
        this.requestCallback = builder.requestCallback;
        this.context = builder.context;
        this.bufferOption = builder.bufferOption;
        this.requestSecurity = builder.requestSecurity;
        this.tlsVersions = builder.tlsVersions;
        this.emitterTick = builder.emitterTick;
        this.emptyLimit = builder.emptyLimit;
        this.sendLimit = builder.sendLimit;
        this.byteLimitGet = builder.byteLimitGet;
        this.byteLimitPost = builder.byteLimitPost;
        this.emitTimeout = builder.emitTimeout;
        this.uri = builder.uri;
        this.timeUnit = builder.timeUnit;
        this.eventStore = null;
        this.eventStoreFuture = Executor.futureCallable(new Callable<Void>() {
            @Override
            public Void call() {
                eventStore = new EventStore(context, sendLimit);
                return null;
            }
        });

        TLSArguments tlsArguments = new TLSArguments(this.tlsVersions);
        buildEmitterUri();

        final OkHttpClient.Builder clientBuilder;
        if (builder.client == null) {
            clientBuilder = new OkHttpClient.Builder();
        }
        else {
            clientBuilder = builder.client.newBuilder();
        }

        client = clientBuilder.sslSocketFactory(tlsArguments.getSslSocketFactory(),
                                                tlsArguments.getTrustManager())
                              .connectTimeout(15, TimeUnit.SECONDS)
                              .readTimeout(15, TimeUnit.SECONDS)
                              .build();

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

    // --- Controls

    /**
     * Adds a payload to the EventStore and
     * then attempts to start the emitter
     * if it is not currently running.
     *
     * @param payload the event payload
     *                to be added.
     */
    public void add(final Payload payload) {
        if (eventStore != null) {
            Executor.execute(new Runnable() {
                @Override
                public void run() {
                    eventStore.add(payload);
                    if (isRunning.compareAndSet(false, true)) {
                        attemptEmit();
                    }
                }
            });
        } else {
            Logger.d(TAG, "Event store not instantiated.");
        }
    }

    /**
     * Attempts to start the emitter if it
     * is not currently running.
     */
    public void flush() {
        Executor.execute(new Runnable() {
            public void run() {
                if (isRunning.compareAndSet(false, true)) {
                    attemptEmit();
                }
            }
        });
    }

    /**
     * Resets the `isRunning` truth to false and shutdown.
     */
    public void shutdown() {
        Logger.d(TAG, "Shutting down emitter.");
        isRunning.compareAndSet(true, false);
        Executor.shutdown();
    }

    /**
     * Attempts to send events in the database to
     * a collector.
     *
     * - If the emitter is not online it will not send
     * - If the emitter is online but there are no events:
     *   + Increment empty counter until emptyLimit reached
     *   + Incurs a backoff period between empty counters
     * - If the emitter is online and we have events:
     *   + Pulls allowed amount of events from database and
     *     attempts to send.
     *   + If there are failures resets running state
     *   + Otherwise will attempt to emit again
     */
    @SuppressWarnings("all")
    private void attemptEmit() {
        if (eventStore != null) {
            if (Util.isOnline(this.context)) {
                if (eventStore.getSize() > 0) {
                    emptyCount = 0;

                    EmittableEvents events = eventStore.getEmittableEvents();
                    LinkedList<ReadyRequest> requests = buildRequests(events);
                    LinkedList<RequestResult> results = performAsyncEmit(requests);

                    events = null;
                    requests = null;

                    Logger.v(TAG, "Processing emitter results.");

                    int successCount = 0;
                    int failureCount = 0;
                    LinkedList<Long> removableEvents = new LinkedList<>();

                    for (RequestResult res : results) {
                        if (res.getSuccess()) {
                            for (final Long eventId : res.getEventIds()) {
                                removableEvents.add(eventId);
                            }
                            successCount += res.getEventIds().size();
                        } else {
                            failureCount += res.getEventIds().size();
                            Logger.e(TAG, "Request sending failed but we will retry later.");
                        }
                    }
                    eventStore.removeEvents(removableEvents);

                    results = null;
                    removableEvents = null;

                    Logger.d(TAG, "Success Count: %s", successCount);
                    Logger.d(TAG, "Failure Count: %s", failureCount);

                    if (requestCallback != null) {
                        if (failureCount != 0) {
                            requestCallback.onFailure(successCount, failureCount);
                        } else {
                            requestCallback.onSuccess(successCount);
                        }
                    }

                    if (failureCount > 0 && successCount == 0) {
                        if (Util.isOnline(this.context)) {
                            Logger.e(TAG, "Ensure collector path is valid: %s", getEmitterUri());
                        }
                        Logger.e(TAG, "Emitter loop stopping: failures.");
                        isRunning.compareAndSet(true, false);
                    } else {
                        attemptEmit();
                    }
                } else {
                    if (emptyCount >= this.emptyLimit) {
                        Logger.e(TAG, "Emitter loop stopping: empty limit reached.");
                        isRunning.compareAndSet(true, false);
                    } else {
                        emptyCount++;
                        Logger.e(TAG, "Emitter database empty: " + emptyCount);
                        try {
                            this.timeUnit.sleep(this.emitterTick);
                        } catch (InterruptedException e) {
                            Logger.e(TAG, "Emitter thread sleep interrupted: " + e.toString());
                        }
                        attemptEmit();
                    }
                }
            } else {
                Logger.e(TAG, "Emitter loop stopping: emitter offline.");
                isRunning.compareAndSet(true, false);
            }
        } else {
            Logger.d(TAG, "Event store not instantiated.");
            isRunning.compareAndSet(true, false);
        }
    }

    /**
     * Asynchronously sends all of the
     * ReadyRequests in the List to the
     * defined endpoint.
     *
     * @param requests the requests to be
     *                 sent
     * @return the results of each request
     */
    @SuppressWarnings("all")
    protected LinkedList<RequestResult> performAsyncEmit(LinkedList<ReadyRequest> requests) {
        LinkedList<RequestResult> results = new LinkedList<>();
        LinkedList<Future> futures = new LinkedList<>();

        // Start all requests in the ThreadPool
        for (ReadyRequest request : requests) {
            futures.add(Executor.futureCallable(getRequestCallable(request.getRequest())));
        }

        Logger.d(TAG, "Request Futures: %s", futures.size());

        // Get results of futures
        // - Wait up to emitTimeout seconds for the request
        for (int i = 0; i < futures.size(); i++) {
            int code = -1;

            try {
                code = (int) futures.get(i).get(emitTimeout, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Logger.e(TAG, "Request Future was interrupted: %s", ie.getMessage());
            } catch (ExecutionException ee) {
                Logger.e(TAG, "Request Future failed: %s", ee.getMessage());
            } catch (TimeoutException te) {
                Logger.e(TAG, "Request Future had a timeout: %s", te.getMessage());
            }

            if (requests.get(i).isOversize()) {
                results.add(new RequestResult(true, requests.get(i).getEventIds()));
            } else {
                results.add(new RequestResult(isSuccessfulSend(code), requests.get(i).getEventIds()));
            }
        }

        requests = null;
        futures = null;

        return results;
    }

    /**
     * Returns a Callable Request Send
     *
     * @param request the request to be
     *                sent
     * @return the new Callable object
     */
    private Callable<Integer> getRequestCallable(final Request request) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return requestSender(request);
            }
        };
    }

    // --- Send Helpers

    /**
     * The function responsible for actually sending
     * the request to the collector.
     *
     * @param request The request to be sent
     * @return a RequestResult
     */
    private int requestSender(Request request) {
        try {
            Logger.v(TAG, "Sending request: %s", request);

            Response resp = client.newCall(request).execute();
            int code = resp.code();
            resp.body().close();

            return code;
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
                boolean oversize = payload.getByteSize() + POST_STM_BYTES > byteLimitGet;
                Request request = requestBuilderGet(payload);
                requests.add(new ReadyRequest(oversize, request, reqEventId));
            }
        } else {
            for (int i = 0; i < payloadCount; i += bufferOption.getCode()) {

                LinkedList<Long> reqEventIds = new LinkedList<>();
                ArrayList<Payload> postPayloadMaps = new ArrayList<>();
                long totalByteSize = 0;

                for (int j = i; j < (i + bufferOption.getCode()) && j < payloadCount; j++) {
                    Payload payload = events.getEvents().get(j);
                    long payloadByteSize = payload.getByteSize() + POST_STM_BYTES;

                    if ((payloadByteSize + POST_WRAPPER_BYTES) > byteLimitPost) {
                        ArrayList<Payload> singlePayloadMap = new ArrayList<>();
                        LinkedList<Long> reqEventId = new LinkedList<>();

                        // Build and store the request
                        singlePayloadMap.add(payload);
                        reqEventId.add(eventIds.get(j));
                        Request request = requestBuilderPost(singlePayloadMap);
                        requests.add(new ReadyRequest(true, request, reqEventId));
                    }
                    else if ((totalByteSize + payloadByteSize + POST_WRAPPER_BYTES +
                            (postPayloadMaps.size() -1)) > byteLimitPost) {
                        Request request = requestBuilderPost(postPayloadMaps);
                        requests.add(new ReadyRequest(false, request, reqEventIds));

                        // Clear collections and build a new POST
                        postPayloadMaps = new ArrayList<>();
                        reqEventIds = new LinkedList<>();

                        // Build and store the request
                        postPayloadMaps.add(payload);
                        reqEventIds.add(eventIds.get(j));
                        totalByteSize = payloadByteSize;
                    }
                    else {
                        totalByteSize += payloadByteSize;
                        postPayloadMaps.add(payload);
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
        addStmToEvent(payload, "");

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
     * @param payloads The payloads to be sent in the
     *                 request.
     * @return an OkHttp request object
     */
    private Request requestBuilderPost(ArrayList<Payload> payloads) {
        ArrayList<Map> finalPayloads = new ArrayList<>();
        String stm = Util.getTimestamp();
        for (Payload payload : payloads) {
            addStmToEvent(payload, stm);
            finalPayloads.add(payload.getMap());
        }

        SelfDescribingJson postPayload =
                new SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, finalPayloads);
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
     * @return the emitter event store object
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter status
     */
    public boolean getEmitterStatus() {
        return isRunning.get();
    }

    /**
     * Returns truth on if the request
     * was sent successfully.
     *
     * @param code the response code
     * @return the truth as to the success
     */
    private boolean isSuccessfulSend(int code) {
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
     * Waits for the event store to load.
     * @return boolean whether event store has successfully loaded
     */
    public boolean waitForEventStore() {
        Future eventStoreFuture = this.getEventStoreFuture();
        try {
            eventStoreFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Logger.e(TAG, "Event store loading was interrupted: %s", ie.getMessage());
        } catch (ExecutionException ee) {
            Logger.e(TAG, "Event store loading failed: %s", ee.getMessage());
        } catch (TimeoutException te) {
            Logger.e(TAG, "Event store loading timedout: %s", te.getMessage());
        }
        return eventStoreFuture.isDone();
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
     * @return the TLS versions accepted for the emitter
     */
    public EnumSet<TLSVersion> getTlsVersions() {
        return this.tlsVersions;
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

    /**
     * @return future for event store loading
     */
    public Future getEventStoreFuture() { return this.eventStoreFuture; }
}
