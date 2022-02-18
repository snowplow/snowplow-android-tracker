/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.internal.emitter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.OkHttpNetworkConnection;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.internal.emitter.storage.SQLiteEventStore;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.network.RequestResult;
import com.snowplowanalytics.snowplow.internal.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;

import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.network.HttpMethod.POST;

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
public class Emitter {
    private final String TAG = Emitter.class.getSimpleName();

    private static final int POST_WRAPPER_BYTES = 88; // "schema":"iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-3","data":[]

    private Context context;
    private RequestCallback requestCallback;
    private HttpMethod httpMethod;
    private BufferOption bufferOption;
    private Protocol requestSecurity;
    private EnumSet<TLSVersion> tlsVersions;
    private String uri;
    private String namespace;
    private int emitterTick;
    private int emptyLimit;
    private int sendLimit;
    private long byteLimitGet;
    private long byteLimitPost;
    private int emitTimeout;
    private TimeUnit timeUnit;
    private String customPostPath;
    private OkHttpClient client;

    private boolean isCustomNetworkConnection;
    private NetworkConnection networkConnection;
    private EventStore eventStore;
    private int emptyCount;

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isEmittingPaused = new AtomicBoolean(false);

    /**
     * Builder for the Emitter.
     */
    public static class EmitterBuilder {
        @Nullable RequestCallback requestCallback = null; // Optional
        @NonNull HttpMethod httpMethod = POST; // Optional
        @NonNull BufferOption bufferOption = BufferOption.DefaultGroup; // Optional
        @NonNull Protocol requestSecurity = Protocol.HTTP; // Optional
        @NonNull EnumSet<TLSVersion> tlsVersions = EnumSet.of(TLSVersion.TLSv1_2); // Optional
        int emitterTick = 5; // Optional
        int sendLimit = 250; // Optional
        int emptyLimit = 5; // Optional
        long byteLimitGet = 40000; // Optional
        long byteLimitPost = 40000; // Optional
        private int emitTimeout = 5; // Optional
        int threadPoolSize = 2; // Optional
        @NonNull TimeUnit timeUnit = TimeUnit.SECONDS;
        @Nullable OkHttpClient client = null; //Optional
        @Nullable String customPostPath = null; //Optional
        @Nullable NetworkConnection networkConnection = null; // Optional
        @Nullable EventStore eventStore = null; // Optional

        /**
         * @param networkConnection The component in charge for sending events to the collector.
         * @return itself
         */
        @NonNull
        public EmitterBuilder networkConnection(@Nullable NetworkConnection networkConnection) {
            this.networkConnection = networkConnection;
            return this;
        }

