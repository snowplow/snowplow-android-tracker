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

    /**
     * Controller for managing tracker plugins
     * @apiNote Don't retain the reference. It may change on tracker reconfiguration.
     */
    val plugins: PluginsController
    
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
