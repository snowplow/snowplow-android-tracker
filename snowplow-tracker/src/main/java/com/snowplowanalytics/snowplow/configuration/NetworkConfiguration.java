package com.snowplowanalytics.snowplow.configuration;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.emitter.NetworkConfigurationInterface;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import org.json.JSONObject;

import java.util.Objects;

import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

/**
 * Represents the network communication configuration
 * allowing the tracker to be able to send events to the Snowplow collector.
 */
public class NetworkConfiguration implements NetworkConfigurationInterface, Configuration {
    private final static String TAG = NetworkConfiguration.class.getSimpleName();

    @Nullable
    private String endpoint;
    @Nullable
    private HttpMethod method;
    @Nullable
    private Protocol protocol;

    /**
     * @see #NetworkConfiguration(NetworkConnection)
     */
    @Nullable
    public NetworkConnection networkConnection;

    /**
     * @see #customPostPath(String)
     */
    @Nullable
    public String customPostPath;

    /**
     * @see #timeout(Integer)
     */
    @Nullable
    public Integer timeout;

    /**
     * @see #okHttpClient(OkHttpClient)
     */
    @Nullable
    public OkHttpClient okHttpClient;

    /**
     * @see #okHttpCookieJar(CookieJar)
     */
    public CookieJar okHttpCookieJar;

    // Constructors

    /**
     * @param endpoint URL of the collector that is going to receive the events tracked by the tracker.
     *                 The URL can include the schema/protocol (e.g.: `http://collector-url.com`).
     *                 In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     *                 automatically selected.
     */
    public NetworkConfiguration(@NonNull String endpoint) {
        this(endpoint, HttpMethod.POST);
    }

    /**
     * @param endpoint URL of the collector that is going to receive the events tracked by the tracker.
     *                 The URL can include the schema/protocol (e.g.: `http://collector-url.com`).
     *                 In case the URL doesn't include the schema/protocol, the HTTPS protocol is
     *                 automatically selected.
     * @param method The method used to send the requests (GET or POST).
     */
    public NetworkConfiguration(@NonNull String endpoint, @NonNull HttpMethod method) {
        Objects.requireNonNull(method);
        this.method = method;
        Objects.requireNonNull(endpoint);
        Uri uri = Uri.parse(endpoint);
        String scheme = uri.getScheme();
        if (scheme == null) {
            protocol = Protocol.HTTPS;
            this.endpoint = "https://" + endpoint;
            return;
        }
        switch (scheme) {
            case "https":
                protocol = Protocol.HTTPS;
                this.endpoint = endpoint;
                break;
            case "http":
                protocol = Protocol.HTTP;
                this.endpoint = endpoint;
                break;
            default:
                protocol = Protocol.HTTPS;
                this.endpoint = "https://" + endpoint;
        }
    }

    /**
     * @param networkConnection The NetworkConnection component which will control the
     *                          communication between the tracker and the collector.
     */
    public NetworkConfiguration(@NonNull NetworkConnection networkConnection) {
        Objects.requireNonNull(networkConnection);
        this.networkConnection = networkConnection;
    }

    // Getters

    /**
     * @return URL (without schema/protocol) used to send events to the collector.
     */
    @Override
    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @return Method used to send events to the collector.
     */
    @Override
    @Nullable
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @return Protocol used to send events to the collector.
     */
    @Override
    @Nullable
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    @Nullable
    public String getCustomPostPath() {
        return customPostPath;
    }

    @Override
    @Nullable
    public Integer getTimeout() {
        return timeout;
    }

    @Override
    @Nullable
    public NetworkConnection getNetworkConnection() {
        return networkConnection;
    }

    @Override
    @Nullable
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    @Override
    @Nullable
    public CookieJar getOkHttpCookieJar() {
        return okHttpCookieJar;
    }

    // Builder methods

    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when paired with the POST method.
     */
    @NonNull
    public NetworkConfiguration customPostPath(@NonNull String customPostPath) {
        this.customPostPath = customPostPath;
        return this;
    }

    /**
     * The timeout set for the requests to the collector.
     */
    @NonNull
    public NetworkConfiguration timeout(@NonNull Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * An OkHttp client that will be used in the emitter, you can provide your
     * own if you want to share your Singleton client's interceptors, connection pool etc..
     * Otherwise a new one is created.
     */
    @NonNull
    public NetworkConfiguration okHttpClient(@NonNull OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        return this;
    }

    /**
     * An OkHttp cookie jar to override the default cookie jar that stores cookies in SharedPreferences.
     * The cookie jar will be ignored in case custom `okHttpClient` is configured.
     */
    @NonNull
    public NetworkConfiguration okHttpCookieJar(@NonNull CookieJar okHttpCookieJar) {
        this.okHttpCookieJar = okHttpCookieJar;
        return this;
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        NetworkConfiguration copy;
        if (networkConnection != null) {
            copy = new NetworkConfiguration(networkConnection);
        } else {
            Objects.requireNonNull(endpoint);
            Objects.requireNonNull(protocol);
            Objects.requireNonNull(method);
            String scheme = protocol == Protocol.HTTPS ? "https://" : "http://";
            copy = new NetworkConfiguration(scheme + endpoint, method);
        }
        copy.customPostPath = customPostPath;
        copy.timeout = timeout;
        return copy;
    }

    // JSON Formatter

    public NetworkConfiguration(@NonNull JSONObject jsonObject) {
        this("");
        try {
            this.endpoint = jsonObject.getString("endpoint");
            String methodStr = jsonObject.getString("method");
            this.method = HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (Exception e) {
            Logger.e(TAG, "Unable to get remote configuration");
        }
    }
}
