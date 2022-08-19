package com.snowplowanalytics.snowplow.network;

import android.content.Context;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.internal.emitter.TLSArguments;
import com.snowplowanalytics.snowplow.internal.emitter.TLSVersion;
import com.snowplowanalytics.snowplow.tracker.BuildConfig;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;
import static com.snowplowanalytics.snowplow.network.HttpMethod.POST;

/**
 * Components in charge to send events to the collector.
 * It uses OkHttp as Http client.
 */
public class OkHttpNetworkConnection implements NetworkConnection {
    private final String TAG = OkHttpNetworkConnection.class.getSimpleName();

    private static final int TRAFFIC_STATS_TAG = 1;
    private static final String DEFAULT_USER_AGENT = String.format("snowplow/%s android/%s", BuildConfig.TRACKER_LABEL, Build.VERSION.RELEASE);
    private final MediaType JSON = MediaType.parse(TrackerConstants.POST_CONTENT_TYPE);

    private final String uri;
    private final Protocol protocol;
    private final HttpMethod httpMethod;
    private final int emitTimeout;
    private final String customPostPath;
    private final boolean serverAnonymisation;

    private OkHttpClient client;
    private Uri.Builder uriBuilder;

    /**
     * Builder for the OkHttpNetworkConnection.
     */
    public static class OkHttpNetworkConnectionBuilder {
        final String uri; // Required
        Context context; // Required
        HttpMethod httpMethod = POST; // Optional
        EnumSet<TLSVersion> tlsVersions = EnumSet.of(TLSVersion.TLSv1_2); // Optional
        private int emitTimeout = 5; // Optional
        OkHttpClient client = null; //Optional
        CookieJar cookieJar = null; // Optional
        String customPostPath = null; //Optional
        boolean serverAnonymisation = false; // Optional

        /**
         * @param uri The uri of the collector
         */
        public OkHttpNetworkConnectionBuilder(@NonNull String uri, @NonNull Context context) {
            this.uri = uri;
            this.context = context;
        }

        /**
         * @param httpMethod The method by which requests are emitted
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder method(@NonNull HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
            return this;
        }

        /**
         * @param version the TLS version allowed for requests
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder tls(@NonNull TLSVersion version) {
            this.tlsVersions = EnumSet.of(version);
            return this;
        }

        /**
         * @param versions the TLS versions allowed for requests
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder tls(@NonNull EnumSet<TLSVersion> versions) {
            this.tlsVersions = versions;
            return this;
        }

        /**
         * @param emitTimeout The maximum timeout for emitting events. If emit time exceeds this value
         *                    TimeOutException will be thrown
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder emitTimeout(int emitTimeout){
            this.emitTimeout = emitTimeout;
            return this;
        }

        /**
         * @param client An OkHttp client that will be used in the emitter, you can provide your
         *               own if you want to share your Singleton client's interceptors, connection pool etc..
         *               ,otherwise a new one is created.
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder client(@Nullable OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * @param cookieJar An OkHttp cookie jar to override the default cookie jar that stores
         *                  cookies in SharedPreferences. The cookie jar will be ignored in case
         *                  custom `client` is configured.
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder cookieJar(@Nullable CookieJar cookieJar) {
            this.cookieJar = cookieJar;
            return this;
        }

        /**
         * @param customPostPath A custom path that is used on the endpoint to send requests.
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder customPostPath(@Nullable String customPostPath) {
            this.customPostPath = customPostPath;
            return this;
        }

        /**
         * @param serverAnonymisation whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
         * @return itself
         */
        @NonNull
        public OkHttpNetworkConnectionBuilder serverAnonymisation(@Nullable boolean serverAnonymisation) {
            this.serverAnonymisation = serverAnonymisation;
            return this;
        }

        /**
         * Creates a new OkHttpNetworkConnection
         *
         * @return a new OkHttpNetworkConnection object
         */
        @NonNull
        public OkHttpNetworkConnection build() {
            return new OkHttpNetworkConnection(this);
        }
    }

