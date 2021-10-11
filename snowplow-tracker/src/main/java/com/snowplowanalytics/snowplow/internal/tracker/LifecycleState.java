package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.Nullable;

public class LifecycleState implements State {

    public final boolean isForeground;

    @Nullable
    public final Integer index;

    public LifecycleState(boolean isForeground, @Nullable Integer index) {
        this.isForeground = isForeground;
        this.index = index;
    }
}
