package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

public class TrackerState implements TrackerStateSnapshot {

    private HashMap<String, StateFuture> trackerState = new HashMap<>();

    public synchronized void put(@NonNull String stateIdentifier, @NonNull StateFuture state) {
        trackerState.put(stateIdentifier, state);
    }

    @Nullable
    public synchronized StateFuture getStateFuture(@NonNull String stateIdentifier) {
        return trackerState.get(stateIdentifier);
    }

    public void removeState(@NonNull String stateIdentifier) {
        trackerState.remove(stateIdentifier);
    }

    @NonNull
    public synchronized TrackerStateSnapshot getSnapshot() {
        TrackerState newTrackerState = new TrackerState();
        newTrackerState.trackerState = new HashMap<>(trackerState);
        return newTrackerState;
    }

    // Implements TrackerStateSnapshot

    @Nullable
    @Override
    public State getState(@NonNull String stateIdentifier) {
        StateFuture stateFuture = getStateFuture(stateIdentifier);
        if (stateFuture == null) {
            return null;
        }
        return stateFuture.getState();
    }
}