    private OkHttpNetworkConnection(OkHttpNetworkConnectionBuilder builder) {
        // Decode uri to extract protocol
        String tempUri = builder.uri;
        Uri url = Uri.parse(builder.uri);
        Protocol tempProtocol = Protocol.HTTPS;
        if (url.getScheme() == null) {
            tempUri = "https://" + builder.uri;
        } else {
            switch (url.getScheme()) {
                case "https":
                    break;
                case "http":
                    tempProtocol = Protocol.HTTP;
                    break;
                default:
                    tempUri = "https://" + builder.uri;
            }
        }

        // Configure
        uri = tempUri;
        protocol = tempProtocol;
        httpMethod = builder.httpMethod;
        emitTimeout = builder.emitTimeout;
        customPostPath = builder.customPostPath;
        serverAnonymisation = builder.serverAnonymisation;

        TLSArguments tlsArguments = new TLSArguments(builder.tlsVersions);
        String protocolString = protocol == Protocol.HTTP ? "http://" : "https://";
        uriBuilder = Uri.parse(uri).buildUpon();

        if (httpMethod == GET) {
            uriBuilder.appendPath("i");
        } else if (this.customPostPath == null) {
            uriBuilder.appendEncodedPath(TrackerConstants.PROTOCOL_VENDOR + "/" +
                    TrackerConstants.PROTOCOL_VERSION);
        } else {
            uriBuilder.appendEncodedPath(this.customPostPath);
        }

        // Configure with external OkHttpClient
        if (builder.client == null) {
            client = new OkHttpClient.Builder()
                    .sslSocketFactory(tlsArguments.getSslSocketFactory(), tlsArguments.getTrustManager())
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(builder.cookieJar == null ? new CollectorCookieJar(builder.context) : builder.cookieJar)
                    .build();
        } else {
            client = builder.client;
        }
    }

    @Override
    @NonNull
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @NonNull
    @Override
    public Uri getUri() {
        return uriBuilder.clearQuery().build();
    }

    @NonNull
    @Override
    public List<RequestResult> sendRequests(@NonNull List<Request> requests) {
        List<Future> futures = new ArrayList<>();
        List<RequestResult> results = new ArrayList<>();

        // Start all requests in the ThreadPool
        for (Request request : requests) {
            String userAgent = request.customUserAgent != null ? request.customUserAgent : DEFAULT_USER_AGENT;

            okhttp3.Request okHttpRequest = httpMethod == HttpMethod.GET
                    ? buildGetRequest(request, userAgent)
                    : buildPostRequest(request, userAgent);

            futures.add(Executor.futureCallable(getRequestCallable(okHttpRequest)));
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

            Request request = requests.get(i);
            List<Long> eventIds = request.emitterEventIds;
            results.add(new RequestResult(code, request.oversize, eventIds));
            if (request.oversize) {
                Logger.track(TAG, "Request is oversized for emitter event IDs: %s", eventIds.toString());
            }
        }
        return results;
    }

    /**
     * Builds an OkHttp GET request which is ready
     * to be executed.
     * @param request The request where to get the payload to be sent.
     * @param userAgent The user-agent used during the transmission to the collector.
     * @return An OkHttp request object.
     */
    private okhttp3.Request buildGetRequest(Request request, String userAgent) {
        // Clear the previous query
        uriBuilder.clearQuery();

        // Build the request query
        HashMap hashMap = (HashMap) request.payload.getMap();

        for (String key : (Iterable<String>) hashMap.keySet()) {
            String value = (String) hashMap.get(key);
            uriBuilder.appendQueryParameter(key, value);
        }

        // Build the request
        String reqUrl = uriBuilder.build().toString();
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(reqUrl)
                .header("User-Agent", userAgent)
                .get();
        if (serverAnonymisation) {
            builder.header("SP-Anonymous", "*");
        }
        return builder.build();
    }

    /**
     * Builds an OkHttp POST request which is ready
     * to be executed.
     * @param request The request where to get the payload to be sent.
     * @param userAgent The user-agent used during the transmission to the collector.
     * @return An OkHttp request object.
     */
    private okhttp3.Request buildPostRequest(Request request, String userAgent) {
        String reqUrl = uriBuilder.build().toString();
        RequestBody reqBody = RequestBody.create(JSON, request.payload.toString());
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(reqUrl)
                .header("User-Agent", userAgent)
                .post(reqBody);
        if (serverAnonymisation) {
            builder.header("SP-Anonymous", "*");
        }
        return builder.build();
    }

    /**
     * Returns a Callable Request Send
     *
     * @param request the request to be
     *                sent
     * @return the new Callable object
     */
    private Callable<Integer> getRequestCallable(final okhttp3.Request request) {
        return () -> requestSender(request);
    }

    /**
     * The function responsible for actually sending
     * the request to the collector.
     *
     * @param request The request to be sent
     * @return a RequestResult
     */
    private int requestSender(okhttp3.Request request) {
        try {
            Logger.v(TAG, "Sending request: %s", request);
            TrafficStats.setThreadStatsTag(TRAFFIC_STATS_TAG);
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
     * Returns truth on if the request
     * was sent successfully.
     *
     * @param code the response code
     * @return the truth as to the success
     */
    private boolean isSuccessfulSend(int code) {
        return code >= 200 && code < 300;
    }
}
