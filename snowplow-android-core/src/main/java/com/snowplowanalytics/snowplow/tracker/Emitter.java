package com.snowplowanalytics.snowplow.tracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public abstract class Emitter {
    private final String TAG = Emitter.class.getSimpleName();

    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    protected Context context;
    protected Uri.Builder uriBuilder;
    protected RequestCallback requestCallback;
    protected HttpMethod httpMethod;
    protected BufferOption bufferOption;
    protected RequestSecurity requestSecurity;

    public static class EmitterBuilder {

        protected static Class<? extends Emitter> defaultEmitterClass;

        /* Prefer Rx, then lite versions of our emitters */
        static {
            try {
                defaultEmitterClass = (Class<? extends Emitter>)Class.forName("com.snowplowanalytics.snowplow.tracker.rx.Emitter");
            } catch (ClassNotFoundException e) {
                try {
                    defaultEmitterClass = (Class<? extends Emitter>)Class.forName("com.snowplowanalytics.snowplow.tracker.lite.Emitter");
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

        /**
         * @param uri The uri of the collector
         */
        public EmitterBuilder(String uri, Context context) {
            this(uri, context, defaultEmitterClass);
        }

        /**
         *
         * @param uri
         * @param context
         * @param emitterClass
         */
        public EmitterBuilder(String uri, Context context, Class<? extends Emitter> emitterClass) {
            this.uri = uri;
            this.context = context;
            this.emitterClass = emitterClass;
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
            if (emitterClass == null) throw new IllegalStateException("No emitter class found or defined");

            try {
                Constructor<? extends Emitter> c =  emitterClass.getDeclaredConstructor(EmitterBuilder.class);
                return c.newInstance(this);
            } catch (NoSuchMethodException|InvocationTargetException|
                    InstantiationException|IllegalAccessException e) {
                throw new IllegalStateException("Can’t create emitter", e);
            }
        }
    }

    /**
     * Creates an emitter object
     * @param builder The builder that constructs an emitter
     */
    public Emitter(EmitterBuilder builder) {
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
     * Synchronously performs a request sending
     * operation for either GET or POST.
     *
     * @param events the events to be sent
     * @return a RequestResult
     */
    protected LinkedList<RequestResult> performEmit(EmittableEvents events) {

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
    protected int requestSender(Request request) {
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
    protected Request requestBuilderGet(Payload payload) {

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
    protected Request requestBuilderPost(Payload payload) {
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, payload.toString());
        return new Request.Builder()
                .url(reqUrl)
                .post(reqBody)
                .build();
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
    protected boolean isSuccessfulSend(int code) {
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
     * @return the emitter uri
     */
    public String getEmitterUri() {
        return this.uriBuilder.clearQuery().build().toString();
    }

    /**
     * @return the emitter event store
     */
//    public EventStore getEventStore() {
//        return this.eventStore;
//    }

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
