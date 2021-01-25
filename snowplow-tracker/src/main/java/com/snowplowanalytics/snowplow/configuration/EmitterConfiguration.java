package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.emitter.BufferOption;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;

public class EmitterConfiguration implements Configuration {

    @NonNull
    public BufferOption bufferOption;
    public int emitRange;
    public int threadPoolSize;
    public int byteLimitGet;
    public int byteLimitPost;

    @Nullable
    public RequestCallback requestCallback;

    @Nullable
    public EventStore eventStore;

    // Constructor

    public EmitterConfiguration() {
        bufferOption = BufferOption.Single;
        emitRange = 150;
        threadPoolSize = 15;
        byteLimitGet = 40000;
        byteLimitPost = 40000;
    }

    // Builders

    @NonNull
    public EmitterConfiguration bufferOption(@NonNull BufferOption bufferOption) {
        this.bufferOption = bufferOption;
        return this;
    }

    @NonNull
    public EmitterConfiguration emitRange(int emitRange) {
        this.emitRange = emitRange;
        return this;
    }

    @NonNull
    public EmitterConfiguration threadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        return this;
    }

    @NonNull
    public EmitterConfiguration byteLimitGet(int byteLimitGet) {
        this.byteLimitGet = byteLimitGet;
        return this;
    }

    @NonNull
    public EmitterConfiguration byteLimitPost(int byteLimitPost) {
        this.byteLimitPost = byteLimitPost;
        return this;
    }

    @NonNull
    public EmitterConfiguration eventStore(@Nullable EventStore eventStore) {
        this.eventStore = eventStore;
        return this;
    }

    @NonNull
    public EmitterConfiguration threadPoolSize(@Nullable RequestCallback requestCallback) {
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
