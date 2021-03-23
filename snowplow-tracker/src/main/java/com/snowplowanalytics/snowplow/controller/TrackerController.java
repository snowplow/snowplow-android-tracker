package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.event.Event;

public interface TrackerController extends TrackerConfigurationInterface {

    @NonNull
    String getVersion();
    boolean isTracking();
    @NonNull
    String getNamespace();

    @Nullable
    NetworkController getNetwork();
    @Nullable
    SessionController getSession();
    @NonNull
    EmitterController getEmitter();
    @NonNull
    GdprController getGdpr();
    @NonNull
    GlobalContextsController getGlobalContexts();

    void track(@NonNull Event event);
    void pause();
    void resume();
}