        /**
         * @param eventStore The component in charge for persisting events before sending.
         * @return itself
         */
        @NonNull
        public EmitterBuilder eventStore(@Nullable EventStore eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        /**
         * @param httpMethod The method by which requests are emitted
         * @return itself
         */
        @NonNull
        public EmitterBuilder method(@NonNull HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        /**
         * @param option the buffer option for the emitter
         * @return itself
         */
        @NonNull
        public EmitterBuilder option(@NonNull BufferOption option) {
            this.bufferOption = option;
            return this;
        }

        /**
         * @param protocol the security chosen for requests
         * @return itself
         */
        @NonNull
        public EmitterBuilder security(@NonNull Protocol protocol) {
            this.requestSecurity = protocol;
            return this;
        }

        /**
         * @param version the TLS version allowed for requests
         * @return itself
         */
        @NonNull
        public EmitterBuilder tls(@NonNull TLSVersion version) {
            this.tlsVersions = EnumSet.of(version);
            return this;
        }

        /**
         * @param versions the TLS versions allowed for requests
         * @return itself
         */
        @NonNull
        public EmitterBuilder tls(@NonNull EnumSet<TLSVersion> versions) {
            this.tlsVersions = versions;
            return this;
        }

        /**
         * @param requestCallback Request callback function
         * @return itself
         */
        @NonNull
        public EmitterBuilder callback(@Nullable RequestCallback requestCallback) {
            this.requestCallback = requestCallback;
            return this;
        }

        /**
         * @param emitterTick The tick count between emitter attempts
         * @return itself
         */
        @NonNull
        public EmitterBuilder tick(int emitterTick) {
            this.emitterTick = emitterTick;
            return this;
        }

        /**
         * @param sendLimit The maximum amount of events to grab for an emit attempt
         * @return itself
         */
        @NonNull
        public EmitterBuilder sendLimit(int sendLimit) {
            this.sendLimit = sendLimit;
            return this;
        }

        /**
         * @param emptyLimit The amount of emitter ticks that are performed before we shut down
         *                   due to the database being empty.
         * @return itself
         */
        @NonNull
        public EmitterBuilder emptyLimit(int emptyLimit) {
            this.emptyLimit = emptyLimit;
            return this;
        }

        /**
         * @param byteLimitGet The maximum amount of bytes allowed to be sent in a payload
         *                     in a GET request.
         * @return itself
         */
        @NonNull
        public EmitterBuilder byteLimitGet(long byteLimitGet) {
            this.byteLimitGet = byteLimitGet;
            return this;
        }

        /**
         * @param byteLimitPost The maximum amount of bytes allowed to be sent in a payload
         *                      in a POST request.
         * @return itself
         */
        @NonNull
        public EmitterBuilder byteLimitPost(long byteLimitPost) {
            this.byteLimitPost = byteLimitPost;
            return this;
        }

        /**
         * @param emitTimeout The maximum timeout for emitting events. If emit time exceeds this value
         *                    TimeOutException will be thrown
         * @return itself
         */
        @NonNull
        public EmitterBuilder emitTimeout(int emitTimeout) {
            this.emitTimeout = emitTimeout;
            return this;
        }

        /**
         * @param timeUnit a valid TimeUnit
         * @return itself
         */
        @NonNull
        public EmitterBuilder timeUnit(@NonNull TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * @param client An OkHttp client that will be used in the emitter, you can provide your
         *               own if you want to share your Singleton client's interceptors, connection pool etc..
         *               ,otherwise a new one is created.
         * @return itself
         */
        @NonNull
        public EmitterBuilder client(@Nullable OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * @param customPostPath A custom path that is used on the endpoint to send requests.
         * @return itself
         */
        @NonNull
        public EmitterBuilder customPostPath(@Nullable String customPostPath) {
            this.customPostPath = customPostPath;
            return this;
        }

        /**
         * @param threadPoolSize The number of threads available for the tracker's operations.
         * @return itself
         */
        @NonNull
        public EmitterBuilder threadPoolSize(int threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }
    }

    /**
     * Creates an emitter object
     */
    public Emitter(@NonNull Context context, @NonNull String collectorUri, @Nullable EmitterBuilder builder) {
        this.context = context;
        if (builder == null) {
            builder = new EmitterBuilder();
        }
        this.requestCallback = builder.requestCallback;
        this.bufferOption = builder.bufferOption;
        this.requestSecurity = builder.requestSecurity;
        this.tlsVersions = builder.tlsVersions;
        this.emitterTick = builder.emitterTick;
        this.emptyLimit = builder.emptyLimit;
        this.sendLimit = builder.sendLimit;
        this.byteLimitGet = builder.byteLimitGet;
        this.byteLimitPost = builder.byteLimitPost;
        this.emitTimeout = builder.emitTimeout;
        this.timeUnit = builder.timeUnit;
        this.client = builder.client;
        this.eventStore = builder.eventStore;

        this.uri = collectorUri;
        this.httpMethod = builder.httpMethod;
        this.customPostPath = builder.customPostPath;
        if (builder.networkConnection == null) {
            isCustomNetworkConnection = false;
            String endpoint = collectorUri;
            if (!endpoint.startsWith("http")) {
                String protocol = builder.requestSecurity == Protocol.HTTPS ? "https://" : "http://";
                endpoint = protocol + endpoint;
            }
            this.uri = endpoint;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(endpoint)
                    .method(builder.httpMethod)
                    .tls(builder.tlsVersions)
                    .emitTimeout(builder.emitTimeout)
                    .customPostPath(builder.customPostPath)
                    .client(builder.client)
                    .build();
        } else {
            isCustomNetworkConnection = true;
            this.networkConnection = builder.networkConnection;
        }

        if (builder.threadPoolSize > 2) {
            Executor.setThreadCount(builder.threadPoolSize);
        }

        Logger.v(TAG, "Emitter created successfully!");
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
    public void add(final @NonNull Payload payload) {
        Executor.execute(TAG, () -> {
            eventStore.add(payload);
            if (isRunning.compareAndSet(false, true)) {
                try {
                    attemptEmit();
                } catch (Throwable t) {
                    isRunning.set(false);
                    Logger.e(TAG, "Received error during emission process: %s", t);
                }
            }
        });
    }

    /**
     * Attempts to start the emitter if it
     * is not currently running.
     */
    public void flush() {
        Executor.execute(TAG, () -> {
            if (isRunning.compareAndSet(false, true)) {
                try {
                    attemptEmit();
                } catch (Throwable t) {
                    isRunning.set(false);
                    Logger.e(TAG, "Received error during emission process: %s", t);
                }
            }
        });
    }

    /**
     * Pause emitting events.
     */
    public void pauseEmit() {
        isEmittingPaused.set(true);
    }

    /**
     * Resume emitting events and attempt to emit any queued events.
     */
    public void resumeEmit() {
        if (isEmittingPaused.compareAndSet(true, false)) {
            flush();
        }
    }

    /**
     * Resets the `isRunning` truth to false and shutdown.
     */
    public void shutdown() {
        shutdown(0);
    }

    /**
     * Resets the `isRunning` truth to false and shutdown.
     *
     * @param timeout the amount of seconds to wait for the termination of the running threads.
     */
    public boolean shutdown(long timeout) {
        Logger.d(TAG, "Shutting down emitter.");
        isRunning.compareAndSet(true, false);
        ExecutorService es = Executor.shutdown();
        if (es == null || timeout <= 0) {
            return true;
        }
        try {
            boolean isTerminated = es.awaitTermination(timeout, TimeUnit.SECONDS);
            Logger.d(TAG, "Executor is terminated: " + isTerminated);
            return isTerminated;
        } catch (InterruptedException e) {
            Logger.e(TAG, "Executor termination is interrupted: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to send events in the database to
     * a collector.
     *
     * - If the emitter is paused, it will not send
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
        if (isEmittingPaused.get()) {
            Logger.d(TAG, "Emitter paused.");
            isRunning.compareAndSet(true, false);
            return;
        }
        if (!Util.isOnline(this.context)) {
            Logger.d(TAG, "Emitter loop stopping: emitter offline.");
            isRunning.compareAndSet(true, false);
            return;
        }
        if (eventStore.getSize() <= 0) {
            if (emptyCount >= this.emptyLimit) {
                Logger.d(TAG, "Emitter loop stopping: empty limit reached.");
                isRunning.compareAndSet(true, false);
                return;
            }
            emptyCount++;
            Logger.e(TAG, "Emitter database empty: " + emptyCount);
            try {
                this.timeUnit.sleep(this.emitterTick);
            } catch (InterruptedException e) {
                Logger.e(TAG, "Emitter thread sleep interrupted: " + e.toString());
            }
            attemptEmit();
            return;
        }
        emptyCount = 0;

        List<EmitterEvent> events = eventStore.getEmittableEvents(sendLimit);
        List<Request> requests = buildRequests(events);
        List<RequestResult> results = networkConnection.sendRequests(requests);

        Logger.v(TAG, "Processing emitter results.");

        int successCount = 0;
        int failureCount = 0;
        List<Long> removableEvents = new ArrayList<>();

        for (RequestResult res : results) {
            if (res.getSuccess()) {
                removableEvents.addAll(res.getEventIds());
                successCount += res.getEventIds().size();
            } else {
                failureCount += res.getEventIds().size();
                Logger.e(TAG, "Request sending failed but we will retry later.");
            }
        }
        eventStore.removeEvents(removableEvents);

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
    @NonNull
    protected List<Request> buildRequests(@NonNull List<EmitterEvent> events) {
        List<Request> requests = new ArrayList<>();
        String sendingTime = Util.getTimestamp();
        HttpMethod httpMethod = networkConnection.getHttpMethod();

        if (httpMethod == GET) {
            for (EmitterEvent event : events) {
                Payload payload = event.payload;
                addSendingTimeToPayload(payload, sendingTime);
                boolean isOversize = isOversize(payload);
                Request request = new Request(payload, event.eventId, isOversize);
                requests.add(request);
            }
        } else {
            for (int i = 0; i < events.size(); i += bufferOption.getCode()) {
                List<Long> reqEventIds = new ArrayList<>();
                List<Payload> postPayloadMaps = new ArrayList<>();

                for (int j = i; j < (i + bufferOption.getCode()) && j < events.size(); j++) {
                    EmitterEvent event = events.get(j);
                    Payload payload = event.payload;
                    Long eventId = event.eventId;
                    addSendingTimeToPayload(payload, sendingTime);

                    if (isOversize(payload)) {
                        Request request = new Request(payload, eventId, true);
                        requests.add(request);

                    } else if (isOversize(payload, postPayloadMaps)) {
                        Request request = new Request(postPayloadMaps, reqEventIds);
                        requests.add(request);

                        // Clear collections and build a new POST
                        postPayloadMaps = new ArrayList<>();
                        reqEventIds = new ArrayList<>();

                        // Build and store the request
                        postPayloadMaps.add(payload);
                        reqEventIds.add(eventId);

                    } else {
                        postPayloadMaps.add(payload);
                        reqEventIds.add(eventId);
                    }
                }

                // Check if all payloads have been processed
                if (!postPayloadMaps.isEmpty()) {
                    Request request = new Request(postPayloadMaps, reqEventIds);
                    requests.add(request);
                }
            }
        }
        return requests;
    }

    /**
     * Calculate if the payload exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to send.
     * @return weather the payload exceeds the maximum size allowed.
     */
    private boolean isOversize(@NonNull Payload payload) {
        return isOversize(payload, new ArrayList<>());
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param previousPaylods already in the payload bundle.
     * @return weather the payload bundle exceeds the maximum size allowed.
     */
    private boolean isOversize(@NonNull Payload payload, @NonNull List<Payload> previousPaylods) {
        long byteLimit = networkConnection.getHttpMethod() == GET ? byteLimitGet : byteLimitPost;
        return isOversize(payload, byteLimit, previousPaylods);
    }

    /**
     * Calculate if the payload bundle exceeds the maximum amount of bytes allowed on configuration.
     * @param payload to add om the payload bundle.
     * @param byteLimit maximum amount of bytes allowed.
     * @param previousPaylods already in the payload bundle.
     * @return weather the payload bundle exceeds the maximum size allowed.
     */
    private boolean isOversize(@NonNull Payload payload, long byteLimit, @NonNull List<Payload> previousPaylods) {
        long totalByteSize = payload.getByteSize();
        for (Payload previousPayload : previousPaylods) {
            totalByteSize += previousPayload.getByteSize();
        }
        int wrapperBytes = previousPaylods.size() > 0 ? (previousPaylods.size() + POST_WRAPPER_BYTES) : 0;
        return totalByteSize + wrapperBytes > byteLimit;
    }

    // Request Builders

    /**
     * Adds the Sending Time (stm) field
     * to each event payload.
     *
     * @param payload The payload to append the field to
     * @param timestamp An optional timestamp String
     */
    private void addSendingTimeToPayload(@NonNull Payload payload, @NonNull String timestamp) {
        payload.add(Parameters.SENT_TIMESTAMP, timestamp);
    }

    // Setters, Getters and Checkers

    /**
     * @return the emitter event store object
     */
    @Nullable
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter status
     */
    public boolean getEmitterStatus() {
        return isRunning.get();
    }

    public void setNamespace(@NonNull String namespace) {
        this.namespace = namespace;
        if (eventStore == null) {
            eventStore = new SQLiteEventStore(context, namespace);
        }
    }

    /**
     * Sets whether the buffer should send events instantly or after the buffer has reached
     * it's limit. By default, this is set to BufferOption Default.
     *
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    public void setBufferOption(@NonNull BufferOption option) {
        if (!isRunning.get()) {
            this.bufferOption = option;
        }
    }

    /**
     * Sets the maximum amount of events to grab for an emit attempt.
     * @param sendLimit The maximum possible amount of events.
     */
    public void setSendLimit(int sendLimit) {
        this.sendLimit = sendLimit;
    }

    /**
     * Sets the HttpMethod for the Emitter
     *
     * @param method the HttpMethod
     */
    public void setHttpMethod(@NonNull HttpMethod method) {
        if (!isCustomNetworkConnection && !isRunning.get()) {
            this.httpMethod = method;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(uri)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .build();
        }
    }

    /**
     * Sets the Protocol for the Emitter
     *
     * @param security the Protocol
     */
    public void setRequestSecurity(@NonNull Protocol security) {
        if (!isCustomNetworkConnection && !isRunning.get()) {
            this.requestSecurity = security;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(uri)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .build();
        }
    }

    /**
     * Updates the URI for the Emitter
     *
     * @param uri new Emitter URI
     */
    public void setEmitterUri(@NonNull String uri) {
        if (!isCustomNetworkConnection && !isRunning.get()) {
            this.uri = uri;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(uri)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .build();
        }
    }

    /**
     * Updates the custom Post path for the Emitter
     *
     * @param customPostPath new Emitter custom Post path
     */
    public void setCustomPostPath(@Nullable String customPostPath) {
        if (!isCustomNetworkConnection && !isRunning.get()) {
            this.customPostPath = customPostPath;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(uri)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .build();
        }
    }

    /**
     * Updates the timeout for the Emitter
     *
     * @param emitTimeout new Emitter timeout
     */
    public void setEmitTimeout(int emitTimeout) {
        if (!isCustomNetworkConnection && !isRunning.get()) {
            this.emitTimeout = emitTimeout;
            this.networkConnection = new OkHttpNetworkConnection.OkHttpNetworkConnectionBuilder(uri)
                    .method(httpMethod)
                    .tls(tlsVersions)
                    .emitTimeout(emitTimeout)
                    .customPostPath(customPostPath)
                    .client(client)
                    .build();
        }
    }

    /**
     * @return the emitter uri
     */
    @NonNull
    public String getEmitterUri() {
        return networkConnection.getUri().toString();
    }

    /**
     * @return the request callback method
     */
    @Nullable
    public RequestCallback getRequestCallback() {
        return this.requestCallback;
    }

    /**
     * @param requestCallback the callback request
     */
    public void setRequestCallback(@Nullable RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
    }

    /**
     * @return the Emitters request method
     */
    @NonNull
    public HttpMethod getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * @return the buffer option selected for the emitter
     */
    @NonNull
    public BufferOption getBufferOption() {
        return this.bufferOption;
    }

    /**
     * @return the request security selected for the emitter
     */
    @NonNull
    public Protocol getRequestSecurity() {
        return this.requestSecurity;
    }

    /**
     * @return the TLS versions accepted for the emitter
     */
    @NonNull
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
     * @param byteLimitGet Set the GET byte limit
     */
    public void setByteLimitGet(long byteLimitGet) {
        this.byteLimitGet = byteLimitGet;
    }

    /**
     * @return the POST byte limit
     */
    public long getByteLimitPost() {
        return this.byteLimitPost;
    }

    /**
     * @param byteLimitPost Set the POST byte limit
     */
    public void setByteLimitPost(long byteLimitPost) {
        this.byteLimitPost = byteLimitPost;
    }

    /**
     * @return the customPostPath
     */
    @Nullable
    public String getCustomPostPath() {
        return this.customPostPath;
    }

    /**
     * @return the emitTimeout
     */
    public int getEmitTimeout() {
        return this.emitTimeout;
    }

    /**
     * @return the NetworkConnection if it exists
     */
    @Nullable
    public NetworkConnection getNetworkConnection() {
        return this.networkConnection;
    }
}
