package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.tracker.events.Event;

public interface TrackerController extends TrackerConfigurationInterface {

    @NonNull
    String getVersion();
    boolean isTracking();

    @Nullable
    NetworkController getNetwork();
    @Nullable
    SessionController getSession();

    void track(@NonNull Event event);
    void pause();
    void resume();
}
