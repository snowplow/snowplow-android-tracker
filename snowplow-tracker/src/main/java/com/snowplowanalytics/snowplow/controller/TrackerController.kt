package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.tracker.TrackerConfigurationInterface
import com.snowplowanalytics.snowplow.event.Event
import java.util.*

interface TrackerController : TrackerConfigurationInterface {
    /** Version of the tracker.  */
    val version: String

    /**
     * Whether the tracker is running and able to collect/send events.
     * @see #pause and #resume
     */
    val isTracking: Boolean

    /**
     * Namespace of the tracker.
     * It is used to identify the tracker among multiple trackers running in the same app.
     */
    val namespace: String
    
    // Controllers
    /**
     * NetworkController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val network: NetworkController?

    /**
     * SessionController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val session: SessionController?

    /**
     * EmitterController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val emitter: EmitterController

    /**
     * SubjectController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val subject: SubjectController

    /**
     * GdprController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val gdpr: GdprController

    /**
     * GlobalContextsController.
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val globalContexts: GlobalContextsController
    
    // Methods
    /**
     * Track the event.
     * The tracker will take care to process and send the event assigning `event_id` and `device_timestamp`.
     * @param event The event to track.
     * @return The event ID or null in case tracking is paused
     */
    fun track(event: Event): UUID?

    /**
     * Pause the tracker.
     * The tracker will stop any new activity tracking but it will continue to send remaining events
     * already tracked but not sent yet.
     * Calling a track method will not have any effect and event tracked will be lost.
     */
    fun pause()

    /**
     * Resume the tracker.
     * The tracker will start tracking again.
     */
    fun resume()
}
