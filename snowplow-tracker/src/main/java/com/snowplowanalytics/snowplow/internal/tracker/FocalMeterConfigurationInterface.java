package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.Nullable;

public interface FocalMeterConfigurationInterface {
    @Nullable
    String getKantarEndpoint();

    void setKantarEndpoint(@Nullable String kantarEndpoint);
}
