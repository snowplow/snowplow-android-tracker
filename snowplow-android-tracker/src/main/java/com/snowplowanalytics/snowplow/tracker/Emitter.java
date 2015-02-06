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
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Request;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.EmitterException;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

import rx.Observable;
import rx.Subscription;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Scheduler scheduler = Schedulers.io();
    private Context context;
    private EventStore eventStore;
    private Uri.Builder uriBuilder;
    private Subscription emitterSub;
    protected RequestCallback requestCallback;
    protected HttpMethod httpMethod;
    protected BufferOption option = BufferOption.Default;

    // TODO: Replace isRunning with a blocking state on the emitter process
    private boolean isRunning = false;
    private int emptyCounter = 0;

    /**
     * Creates an emitter object
     * @param builder The builder that constructs an emitter
     */
    private Emitter(EmitterBuilder builder) {
        this.httpMethod = builder.httpMethod;
        this.requestCallback = builder.requestCallback;
        this.context = builder.context;

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

        // Create the event store with the context and the buffer option
        this.eventStore = new EventStore(this.context);

        // If the device is not online do not send anything!
        if (isOnline()) {
            start();
        }
    }

    public static class EmitterBuilder {
        private final String uri; // Required
        private final Context context; // Required
        protected RequestCallback requestCallback = null; // Optional
        protected HttpMethod httpMethod = HttpMethod.POST; // Optional

        /**
         * @param uri The uri of the collector
         */
        public EmitterBuilder(String uri, Context context) {
            this.uri = uri;
            this.context = context;
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
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(Payload payload) {

        // Adds the payload to the EventStore
        eventStore.add(payload);

        // If the emitter is currently shutdown start it..
        if (emitterSub == null && isOnline()) {
            start();
        }
    }

    /**
     * Starts a polling emitter subscription
     * which will send all events to the collector.
     *
     * 1. If it is currently sending events we
     *    cannot get new batches of events.
     * 2. If it pulls an empty batch of events
     *    a certain amount of times we
     *    shutdown and wait for a new event add.
     * 3. This subscription will only start if
     *    we are online.
     */
    public void start() {
        emitterSub = Observable.interval(TrackerConstants.EMITTER_TICK, TimeUnit.SECONDS)
            .map((tick) -> {
                if (!isRunning) {
                    isRunning = true;
                    return eventStore.getEmittableEvents();
                }
                else {
                    throw new EmitterException("Event sending concurrency exception");
                }
            })
            .doOnError((err) -> Logger.ifDebug(TAG, "Emitter Error: %s", err.toString()))
            .retry()
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .doOnSubscribe(() -> Logger.ifDebug(TAG, "Emitter has been started!"))
            .doOnUnsubscribe(() -> Logger.ifDebug(TAG, "Emitter has been shutdown!"))
            .flatMap(this::emitEvent)
            .subscribe(results -> {

                Logger.ifDebug(TAG, "Processing emitter results.");

                if (results.size() == 0) {
                    emptyCounter++;
                    Logger.ifDebug(TAG, "Empty results counter: %s", emptyCounter);

                    if (emptyCounter >= TrackerConstants.EMITTER_EMPTY_EVENTS_LIMIT) {
                        shutdown();
                    }
                }
                else {
                    emptyCounter = 0;
                    int successCount = 0;
                    int failureCount = 0;

                    for (RequestResult res : results) {
                        if (res.getSuccess()) {
                            successCount++;
                            Logger.ifDebug(TAG, "Successful send.");

                            // Delete event rows for successfully sent requests
                            for (Long eventId : res.getEventIds()) {
                                eventStore.removeEvent(eventId);
                            }
                        }
                        else if (!res.getSuccess()) {
                            failureCount++;
                            Logger.ifDebug(TAG, "Request sending failed but we will retry later.");
                        }
                    }

                    // If any events failed and the device is online
                    if (isOnline() && failureCount != 0) {
                        Logger.ifDebug(TAG, "Check your collector path: %s",
                                uriBuilder.clearQuery().toString());

                        shutdown();
                    }

                    // Send the callback if it is not null...
                    if (requestCallback != null) {
                        if (failureCount != 0) {
                            requestCallback.onFailure(successCount, failureCount);
                        }
                        else {
                            requestCallback.onSuccess(successCount);
                        }
                    }
                }

                // Reset isRunning after completion
                isRunning = false;
            });
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        if (emitterSub != null) {
            emitterSub.unsubscribe();
            emitterSub = null;
        }
    }

    /**
     * Emits all the events in the EmittableEvents
     * object.
     *
     * @return Observable that will emit once containing
     * the request results.
     */
    public Observable<LinkedList<RequestResult>> emitEvent(final EmittableEvents events) {
        return Observable
            .just(events)
            .map(this::performEmit)
            .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
    }

    /**
     * Synchronously performs a request sending
     * operation for either GET or POST.
     *
     * @param events the events to be sent
     * @return a RequestResult
     */
    public LinkedList<RequestResult> performEmit(EmittableEvents events) {

        ArrayList<Payload> payloads = events.getEvents();
        LinkedList<Long> eventIds = events.getEventIds();
        LinkedList<RequestResult> results = new LinkedList<>();

        // If there are no events to send...
        if (events.getEvents().size() == 0) {
            return results;
        }

        // If the request method is GET...
        if (httpMethod == HttpMethod.GET) {

            Logger.ifDebug(TAG, "Sending GET requests...");

            for (int i = 0; i < payloads.size(); i++) {
                // Get the eventId for this request
                LinkedList<Long> reqEventId = new LinkedList<>();
                reqEventId.add(eventIds.get(i));

                // Build the request
                Request req = requestBuilderGet(events.getEvents().get(i));
                int code = requestSender(req);

                Logger.ifDebug(TAG, "Sent a GET");

                if (code == -1) {
                    results.add(new RequestResult(false, reqEventId));
                }
                else {
                    boolean success = isSuccessfulSend(code);
                    results.add(new RequestResult(success, reqEventId));
                }
            }
        }
        else {

            Logger.ifDebug(TAG, "Sending POST requests...");

            for (int i = 0; i < payloads.size(); i += option.getCode()) {
                // Get the eventIds for this POST Request
                LinkedList<Long> reqEventIds = new LinkedList<>();

                // Add payloads together for a POST Event
                ArrayList<Map> postPayloadMaps = new ArrayList<>();
                for (int j = i; j < (i + option.getCode()) && j < payloads.size(); j++) {
                    postPayloadMaps.add(events.getEvents().get(j).getMap());
                    reqEventIds.add(eventIds.get(j));
                }

                // As we can send multiple events in a POST we need to create a wrapper
                SchemaPayload postPayload = new SchemaPayload();
                postPayload.setSchema(TrackerConstants.SCHEMA_PAYLOAD_DATA);
                postPayload.setData(postPayloadMaps);

                // Build the request
                Request req = requestBuilderPost(postPayload);
                int code = requestSender(req);

                Logger.ifDebug(TAG, "Sent a POST");

                if (code == -1) {
                    results.add(new RequestResult(false, reqEventIds));
                }
                else {
                    boolean success = isSuccessfulSend(code);
                    results.add(new RequestResult(success, reqEventIds));
                }
            }
        }

        Logger.ifDebug(TAG, "Request Results: %s", results);
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
            Logger.ifDebug(TAG, "Request sending failed exceptionally!", e);
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
        return req;
    }

    // Setters, Getters and Checkers
    /**
     * Sets whether the buffer should send events instantly or after the buffer has reached
     * it's limit. By default, this is set to BufferOption Default.
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    public void setBufferOption(BufferOption option) {
        this.option = option;
    }

    /**
     * Checks whether or not the device
     * is online and able to communicate
     * with the outside world.
     */
    public boolean isOnline() {

        Logger.ifDebug(TAG, "Checking for connectivity...");

        ConnectivityManager cm = (ConnectivityManager)
                this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            return false;
        }

        return ni.isConnected();
    }

    /**
     * Returns truth on if the request
     * was sent successfully.
     *
     * @param code the response code
     * @return the truth as to the success
     */
    public boolean isSuccessfulSend(int code) {
        return code >= 200 && code < 300;
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

    /**
     * @return the Emitters event store
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter subscription
     */
    public boolean getEmitterSubscriptionStatus() {
        return this.emitterSub != null;
    }
}
