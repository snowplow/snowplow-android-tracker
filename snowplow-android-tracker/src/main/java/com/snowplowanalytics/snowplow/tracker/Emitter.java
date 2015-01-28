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
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.emitter_utils.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter_utils.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter_utils.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.payload_utils.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.payload_utils.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.storage.EventStoreHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Request;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class Emitter {

    private final String TAG = Emitter.class.getName();
    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final EventStore eventStore;
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
        this.eventStore = new EventStore(builder.context);

        // Need to create URI Builder in this way to preserve port keys
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
        private final Context context; // Required
        protected RequestCallback requestCallback = null; // Optional
        protected HttpMethod httpMethod = HttpMethod.POST; // Optional

        /**
         * @param uri The uri of the collector
         * @param context The android context object
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
        public Emitter build(){
            return new Emitter(this);
        }
    }

    // TODO: Convert this function to an observable event that happens in a non-ui thread
    /**
     * Checking that the eventStore is of appropriate size before calling 'addToBuffer'
     * There doesn't seem to be any need for the in-memory buffer array,
     * but we keep it for future development in case we find a better use for it.
     * @param payload The Payload to add to the eventStore
     * @return a boolean if insert was successful or not
     */
    public boolean addToBuffer(Payload payload) {
        long eventId = eventStore.insertPayload(payload);
        if (eventStore.size() >= option.getCode()) {
            flushBuffer();
        }

        // Android returns -1 if an error occurred during insert.
        return eventId != -1;
    }

    // TODO: Convert this function to an observable event that happens in a non-ui thread
    /**
     * Empties the cached events manually. This shouldn't be used unless you're aware of it works.
     * If you need events send instantly, use set the <code>Emitter</code> buffer to <code>BufferOption.Instant</code>
     */
    @SuppressWarnings("unchecked")
    public void flushBuffer() {

        // Reset the indexArray on each buffer flush
        LinkedList<Long> indexArray = new LinkedList<>();

        // Store all events as payloads
        ArrayList<Payload> events = new ArrayList<>();

        // Loop through all non-pending events and convert them into Payloads
        for (Map<String, Object> eventMetadata : eventStore.getAllNonPendingEvents()) {

            // Create a TrackerPayload for each non-pending event
            TrackerPayload payload = new TrackerPayload();
            Map<String, Object> eventData = (Map<String, Object>)
                    eventMetadata.get(EventStoreHelper.METADATA_EVENT_DATA);
            payload.addMap(eventData);

            // Set the event to pending status to prevent it being picked up again
            Long eventId = (Long) eventMetadata.get(EventStoreHelper.METADATA_ID);
            indexArray.add(eventId);
            eventStore.setPending(eventId);

            // Add the event payload
            events.add(payload);
        }

        // If the request method is GET...
        if (httpMethod == HttpMethod.GET) {
            for (Payload event : events) {

                // Build the request and send it
                Request req = getRequestBuilder(event);

                // Create a new subscriber and send the request
                requestSubscribe(req, event, indexArray);
            }
        }
        else {

            // Convert the ArrayList of Payloads into an ArrayList of Maps
            ArrayList<Map> eventMaps = new ArrayList<>();
            for (Payload event : events) {
                eventMaps.add(event.getMap());
            }

            // As we can send multiple events in a POST we need to create a wrapper
            SchemaPayload postPayload = new SchemaPayload();
            postPayload.setSchema(TrackerConstants.SCHEMA_PAYLOAD_DATA);
            postPayload.setData(eventMaps);

            // Build the request
            Request req = postRequestBuilder(postPayload);

            // Create a new subscriber and send the request
            requestSubscribe(req, postPayload, indexArray);
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
    private Request getRequestBuilder(Payload payload) {

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
    private Request postRequestBuilder(Payload payload) {
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, payload.toString());
        return new Request.Builder()
                .url(reqUrl)
                .post(reqBody)
                .build();
    }

    // RxAndroid Asynchronous request sending

    /**
     * The function responsible for actually sending
     * the request to the collector.
     *
     * @param request The request to be sent
     * @return success code or -1 for any exceptions
     */
    private Integer observableRequestSender(Request request) {
        try {
            // Send the request and record the response code
            return client.newCall(request).execute().code();
        } catch (IOException e) {
            // Return -1 for an IOException
            return -1;
        }
    }

    /**
     * Creates a new subscriber through which we can
     * observe the sending of the Http Request in a
     * different thread.
     *
     * @param request The request to be sent
     * @param payload The payload to be sent (in case
     *                the request fails for whatever
     *                reason).
     * @param eventIds The indexArray at the time of
     *                 sending
     * @return a subscription to the request
     */
    public Subscription requestSubscribe(Request request, Payload payload,
                                         LinkedList<Long> eventIds) {

        return getRequestSenderObservable(request)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(getRequestSenderObserver(payload, eventIds));
    }

    /**
     * Creates a new Observable which is the object
     * containing the action of sending the Request.
     * Every time this is subscribed to it will send
     * the request.
     *
     * @param request The request to be sent
     * @return a new observable of type integer
     */
    @SuppressWarnings("unchecked")
    public Observable<Integer> getRequestSenderObservable(final Request request) {

        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber subscriber) {
                subscriber.onNext(observableRequestSender(request));
                subscriber.onCompleted();
            }
        });
    }

    /**
     * Creates an observer which is responsible for
     * responding to the outcome of the observable
     * event.
     *
     * @param payload The payload to be sent (in case
     *                the request fails for whatever
     *                reason).
     * @param eventIds The indexArray at the time of
     *                 sending
     * @return a custom observer
     */
    private Observer<Integer> getRequestSenderObserver(final Payload payload,
                                                       final LinkedList<Long> eventIds) {

        return new Observer<Integer>() {
            @Override
            public void onCompleted() {
                Log.d(TAG+"Rx - Complete", "On complete");
            }
            @Override
            public void onError(Throwable e) {
                Log.d(TAG+"Rx - Error", String.format(e.getMessage()));
            }
            @Override
            public void onNext(Integer code) {

                // TODO: Update how bad response codes are handled
                // - what happens to the eventStore?
                // - how can we retry?
                // - should there be a check to see if we are online before even attempting?

                // Remove events from the eventStore
                for (int i = 0; i < eventIds.size(); i++) {
                    eventStore.removeEvent(eventIds.get(i));
                }

                // Create variable for all failed requests
                LinkedList<Payload> unsentPayloads = new LinkedList<>();

                // Check if event sending was successful...
                if (code != 200) {
                    unsentPayloads.add(payload);
                }

                // Send the request callback
                if (requestCallback != null) {
                    if (unsentPayloads.size() != 0) {
                        requestCallback.onFailure(0, unsentPayloads);
                    }
                    else {
                        requestCallback.onSuccess(eventIds.size());
                    }
                }

                Log.d(TAG+"Rx - Response Code", code.toString());
            }
        };
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
}
