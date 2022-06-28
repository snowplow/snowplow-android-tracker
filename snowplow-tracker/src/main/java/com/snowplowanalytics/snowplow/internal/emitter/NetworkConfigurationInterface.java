package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.NetworkConnection;
import com.snowplowanalytics.snowplow.network.Protocol;

import okhttp3.CookieJar;
import okhttp3.OkHttpClient;

public interface NetworkConfigurationInterface {

    @Nullable
    String getEndpoint();

    @Nullable
    HttpMethod getMethod();

    @Nullable
    Protocol getProtocol();

    @Nullable
    NetworkConnection getNetworkConnection();

    @Nullable
    String getCustomPostPath();

    @Nullable
    Integer getTimeout();

    @Nullable
    OkHttpClient getOkHttpClient();

    @Nullable
    CookieJar getOkHttpCookieJar();
}
