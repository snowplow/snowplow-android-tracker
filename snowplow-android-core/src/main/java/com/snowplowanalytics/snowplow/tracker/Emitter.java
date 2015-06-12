package com.snowplowanalytics.snowplow.tracker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

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
        protected int emitterTick = 5; // Optional
        protected int sendLimit = 250; // Optional
        protected int emptyLimit = 5; // Optional
        protected long byteLimitGet = 40000; // Optional
        protected long byteLimitPost = 40000; // Optional

        /**
         * @param uri The uri of the collector
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
         * @param emitterTick The tick count between emitter attempts
         */
        public EmitterBuilder tick(int emitterTick) {
            this.emitterTick = emitterTick;
            return this;
        }

        /**
         * @param sendLimit The maximum amount of events to grab for an emit attempt
         */
        public EmitterBuilder sendLimit(int sendLimit) {
            this.sendLimit = sendLimit;
            return this;
        }

        /**
         * @param emptyLimit The amount of emitter ticks that are performed before we shut down
         *                   due to the database being empty.
         */
        public EmitterBuilder emptyLimit(int emptyLimit) {
            this.emptyLimit = emptyLimit;
            return this;
        }

        /**
         * @param byteLimitGet The maximum amount of bytes allowed to be sent in a payload
         *                     in a GET request.
         */
        public EmitterBuilder byteLimitGet(long byteLimitGet) {
            this.byteLimitGet = byteLimitGet;
            return this;
        }

        /**
         * @param byteLimitPost The maximum amount of bytes allowed to be sent in a payload
         *                      in a POST request.
         */
        public EmitterBuilder byteLimitPost(long byteLimitPost) {
            this.byteLimitPost = byteLimitPost;
            return this;
        }

        /**
         * @return a new Emitter object
         */
        public Emitter build() {
            if (emitterClass == null) {
                throw new IllegalStateException("No emitter class found or defined");
            }

            try {
                Constructor<? extends Emitter> c =  emitterClass.getDeclaredConstructor(EmitterBuilder.class);
                return c.newInstance(this);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Can’t create emitter", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Can’t create emitter", e);
            } catch (InstantiationException e) {
                throw new IllegalStateException("Can’t create emitter", e);
            } catch (IllegalAccessException e) {
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
        this.emitterTick = builder.emitterTick;
        this.emptyLimit = builder.emptyLimit;
        this.sendLimit = builder.sendLimit;
        this.byteLimitGet = builder.byteLimitGet;
        this.byteLimitPost = builder.byteLimitPost;
        this.uri = builder.uri;
        buildEmitterUri();

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
     * Synchronously performs a request sending
     * operation for either GET or POST.
     *
     * @param events the events to be sent
     * @return a RequestResult
     */
    protected LinkedList<RequestResult> performEmit(EmittableEvents events) {

        int payloadCount = events.getEvents().size();
        LinkedList<Long> eventIds = events.getEventIds();
        LinkedList<RequestResult> results = new LinkedList<>();

        if (httpMethod == HttpMethod.GET) {

            Logger.v(TAG, "Sending events with GET requests: count %s", payloadCount);

            for (int i = 0; i < payloadCount; i++) {

                // Get the eventId for this request
                LinkedList<Long> reqEventId = new LinkedList<>();
                reqEventId.add(eventIds.get(i));

                // Build and send the request
                Payload payload = events.getEvents().get(i);
                addStmToEvent(payload, "");
                Request req = requestBuilderGet(payload);
                int code = requestSender(req);

                // If the payload is too large we will attempt to send anyway
                // but will not re-attempt.

                long payloadByteSize = payload.getByteSize();

                if (payloadByteSize > byteLimitGet) {
                    Logger.d(TAG, "Over-sized GET request - result: %s", "" + code);
                    code = 200;
                }
                else {
                    Logger.d(TAG, "GET request - result: %s", "" + code);
                }
                Logger.d(TAG, "GET request - byte-size: %s", payloadByteSize);

                results.add(new RequestResult(isSuccessfulSend(code), reqEventId));
            }
        }
        else {

            Logger.v(TAG, "Sending events with POST requests: count %s", payloadCount);

            for (int i = 0; i < payloadCount; i += bufferOption.getCode()) {

                String timestamp = Util.getTimestamp();

                // Collections for Multi-Event Posts
                LinkedList<Long> reqEventIds = new LinkedList<>();
                ArrayList<Map> postPayloadMaps = new ArrayList<>();

                // Keep record of total byte size
                long totalByteSize = TrackerConstants.POST_ENVELOPE_SIZE;

                for (int j = i; j < (i + bufferOption.getCode()) && j < payloadCount; j++) {

                    Payload payload = events.getEvents().get(j);
                    addStmToEvent(payload, timestamp);
                    long payloadByteSize = payload.getByteSize();

                    if (payloadByteSize + TrackerConstants.POST_ENVELOPE_SIZE > byteLimitPost) {

                        // Add needed information to collections
                        ArrayList<Map> singlePayloadMap = new ArrayList<>();
                        LinkedList<Long> reqEventId = new LinkedList<>();

                        // Update the sent time
                        addStmToEvent(payload, Util.getTimestamp());

                        singlePayloadMap.add(payload.getMap());
                        reqEventId.add(eventIds.get(j));

                        // Build and send request
                        int code = buildAndSendPost(singlePayloadMap);

                        Logger.d(TAG, "Over-sized POST request - result: %s", "" + code);
                        Logger.d(TAG, "Over-sized POST request - byte-size: %s", payloadByteSize);

                        // Add successful send
                        results.add(new RequestResult(true, reqEventId));
                    }
                    else if (totalByteSize + payloadByteSize > byteLimitPost) {
                        // Build and send request
                        int code = buildAndSendPost(postPayloadMaps);

                        Logger.d(TAG, "POST request - result: %s", "" + code);
                        Logger.d(TAG, "POST request - byte-size: %s", totalByteSize);

                        // Add result
                        results.add(new RequestResult(isSuccessfulSend(code), reqEventIds));

                        // Clear collections and add new event
                        postPayloadMaps = new ArrayList<>();
                        reqEventIds = new LinkedList<>();

                        // Update the sent time
                        timestamp = Util.getTimestamp();
                        addStmToEvent(payload, timestamp);

                        postPayloadMaps.add(payload.getMap());
                        reqEventIds.add(eventIds.get(j));
                        totalByteSize = payloadByteSize + TrackerConstants.POST_ENVELOPE_SIZE;
                    }
                    else {
                        totalByteSize += payloadByteSize;
                        postPayloadMaps.add(payload.getMap());
                        reqEventIds.add(eventIds.get(j));
                    }
                }

                if (!postPayloadMaps.isEmpty()) {
                    int code = buildAndSendPost(postPayloadMaps);
                    results.add(new RequestResult(isSuccessfulSend(code), reqEventIds));

                    Logger.d(TAG, "POST request - result: %s", "" + code);
                    Logger.d(TAG, "POST request - byte-size: %s", totalByteSize);
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
            Logger.v(TAG, "Sending request: %s", request);
            return client.newCall(request).execute().code();
        } catch (IOException e) {
            Logger.e(TAG, "Request sending failed: %s", e.toString());
            return -1;
        }
    }

    /**
     * Builds and sends the POST event
     * to the configured collector.
     *
     * @param payload The payload to be sent
     * @return the response code
     */
    private int buildAndSendPost(ArrayList<Map> payload) {
        SelfDescribingJson postPayload =
                new SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, payload);
        Request req = requestBuilderPost(postPayload);
        return requestSender(req);
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
     * Checks whether or not the device
     * is online and able to communicate
     * with the outside world.
     */
    public boolean isOnline() {

        Logger.v(TAG, "Checking tracker internet connectivity.");

        ConnectivityManager cm = (ConnectivityManager)
                this.context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            boolean connected = ni != null && ni.isConnected();
            Logger.d(TAG, "Tracker connection online: %s", connected);
            return connected;
        } catch (SecurityException e) {
            Logger.e(TAG, "Security exception checking connection: %s", e.toString());
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
     * Sets the HttpMethod for the Emitter
     * @param method the HttpMethod
     */
    public void setHttpMethod(HttpMethod method) {
        this.httpMethod = method;
        buildEmitterUri();
    }

    /**
     * Sets the RequestSecurity for the Emitter
     * @param security the RequestSecurity
     */
    public void setRequestSecurity(RequestSecurity security) {
        this.requestSecurity = security;
        buildEmitterUri();
    }

    /**
     * Updates the URI for the Emitter
     * @param uri new Emitter URI
     */
    public void setEmitterUri(String uri) {
        this.uri = uri;
        buildEmitterUri();
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
