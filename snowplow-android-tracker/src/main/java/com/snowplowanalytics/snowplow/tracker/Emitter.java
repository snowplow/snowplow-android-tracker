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
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.EmitterException;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    //private final Scheduler scheduler = Schedulers.io();

    private Context context;
    private EventStore eventStore;
    private Uri.Builder uriBuilder;
    //private Subscription emitterSub;

    protected RequestCallback requestCallback;
    protected HttpMethod httpMethod;
    protected BufferOption bufferOption;
    protected RequestSecurity requestSecurity;

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
        this.bufferOption = builder.bufferOption;
        this.requestSecurity = builder.requestSecurity;

        // Need to create URI Builder in this way to preserve port keys/characters that would
        // be incorrectly encoded by the uriBuilder.
        if (requestSecurity == RequestSecurity.HTTP) {
            this.uriBuilder = Uri.parse("http://" + builder.uri).buildUpon();
        }
        else {
            this.uriBuilder = Uri.parse("https://" + builder.uri).buildUpon();
        }

        // Create URI based on request method
        if (httpMethod == HttpMethod.GET) {
            uriBuilder.appendPath("i");
        }
        else {
            uriBuilder.appendEncodedPath(TrackerConstants.PROTOCOL_VENDOR + "/" +
                            TrackerConstants.PROTOCOL_VERSION);
        }

        // Create the event store with the context and the buffer option
        this.eventStore = new EventStore(this.context);

        // If the device is not online do not send anything!
        if (isOnline()) {
            //start();
        }
    }

    public static class EmitterBuilder {
        private final String uri; // Required
        private final Context context; // Required
        protected RequestCallback requestCallback = null; // Optional
        protected HttpMethod httpMethod = HttpMethod.POST; // Optional
        protected BufferOption bufferOption = BufferOption.DefaultGroup; // Optional
        protected RequestSecurity requestSecurity = RequestSecurity.HTTP; // Optional

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
        public EmitterBuilder method(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        /**
         * @param option the buffer option for the emitter
         */
        public EmitterBuilder option(BufferOption option) {
            this.bufferOption = option;
            return this;
        }

        /**
         * @param requestSecurity the security chosen for requests
         */
        public EmitterBuilder security(RequestSecurity requestSecurity) {
            this.requestSecurity = requestSecurity;
            return this;
        }

        /**
         * @param requestCallback Request callback function
         */
        public EmitterBuilder callback(RequestCallback requestCallback) {
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
//        if (emitterSub == null && isOnline()) {
//            start();
//        }
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
//    private void start() {
//        // TODO SOMETHING HERE
//
//        emitterSub = Observable.interval(TrackerConstants.EMITTER_TICK, TimeUnit.SECONDS)
//            .map((tick) -> {
//                if (!isRunning) {
//                    if (eventStore.getSize() == 0) {
//                        emptyCounter++;
//
//                        Logger.ifDebug(TAG, "EventStore empty counter: %s", emptyCounter);
//
//                        if (emptyCounter >= TrackerConstants.EMITTER_EMPTY_EVENTS_LIMIT) {
//                            shutdown();
//                            throw new EmitterException("EventStore empty exception - limit");
//                        }
//                        throw new EmitterException("EventStore empty exception");
//                    }
//                    else {
//                        emptyCounter = 0;
//                        isRunning = true;
//                        return eventStore.getEmittableEvents();
//                    }
//                }
//                else {
//                    throw new EmitterException("Emitter concurrency exception");
//                }
//            })
//            .doOnError((err) -> Logger.ifDebug(TAG, "Emitter Error: %s", err.toString()))
//            .retry()
//            .subscribeOn(scheduler)
//            .unsubscribeOn(scheduler)
//            .doOnSubscribe(() -> Logger.ifDebug(TAG, "Emitter has been started!"))
//            .doOnUnsubscribe(() -> Logger.ifDebug(TAG, "Emitter has been shutdown!"))
//            .flatMap(this::emitEvent)
//            .subscribe(results -> {
//
//                Logger.ifDebug(TAG, "Processing emitter results.");
//
//                // Start counting successes and failures
//                int successCount = 0;
//                int failureCount = 0;
//
//                for (RequestResult res : results) {
//                    if (res.getSuccess()) {
//                        successCount++;
//                        Logger.ifDebug(TAG, "Successful send.");
//
//                        // Delete event rows for successfully sent requests
//                        for (Long eventId : res.getEventIds()) {
//                            eventStore.removeEvent(eventId);
//                        }
//                    } else if (!res.getSuccess()) {
//                        failureCount++;
//                        Logger.ifDebug(TAG, "Request sending failed but we will retry later.");
//                    }
//                }
//
//                // If we have any failures shut the emitter down
//                if (failureCount != 0) {
//                    if (isOnline()) {
//                        Logger.ifDebug(TAG, "Check your collector path: %s",
//                                getEmitterUri());
//                    }
//                    shutdown();
//                }
//
//                // Send the callback
//                if (requestCallback != null) {
//                    if (failureCount != 0) {
//                        requestCallback.onFailure(successCount, failureCount);
//                    } else {
//                        requestCallback.onSuccess(successCount);
//                    }
//                }
//
//                // Reset isRunning after completion
//                isRunning = false;
//            });
//    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
//        if (emitterSub != null) {
//            emitterSub.unsubscribe();
//            emitterSub = null;
//        }
    }

//    /**
//     * Emits all the events in the EmittableEvents
//     * object.
//     *
//     * @return Observable that will emit once containing
//     * the request results.
//     */
//    private Observable<LinkedList<RequestResult>> emitEvent(final EmittableEvents events) {
//        return Observable
//            .just(events)
//            .map(this::performEmit)
//            .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
//    }

    /**
     * Synchronously performs a request sending
     * operation for either GET or POST.
     *
     * @param events the events to be sent
     * @return a RequestResult
     */
    private LinkedList<RequestResult> performEmit(EmittableEvents events) {

        ArrayList<Payload> payloads = events.getEvents();
        LinkedList<Long> eventIds = events.getEventIds();
        LinkedList<RequestResult> results = new LinkedList<>();

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

                Logger.ifDebug(TAG, "Sent a GET request - code: %s", "" + code);

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

            for (int i = 0; i < payloads.size(); i += bufferOption.getCode()) {
                // Get the eventIds for this POST Request
                LinkedList<Long> reqEventIds = new LinkedList<>();

                // Add payloads together for a POST Event
                ArrayList<Map> postPayloadMaps = new ArrayList<>();
                for (int j = i; j < (i + bufferOption.getCode()) && j < payloads.size(); j++) {
                    postPayloadMaps.add(events.getEvents().get(j).getMap());
                    reqEventIds.add(eventIds.get(j));
                }

                // As we can send multiple events in a POST we need to create a wrapper
                SelfDescribingJson postPayload = new SelfDescribingJson(
                        TrackerConstants.SCHEMA_PAYLOAD_DATA, postPayloadMaps);

                // Build the request
                Request req = requestBuilderPost(postPayload);
                int code = requestSender(req);

                Logger.ifDebug(TAG, "Sent a POST request - code: %s", "" + code);

                if (code == -1) {
                    results.add(new RequestResult(false, reqEventIds));
                }
                else {
                    boolean success = isSuccessfulSend(code);
                    results.add(new RequestResult(success, reqEventIds));
                }
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
            Logger.ifDebug(TAG, "Sending request..");
            return client.newCall(request).execute().code();
        } catch (IOException e) {
            Logger.ifDebug(TAG, "Request sending failed exceptionally: %s", e.toString());
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
    private Request requestBuilderPost(Payload payload) {
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, payload.toString());
        return new Request.Builder()
                .url(reqUrl)
                .post(reqBody)
                .build();
    }

    // Setters, Getters and Checkers

    /**
     * Checks whether or not the device
     * is online and able to communicate
     * with the outside world.
     */
    public boolean isOnline() {

        Logger.ifDebug(TAG, "Checking for connectivity...");

        ConnectivityManager cm = (ConnectivityManager)
                this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (SecurityException e) {
            Logger.ifDebug(TAG, "SecurityException: %s", e.toString());
            return true;
        }
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
     * @param option Set the BufferOption enum to Instant send events upon creation.
     */
    public void setBufferOption(BufferOption option) {
        this.bufferOption = option;
    }

    /**
     * @return the emitter context
     */
    public Context getEmitterContext() {
        return this.context;
    }

    /**
     * @return the emitter event store
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter uri
     */
    public String getEmitterUri() {
        return this.uriBuilder.clearQuery().build().toString();
    }

    /**
     * @return the emitter subscription status
     */
    public boolean getEmitterSubscriptionStatus() {
        //return this.emitterSub != null;
        // TODO something useful here
        return false;
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
}
