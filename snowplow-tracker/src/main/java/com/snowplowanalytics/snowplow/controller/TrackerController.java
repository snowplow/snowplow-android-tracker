package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.event.Event;

import java.util.UUID;

public interface TrackerController extends TrackerConfigurationInterface {

    /** Version of the tracker. */
    @NonNull
    String getVersion();

    /**
     * Whether the tracker is running and able to collect/send events.
     * @see {@link #pause()} and {@link #resume()}
     */
    boolean isTracking();

    /**
     * Namespace of the tracker.
     * It is used to identify the tracker among multiple trackers running in the same app.
     */
    @NonNull
    String getNamespace();

    // Controllers

    /**
     * NetworkController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @Nullable
    NetworkController getNetwork();

    /**
     * SessionController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @Nullable
    SessionController getSession();

    /**
     * EmitterController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @NonNull
    EmitterController getEmitter();

    /**
     * SubjectController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @NonNull
    SubjectController getSubject();

    /**
     * GdprController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @NonNull
    GdprController getGdpr();

    /**
     * GlobalContextsController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    @NonNull
    GlobalContextsController getGlobalContexts();

    // Methods

    /**
     * Track the event.
     * The tracker will take care to process and send the event assigning `event_id` and `device_timestamp`.
     * @param event The event to track.
     * @return The event ID or null in case tracking is paused
     */
    UUID track(@NonNull Event event);

    /**
     * Pause the tracker.
     * The tracker will stop any new activity tracking but it will continue to send remaining events
     * already tracked but not sent yet.
     * Calling a track method will not have any effect and event tracked will be lost.
     */
    void pause();

    /**
     * Resume the tracker.
     * The tracker will start tracking again.
     */
    void resume();
}
