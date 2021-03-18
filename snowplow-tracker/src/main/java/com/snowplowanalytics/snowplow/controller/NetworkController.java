package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;

public interface NetworkController {

    /**
     * URL (without schema/protocol) used to send events to the collector.
     */
    void setEndpoint(@NonNull String endpoint);

    /**
     * URL (without schema/protocol) used to send events to the collector.
     */
    @NonNull
    String getEndpoint();

    /**
     * Method used to send events to the collector.
     */
    void setMethod(@NonNull HttpMethod method);

    /**
     * Method used to send events to the collector.
     */
    @NonNull
    HttpMethod getMethod();

    /**
     * Protocol used to send events to the collector.
     */
    void setProtocol(@NonNull Protocol protocol);

    /**
     * Protocol used to send events to the collector.
     */
    @NonNull
    Protocol getProtocol();

    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when used in pair with the POST method.
     */
    void setCustomPostPath(@Nullable String customPostPath);

    /**
     * A custom path which will be added to the endpoint URL to specify the
     * complete URL of the collector when used in pair with the POST method.
     */
    @Nullable
    String getCustomPostPath();

    /**
     * The timeout set for the requests to the collector.
     */
    void setTimeout(int timeout);

    /**
     * The timeout set for the requests to the collector.
     */
    int getTimeout();
}
