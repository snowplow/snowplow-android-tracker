/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.android.emitter;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.android.Constants;
import com.snowplowanalytics.snowplow.tracker.android.EventStore;
import com.snowplowanalytics.snowplow.tracker.android.EventStoreHelper;
import com.snowplowanalytics.snowplow.tracker.android.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.core.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.core.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.core.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.core.emitter.RequestMethod;
import com.snowplowanalytics.snowplow.tracker.core.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.core.payload.TrackerPayload;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Emitter extends com.snowplowanalytics.snowplow.tracker.core.emitter.Emitter {

    private final String TAG = Emitter.class.getName();
    private final Uri.Builder uriBuilder = new Uri.Builder();
    private final EventStore eventStore;

    private LinkedList<Payload> unsentPayloads;
    private LinkedList<Long> indexArray;

    /**
     * Create an Emitter instance with a collector URL.
     *
     * @param URI The collector URL. Don't include "http://" - this is done automatically.
     */
    public Emitter(String URI, Context context) {
        this(URI, HttpMethod.GET, null, context);
    }

    /**
     * Create an Emitter instance with a collector URL, and callback method.
     *
     * @param URI      The collector URL. Don't include "http://" - this is done automatically.
     * @param callback The callback method to handle success/failure cases when sending events.
     */
    public Emitter(String URI, Context context, RequestCallback callback) {
        this(URI, HttpMethod.GET, callback, context);
    }

    /**
     * Create an Emitter instance with a collector URL,
     *
     * @param URI        The collector URL. Don't include "http://" - this is done automatically.
     * @param httpMethod The HTTP request method. If GET, <code>BufferOption</code> is set to <code>Instant</code>.
     */
    public Emitter(String URI, Context context, HttpMethod httpMethod) {
        this(URI, httpMethod, null, context);
    }

    /**
     * Create an Emitter instance with a collector URL and HttpMethod to send requests.
     *
     * @param URI        The collector URL. Don't include "http://" - this is done automatically.
     * @param httpMethod The HTTP request method. If GET, <code>BufferOption</code> is set to <code>Instant</code>.
     * @param callback   The callback method to handle success/failure cases when sending events.
     */
    public Emitter(String URI, HttpMethod httpMethod, RequestCallback callback, Context context) {
        if(httpMethod == HttpMethod.GET) {
            uriBuilder.scheme("http")
                    .authority(URI)
                    .appendPath("i");
        } else {
            uriBuilder.scheme("http")
                    .authority(URI)
                    .appendEncodedPath(Constants.PROTOCOL_VENDOR + "/" + Constants.PROTOCOL_VERSION);
        }
        super.httpMethod = httpMethod;
        super.requestCallback = callback;
        this.eventStore = new EventStore(context);

        if (httpMethod == HttpMethod.GET) {
            super.setBufferOption(BufferOption.Instant);
        }
    }

    /**
     * Empties the cached events manually. This shouldn't be used unless you're aware of it works.
     * If you need events send instantly, use set the <code>Emitter</code> buffer to <code>BufferOption.Instant</code>
     */
    @SuppressWarnings("unchecked")
    @Override
    public void flushBuffer() {
        indexArray = new LinkedList<Long>();

        if (httpMethod == HttpMethod.GET) {

            // We cycle through each event that is NOT pending.
            for (Map<String, Object> eventMetadata : eventStore.getAllNonPendingEvents()) {
                // Because we get a raw Object from the database, we cast it to Map<String, Object>
                // Then create a TrackerPayload map and add the contents to that.
                // We do this because our overridden sendGetData method accepts a Payload parameter.
                TrackerPayload payload = new TrackerPayload();
                payload.addMap((Map<String, Object>)
                        eventMetadata.get(EventStoreHelper.METADATA_EVENT_DATA));

                // Each event's database index is added to a global array
                // to be used later for removal of events from the database.
                indexArray.add((Long) eventMetadata.get(EventStoreHelper.METADATA_ID));

                // Setting the event to pending so we don't pick it up a second time.
                eventStore.setPending((Long)eventMetadata.get(EventStoreHelper.METADATA_ID));

                // Sending each event individually.
                this.sendGetData(payload);
            }


        } else if (httpMethod == HttpMethod.POST) {

            // We can accept multiple events in a POST request so we create a wrapper for them.
            SchemaPayload postPayload = new SchemaPayload();
            postPayload.setSchema(Constants.SCHEMA_PAYLOAD_DATA);
            ArrayList<Map> eventMaps = new ArrayList<Map>();

            // We cycle through each event that is NOT pending
            for (Map<String, Object> eventMetadata : eventStore.getAllNonPendingEvents()) {

                // Because we get a raw Object from the database, we cast it to Map<String, Object>
                // Then create a TrackerPayload map and add the contents to that.
                // We do this because our overridden sendGetData method accepts a Payload parameter.
                TrackerPayload payload = new TrackerPayload();
                payload.addMap((Map<String, Object>)
                        eventMetadata.get(EventStoreHelper.METADATA_EVENT_DATA));

                // Each event's database index is added to a global array
                // to be used later for removal of events from the database.
                indexArray.add((Long) eventMetadata.get(EventStoreHelper.METADATA_ID));

                // Setting the event to pending so we don't pick it up a second time.
                eventStore.setPending((Long)eventMetadata.get(EventStoreHelper.METADATA_ID));

                // Adding the event to the wrapper payload.
                eventMaps.add(payload.getMap());
            }

            // Setting the array of events as the 'data' of the wrapper.
            Log.d(TAG, "indexArray before deleting: " + indexArray);
            postPayload.setData(eventMaps);

            // We finally send this completed wrapper with all the events in it.
            this.sendPostData(postPayload);
        }
    }

    protected HttpResponse sendGetData(Payload payload) {
        // This method is used to call the AsyncHttpGet class to actually send the events
        // The httpResponse is almost always going to be empty, so avoid using it's return value

        HttpResponse httpResponse = null;
        AsyncHttpGet asyncHttpGet = new AsyncHttpGet(payload, indexArray);
        asyncHttpGet.execute();

        try {
            httpResponse = asyncHttpGet.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return httpResponse;
    }

    protected HttpResponse sendPostData(Payload payload) {
        // This method is used to call the AsyncHttpPost class to actually send the events
        // The httpResponse is almost always going to be empty, so avoid using it's return value

        HttpResponse httpResponse = null;
        AsyncHttpPost asyncHttpPost = new AsyncHttpPost(payload, indexArray);
        asyncHttpPost.execute();

        try {
            httpResponse = asyncHttpPost.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return httpResponse;
    }

    @Deprecated
    @Override
    public void setRequestMethod(RequestMethod option) {
        Log.e(TAG, "Cannot change RequestMethod: Asynchronous requests only available.");
    }

    @Override
    public boolean addToBuffer(Payload payload) {
        // Checking that the eventStore is of appropriate size before calling super.addToBuffer
        // There doesn't seem to be any need for the in-memory buffer array,
        // but we keep it for future development in case we find a better use for it.
        long eventId = eventStore.insertPayload(payload);

        if (eventStore.size() >= super.option.getCode()) {
            flushBuffer();
        }

        // Android returns -1 if an error occurred during insert.
        return eventId != -1;
    }

    private class AsyncHttpPost extends AsyncTask<Void, Void, HttpResponse> {
        private Payload payload = null;
        private String TAG = "Emitter" + "+AsyncHttpPost";
        private LinkedList<Long> pendingEventIds = null;

        AsyncHttpPost(Payload payload, LinkedList<Long> pendingEventIds) {
            this.payload = payload;
            this.pendingEventIds = pendingEventIds;
        }

        @Override
        protected HttpResponse doInBackground(Void... voids) {
            // Here we do the actual sending of the request

            HttpPost httpPost = new HttpPost(uriBuilder.build().toString());
            httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
            HttpResponse httpResponse = null;
            HttpClient httpClient = new DefaultHttpClient();

            try {
                StringEntity params = new StringEntity(payload.toString());
                httpPost.setEntity(params);
                httpResponse = httpClient.execute(httpPost);
                Log.d(TAG, payload.toString());
                Log.d(TAG, httpResponse.getStatusLine().toString());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Encoding exception with the payload.");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "Error when sending HTTP POST.");
                e.printStackTrace();
            }

            return httpResponse;
        }

        @Override
        protected void onPostExecute(HttpResponse result) {
            // When the task is done, we execute the onPostExecute to see
            // what kind of a HTTP response code we get.

            // Ths is used for the callback.
            unsentPayloads = new LinkedList<Payload>();
            int status_code = -1;

            if (result != null)
                status_code = result.getStatusLine().getStatusCode();
            else
                Log.w(TAG, "HttpResponse was null. There may be a network issue.");

            Log.d(TAG, "Status code: " + status_code);

            // If the events were successfully sent...
            if (status_code == 200) {
                // We remove the event from the database using the indexes from the pendingEventIds.
                Log.d(TAG, "We're about to remove from pendingEventIds: " + this.pendingEventIds);
                for (int i = 0; i < this.pendingEventIds.size(); i++) {
                    eventStore.removeEvent(this.pendingEventIds.get(i));
                    Log.d(TAG, "Removing event with index: " + this.pendingEventIds.get(i));
                }

                // If there is a RequestCallback set, we send the appropriate information
                if(requestCallback != null) {
                    int success_count = (int) eventStore.size();
                    Log.d(TAG, "onPostExecute POST Success: " + success_count);
                    requestCallback.onSuccess(success_count);
                }

            } else { // If there was any kind of failure..

                // We remove the pending flag from the events so they can be picked up again
                // when we try to send the events in another attempt.
                for (int i = 0; i < this.pendingEventIds.size(); i++) {
                    Log.d(TAG, "Removing PENDING with index: " + this.pendingEventIds.get(i));
                    eventStore.removePending(this.pendingEventIds.get(i));
                }

                // If there is a RequestCallback set, we send the appropriate information
                if (requestCallback != null) {
                    Log.d(TAG, "onPostExecute POST Failure: 0");
                    unsentPayloads.add(payload);
                    requestCallback.onFailure(0, unsentPayloads);
                }
            }
        }
    }

    private class AsyncHttpGet extends AsyncTask<Void, Void, HttpResponse> {
        private Payload payload = null;
        private String TAG = "Emitter" + "+AsyncHttpGet";
        private LinkedList<Long> pendingEventIds = null;

        AsyncHttpGet(Payload payload, LinkedList<Long> pendingEventIds) {
            this.payload = payload;
            this.pendingEventIds = pendingEventIds;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected HttpResponse doInBackground(Void... voids) {
            // Here we do the actual sending of the request

            HashMap hashMap = (HashMap) payload.getMap();
            Iterator<String> iterator = hashMap.keySet().iterator();
            HttpResponse httpResponse = null;
            HttpClient httpClient = new DefaultHttpClient();
            uriBuilder.clearQuery();

            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = (String) hashMap.get(key);
                uriBuilder.appendQueryParameter(key, value);
            }

            try {
                HttpGet httpGet = new HttpGet(uriBuilder.build().toString());
                httpResponse = httpClient.execute(httpGet);
                Log.d(TAG, payload.toString());
                Log.d(TAG, httpResponse.getStatusLine().toString());
            } catch (IOException e) {
                Log.d(TAG, "Error when sending HTTP GET error.");
                e.printStackTrace();
            }

            return httpResponse;
        }

        @Override
        protected void onPostExecute(HttpResponse response) {
            // When the task is done, we execute the onPostExecute to see
            // what kind of a HTTP response code we get.

            // Ths is used for the callback.
            unsentPayloads = new LinkedList<Payload>();

            // The success count is reset before we start counting again.
            int success_count = 0;
            int status_code = -1;

            if (response != null)
                status_code = response.getStatusLine().getStatusCode();
            else
                Log.w(TAG, "HttpResponse was null. There may be a network issue.");

            Log.d(TAG, "Status code: " + status_code);

            // If the events were successfully sent...
            if (status_code == 200) {
                // Incrementing our success count
                success_count++;

                // We remove the event from the database using the indexes from the pendingEventIds.
                for (int i = 0; i < this.pendingEventIds.size(); i++) {
                    eventStore.removeEvent(this.pendingEventIds.get(i));
                    Log.d(TAG, "Removing event with index: " + this.pendingEventIds.get(i));
                }

            } else { // If there was any kind of failure..
                // Adding the payload to the array to be returned in the callback.
                unsentPayloads.add(payload);

                // We remove the pending flag from the events so they can be picked up again
                // when we try to send the events in another attempt.
                for (int i = 0; i < this.pendingEventIds.size(); i++) {
                    Log.d(TAG, "Removing PENDING with index: " + this.pendingEventIds.get(i));
                    eventStore.removePending(this.pendingEventIds.get(i));
                }

            }

            // If there is a RequestCallback set, we send the appropriate information
            if (unsentPayloads.size() == 0) {
                if (requestCallback != null)
                    requestCallback.onSuccess(success_count);
            } else if (requestCallback != null) {
                requestCallback.onFailure(success_count, unsentPayloads);
            }
        }
    }
}
