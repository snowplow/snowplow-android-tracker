package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationInterface;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.emitter.EventStore;

/**
 * It allows the tracker configuration from the emission perspective.
 * The EmitterConfiguration can be used to setup details about how the tracker should treat the events
 * to emit to the collector.
 */
public class EmitterConfiguration implements Configuration, EmitterConfigurationInterface {

    /**
     * @see #bufferOption(BufferOption)
     */
    @NonNull
    public BufferOption bufferOption;

    /**
     * @see #emitRange(int)
     */
    public int emitRange;

    /**
     * @see #threadPoolSize(int) 
     */
    public int threadPoolSize;

    /**
     * @see #byteLimitGet(int) 
     */
    public long byteLimitGet;

    /**
     * @see #byteLimitPost(int) 
     */
    public long byteLimitPost;

    /**
     * @see #requestCallback(RequestCallback) 
     */
    @Nullable
    public RequestCallback requestCallback;

    /**
     * @see #eventStore(EventStore)
     */
    @Nullable
    public EventStore eventStore;

    // Constructor

    /**
     * It sets a default EmitterConfiguration.
     * Default values:
     *         bufferOption = BufferOption.Single;
     *         emitRange = 150;
     *         threadPoolSize = 15;
     *         byteLimitGet = 40000;
     *         byteLimitPost = 40000;
     */
    public EmitterConfiguration() {
        bufferOption = BufferOption.Single;
        emitRange = 150;
        threadPoolSize = 15;
        byteLimitGet = 40000;
        byteLimitPost = 40000;
    }

    // Getters and Setters

    @Override
    @NonNull
    public BufferOption getBufferOption() {
        return bufferOption;
    }

    @Override
    public void setBufferOption(@NonNull BufferOption bufferOption) {
        this.bufferOption = bufferOption;
    }

    @Override
    public int getEmitRange() {
        return emitRange;
    }

    @Override
    public void setEmitRange(int emitRange) {
        this.emitRange = emitRange;
    }

    @Override
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public long getByteLimitGet() {
        return byteLimitGet;
    }

    @Override
    public void setByteLimitGet(long byteLimitGet) {
        this.byteLimitGet = byteLimitGet;
    }

    @Override
    public long getByteLimitPost() {
        return byteLimitPost;
    }

    @Override
    public void setByteLimitPost(long byteLimitPost) {
        this.byteLimitPost = byteLimitPost;
    }

    @Override
    @Nullable
    public RequestCallback getRequestCallback() {
        return requestCallback;
    }

    @Override
    public void setRequestCallback(@Nullable RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
    }


    // Builders

    /**
     * Sets whether the buffer should send events instantly or after the buffer
     * has reached it's limit. By default, this is set to BufferOption Default.
     */
    @NonNull
    public EmitterConfiguration bufferOption(@NonNull BufferOption bufferOption) {
        this.bufferOption = bufferOption;
        return this;
    }

    /**
     * Maximum number of events collected from the EventStore to be sent in a request.
     */
    @NonNull
    public EmitterConfiguration emitRange(int emitRange) {
        this.emitRange = emitRange;
        return this;
    }

    /**
     * Maximum number of threads working in parallel in the tracker to send requests.
     */
    @NonNull
    public EmitterConfiguration threadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a GET request.
     */
    @NonNull
    public EmitterConfiguration byteLimitGet(int byteLimitGet) {
        this.byteLimitGet = byteLimitGet;
        return this;
    }

    /**
     * Maximum amount of bytes allowed to be sent in a payload in a POST request.
     */
    @NonNull
    public EmitterConfiguration byteLimitPost(int byteLimitPost) {
        this.byteLimitPost = byteLimitPost;
        return this;
    }

    /**
     * Custom component with full ownership for persisting events before to be sent to the collector.
     * If it's not set the tracker will use a SQLite database as default EventStore.
     */
    @NonNull
    public EmitterConfiguration eventStore(@Nullable EventStore eventStore) {
        this.eventStore = eventStore;
        return this;
    }

    /**
     * Callback called for each request performed by the tracker to the collector.
     */
    @NonNull
    public EmitterConfiguration requestCallback(@Nullable RequestCallback requestCallback) {
        this.requestCallback = requestCallback;
        return this;
    }

    // Copyable

    @Override
    @NonNull
    public EmitterConfiguration copy() {
        EmitterConfiguration copy = new EmitterConfiguration();
        copy.bufferOption = bufferOption;
        copy.emitRange = emitRange;
        copy.threadPoolSize = threadPoolSize;
        copy.byteLimitGet = byteLimitGet;
        copy.byteLimitPost = byteLimitPost;
        copy.eventStore = eventStore;
        copy.requestCallback = requestCallback;
        return copy;
    }
}
