package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.network.RequestCallback;

import java.util.Map;

public interface EmitterConfigurationInterface {

    /**
     * Custom component with full ownership for persisting events before to be sent to the collector.
     * If it's not set the tracker will use a SQLite database as default EventStore.
     */
    @Nullable
    EventStore getEventStore();

    /**
     * Whether the buffer should send events instantly or after the buffer
     * has reached it's limit. By default, this is set to BufferOption Default.
     */
    @NonNull
    BufferOption getBufferOption();

    /**
     * Whether the buffer should send events instantly or after the buffer
     * has reached it's limit. By default, this is set to BufferOption Default.
     */
    void setBufferOption(@NonNull BufferOption bufferOption);

    /**
     * Maximum number of events collected from the EventStore to be sent in a request.
     */
    int getEmitRange();

    /**
     * Maximum number of events collected from the EventStore to be sent in a request.
     */
    void setEmitRange(int emitRange);

    /**
     * Maximum number of threads working in parallel in the tracker to send requests.
     */
    int getThreadPoolSize();

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a GET request.
     */
    long getByteLimitGet();

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a GET request.
     */
    void setByteLimitGet(long byteLimitGet);

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a POST request.
     */
    long getByteLimitPost();

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a POST request.
     */
    void setByteLimitPost(long byteLimitPost);

    /**
     * Callback called for each request performed by the tracker to the collector.
     */
    @Nullable
    RequestCallback getRequestCallback();

    /**
     * Callback called for each request performed by the tracker to the collector.
     */
    void setRequestCallback(@Nullable RequestCallback requestCallback);

    /**
     * Custom retry rules for HTTP status codes returned from the Collector.
     * The dictionary is a mapping of integers (status codes) to booleans (true for retry and false for not retry).
     */
    @Nullable
    Map<Integer, Boolean> getCustomRetryForStatusCodes();

    /**
     * Custom retry rules for HTTP status codes returned from the Collector.
     * The dictionary is a mapping of integers (status codes) to booleans (true for retry and false for not retry).
     */
    void setCustomRetryForStatusCodes(@Nullable Map<Integer, Boolean> customRetryForStatusCodes);

    /**
     * Whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    boolean isServerAnonymisation();

    /**
     * Whether to anonymise server-side user identifiers including the `network_userid` and `user_ipaddress`
     */
    void setServerAnonymisation(boolean serverAnonymisation);
}
