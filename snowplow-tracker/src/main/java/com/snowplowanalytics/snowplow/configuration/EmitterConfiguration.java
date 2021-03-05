package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.RequestCallback;
import com.snowplowanalytics.snowplow.emitter.EventStore;

public class EmitterConfiguration implements Configuration, com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationInterface {

    @NonNull
    public BufferOption bufferOption;
    public int emitRange;
    public int threadPoolSize;
    public long byteLimitGet;
    public long byteLimitPost;

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
