package com.snowplowanalytics.snowplow.configuration;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import java.util.Objects;

import okhttp3.OkHttpClient;

/**
 * It allows the tracker configuration from the network communication
 * perspective making the tracker able to send events to the Snowplow collector.
 */
public class NetworkConfiguration implements Configuration {

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
            this.endpoint = endpoint;
            return;
        }
        switch (scheme) {
            case "https":
                protocol = Protocol.HTTPS;
                this.endpoint = endpoint.substring(8);
                break;
            case "http":
                protocol = Protocol.HTTP;
                this.endpoint = endpoint.substring(7);
                break;
            default:
                protocol = Protocol.HTTPS;
                this.endpoint = endpoint;
        }
    }

    /**
     * @param networkConnection The NetworkConnection component which will take full ownership of the
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
    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @return Method used to send events to the collector.
     */
    @Nullable
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * @return Protocol used to send events to the collector.
     */
    @Nullable
    public Protocol getProtocol() {
        return protocol;
    }

    // Builder methods

    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when used in pair with the POST method.
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
}
