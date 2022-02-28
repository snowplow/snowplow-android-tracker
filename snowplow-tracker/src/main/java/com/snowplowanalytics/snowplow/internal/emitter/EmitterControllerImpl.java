package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.EmitterController;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;
import com.snowplowanalytics.snowplow.network.RequestCallback;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EmitterControllerImpl extends Controller implements EmitterController {
    private final static String TAG = EmitterControllerImpl.class.getSimpleName();

    public EmitterControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    private Emitter getEmitter() {
        return serviceProvider.getTracker().getEmitter();
    }

    // Getters and Setters

    @Nullable
    @Override
    public EventStore getEventStore() {
        return getEmitter().getEventStore();
    }

    @NonNull
    @Override
    public BufferOption getBufferOption() {
        return getEmitter().getBufferOption();
    }

    @Override
    public void setBufferOption(@NonNull BufferOption bufferOption) {
        getDirtyConfig().bufferOption = bufferOption;
        getDirtyConfig().bufferOptionUpdated = true;
        getEmitter().setBufferOption(bufferOption);
    }

    @Override
    public int getEmitRange() {
        return getEmitter().getSendLimit();
    }

    @Override
    public void setEmitRange(int emitRange) {
        getDirtyConfig().emitRange = emitRange;
        getDirtyConfig().emitRangeUpdated = true;
        getEmitter().setSendLimit(emitRange);
    }

    @Override
    public int getThreadPoolSize() {
        return Executor.getThreadCount();
    }

    @Override
    public long getByteLimitGet() {
        return getEmitter().getByteLimitGet();
    }

    @Override
    public void setByteLimitGet(long byteLimitGet) {
        getDirtyConfig().byteLimitGet = byteLimitGet;
        getDirtyConfig().byteLimitGetUpdated = true;
        getEmitter().setByteLimitGet(byteLimitGet);
    }

    @Override
    public long getByteLimitPost() {
        return getEmitter().getByteLimitPost();
    }

    @Override
    public void setByteLimitPost(long byteLimitPost) {
        getDirtyConfig().byteLimitPost = byteLimitPost;
        getDirtyConfig().byteLimitPostUpdated = true;
        getEmitter().setByteLimitPost(byteLimitPost);
    }

    @Nullable
    @Override
    public RequestCallback getRequestCallback() {
        return getEmitter().getRequestCallback();
    }

    @Override
    public void setRequestCallback(@Nullable RequestCallback requestCallback) {
        getEmitter().setRequestCallback(requestCallback);
    }

    @Override
    public long getDbCount() {
        EventStore eventStore = getEmitter().getEventStore();
        if (eventStore == null) {
            Logger.e(TAG,"EventStore not available in the Emitter.");
            return -1;
        }
        return eventStore.getSize();
    }

    @Override
    public boolean isSending() {
        return getEmitter().getEmitterStatus();
    }

    @Override
    public void pause() {
        getDirtyConfig().isPaused = true;
        getEmitter().pauseEmit();
    }

    @Override
    public void resume() {
        getDirtyConfig().isPaused = false;
        getEmitter().resumeEmit();
    }

    // Private methods

    @NonNull
    public EmitterConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getEmitterConfigurationUpdate();
    }
}
