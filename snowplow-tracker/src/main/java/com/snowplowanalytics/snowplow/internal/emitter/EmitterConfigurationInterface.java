package com.snowplowanalytics.snowplow.internal.emitter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.network.RequestCallback;

public interface EmitterConfigurationInterface {
    @NonNull
    BufferOption getBufferOption();

    void setBufferOption(@NonNull BufferOption bufferOption);

    int getEmitRange();

    void setEmitRange(int emitRange);

    int getThreadPoolSize();

    long getByteLimitGet();

    void setByteLimitGet(long byteLimitGet);

    long getByteLimitPost();

    void setByteLimitPost(long byteLimitPost);

    @Nullable
    RequestCallback getRequestCallback();

    void setRequestCallback(@Nullable RequestCallback requestCallback);
}
