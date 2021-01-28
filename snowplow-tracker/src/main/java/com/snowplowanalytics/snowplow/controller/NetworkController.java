package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.Protocol;

public interface NetworkController {

    void setEndpoint(@NonNull String endpoint);

    @NonNull
    String getEndpoint();

    void setMethod(@NonNull HttpMethod method);

    @NonNull
    HttpMethod getMethod();

    void setProtocol(@NonNull Protocol protocol);

    @NonNull
    Protocol getProtocol();

    void setCustomPostPath(@Nullable String customPostPath);

    @Nullable String getCustomPostPath();

    void setTimeout(int timeout);

    int getTimeout();
}
