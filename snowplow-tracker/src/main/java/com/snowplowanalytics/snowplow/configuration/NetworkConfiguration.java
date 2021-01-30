package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

import java.util.Objects;

public class NetworkConfiguration implements Configuration {

    @Nullable
    private String endpoint;
    @Nullable
    private HttpMethod method;  // TODO: change the class name in iOS
    @Nullable
    private Protocol protocol;

    @Nullable
    public NetworkConnection networkConnection;

    @Nullable
    public String customPostPath;
    @Nullable
    public Integer timeout;

    // Constructors

    public NetworkConfiguration(@NonNull String endpoint) {
        Objects.requireNonNull(endpoint);
        this.endpoint = endpoint;
        protocol = Protocol.HTTPS;
        method = HttpMethod.POST;
    }

    public NetworkConfiguration(@NonNull String endpoint, @NonNull Protocol protocol, @NonNull HttpMethod method) {
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(protocol);
        Objects.requireNonNull(method);
        this.endpoint = endpoint;
        this.protocol = protocol;
        this.method = method;
    }

    public NetworkConfiguration(@NonNull NetworkConnection networkConnection) {
        Objects.requireNonNull(networkConnection);
        this.networkConnection = networkConnection;
    }

    // Getters

    @Nullable
    public String getEndpoint() {
        return endpoint;
    }

    @Nullable
    public HttpMethod getMethod() {
        return method;
    }

    @Nullable
    public Protocol getProtocol() {
        return protocol;
    }

    // Builder methods

    @NonNull
    public NetworkConfiguration customPostPath(@NonNull String customPostPath) {
        this.customPostPath = customPostPath;
        return this;
    }

    @NonNull
    public NetworkConfiguration timeout (@NonNull Integer timeout) {
        this.timeout = timeout;
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
            copy = new NetworkConfiguration(endpoint, protocol, method);
        }
        copy.customPostPath = customPostPath;
        copy.timeout = timeout;
        return copy;
    }
}
