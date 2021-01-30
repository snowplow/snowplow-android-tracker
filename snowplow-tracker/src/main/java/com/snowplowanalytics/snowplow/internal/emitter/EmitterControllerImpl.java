package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.EmitterController;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.RequestCallback;

public class EmitterControllerImpl implements EmitterController {

    @NonNull
    private final Emitter emitter;

    public EmitterControllerImpl(@NonNull Emitter emitter) {
        this.emitter = emitter;
    }

    // Getters and Setters

    @NonNull
    @Override
    public BufferOption getBufferOption() {
        return emitter.getBufferOption();
    }

    @Override
    public void setBufferOption(@NonNull BufferOption bufferOption) {
        emitter.setBufferOption(bufferOption);
    }

    @Override
    public int getEmitRange() {
        return emitter.getSendLimit();
    }

    @Override
    public void setEmitRange(int emitRange) {
        emitter.setSendLimit(emitRange);
    }

    @Override
    public int getThreadPoolSize() {
        return Executor.getThreadCount();
    }

    @Override
    public long getByteLimitGet() {
        return emitter.getByteLimitGet();
    }

    @Override
    public void setByteLimitGet(long byteLimitGet) {
        emitter.setByteLimitGet(byteLimitGet);
    }

    @Override
    public long getByteLimitPost() {
        return emitter.getByteLimitPost();
    }

    @Override
    public void setByteLimitPost(long byteLimitPost) {
        emitter.setByteLimitPost(byteLimitPost);
    }

    @Nullable
    @Override
    public RequestCallback getRequestCallback() {
        return emitter.getRequestCallback();
    }

    @Override
    public void setRequestCallback(@Nullable RequestCallback requestCallback) {

    }

    @Override
    public long getDbCount() {
        return emitter.getEventStore().getSize();
    }

    @Override
    public boolean isSending() {
        return emitter.getEmitterStatus();
    }
}
