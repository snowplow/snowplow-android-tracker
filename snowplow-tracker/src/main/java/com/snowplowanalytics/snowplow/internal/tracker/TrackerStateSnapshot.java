package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface TrackerStateSnapshot {

    @Nullable
    State getState(@NonNull String stateIdentifier);

}
