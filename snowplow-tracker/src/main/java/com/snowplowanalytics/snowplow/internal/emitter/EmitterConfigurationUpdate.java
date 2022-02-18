package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.network.RequestCallback;

public class EmitterConfigurationUpdate extends EmitterConfiguration {

    @Nullable
    public EmitterConfiguration sourceConfig;

    public boolean isPaused;

    @Nullable
    public EventStore getEventStore() {
        return (sourceConfig == null) ? null : sourceConfig.eventStore;
    }

    @Nullable
    public RequestCallback getRequestCallback() {
        return (sourceConfig == null) ? null : sourceConfig.requestCallback;
    }

    // bufferOption flag

    public boolean bufferOptionUpdated;

    @NonNull
    public BufferOption getBufferOption() {
        return (sourceConfig == null || bufferOptionUpdated) ? super.bufferOption : sourceConfig.bufferOption;
    }

    // emitRange flag

    public boolean emitRangeUpdated;

    public int getEmitRange() {
        return (sourceConfig == null || emitRangeUpdated) ? super.emitRange : sourceConfig.emitRange;
    }

    // threadPoolSize flag

    public boolean threadPoolSizeUpdated;

    public int getThreadPoolSize() {
        return (sourceConfig == null || threadPoolSizeUpdated) ? super.threadPoolSize : sourceConfig.threadPoolSize;
    }

    // byteLimitGet flag

    public boolean byteLimitGetUpdated;

    public long getByteLimitGet() {
        return (sourceConfig == null || byteLimitGetUpdated) ? super.byteLimitGet : sourceConfig.byteLimitGet;
    }

    // byteLimitPost flag

    public boolean byteLimitPostUpdated;

    public long getByteLimitPost() {
        return (sourceConfig == null || byteLimitPostUpdated) ? super.byteLimitPost : sourceConfig.byteLimitPost;
    }
}
