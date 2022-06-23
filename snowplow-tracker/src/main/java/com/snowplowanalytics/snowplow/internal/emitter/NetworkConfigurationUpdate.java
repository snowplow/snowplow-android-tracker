package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.Protocol;

import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

public class NetworkConfigurationUpdate implements NetworkConfigurationInterface {

    @Nullable
    public NetworkConfiguration sourceConfig;

    @Nullable
    public String customPostPath;

    // Getters

    @Nullable
    @Override
    public String getEndpoint() {
        return sourceConfig == null ? null : sourceConfig.getEndpoint();
    }

    @Nullable
    @Override
    public HttpMethod getMethod() {
        return sourceConfig == null ? null : sourceConfig.getMethod();
    }

    @Nullable
    @Override
    public Protocol getProtocol() {
        return sourceConfig == null ? null : sourceConfig.getProtocol();
    }

    @Nullable
    @Override
    public NetworkConnection getNetworkConnection() {
        return sourceConfig == null ? null : sourceConfig.getNetworkConnection();
    }

    @Nullable
    @Override
    public Integer getTimeout() {
        return sourceConfig == null ? null : sourceConfig.getTimeout();
    }

    @Nullable
    @Override
    public OkHttpClient getOkHttpClient() {
        return sourceConfig == null ? null : sourceConfig.getOkHttpClient();
    }

    @Nullable
    @Override
    public CookieJar getOkHttpCookieJar() {
        return sourceConfig == null ? null : sourceConfig.getOkHttpCookieJar();
    }

    // customPostPath flag

    public boolean customPostPathUpdated;

    @Nullable
    public String getCustomPostPath() {
        return (sourceConfig == null || customPostPathUpdated) ? this.customPostPath : sourceConfig.customPostPath;
    }
}
