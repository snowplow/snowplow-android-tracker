/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.tracker.TrackerConfigurationInterface
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.controller.MediaController
import java.util.*

/**
 * Controller for managing the tracker.
 */
interface TrackerController : TrackerConfigurationInterface {
    /** Version of the tracker.  */
    val version: String

    /**
     * Whether the tracker is running and able to collect/send events.
     * @see [pause] and [resume]
     */
    val isTracking: Boolean

    /**
     * Namespace of the tracker.
     * It is used to identify the tracker when there are multiple trackers running in the same app.
     */
    val namespace: String
    
    // Controllers
    
    /**
     * NetworkController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val network: NetworkController?

    /**
     * SessionController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val session: SessionController?

    /**
     * EmitterController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val emitter: EmitterController

    /**
     * SubjectController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val subject: SubjectController

    /**
     * GdprController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val gdpr: GdprController

    /**
     * GlobalContextsController.
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val globalContexts: GlobalContextsController

    /**
     * Controller for managing tracker plugins
     * Note: don't retain the reference. It may change on tracker reconfiguration.
     */
    val plugins: PluginsController

    /**
     * Media controller for managing media tracking instances and tracking media events.
     */
    val media: MediaController
    
    // Methods
    
    /**
     * Track the event.
     * The tracker will process and send the event.
     * 
     * @param event The event to track.
     * @return The event's unique ID, or null when tracking is paused
     */
    fun track(event: Event): UUID?

    /**
     * Pause the tracker.
     * The tracker will stop any new activity tracking, but will continue to send any remaining events
     * already tracked but not yet sent.
     * Calling [track] will not have any effect, and the event tracked will be lost.
     */
    fun pause()

    /**
     * Resume the tracker.
     * The tracker will start tracking again.
     */
    fun resume()
}
