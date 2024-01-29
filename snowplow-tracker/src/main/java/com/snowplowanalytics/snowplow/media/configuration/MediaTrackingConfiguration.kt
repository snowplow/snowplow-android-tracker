/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.media.configuration

import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import kotlin.reflect.KClass

/**
 * Configuration for a `MediaTracking` instance.
 *
 * @param id Unique identifier for the media tracking instance. The same ID is used for media player session if enabled.
 * @param player Properties for the media player context entity attached to media events.
 * @param pings Whether to track media ping events. Defaults to true.
 * @param pingInterval Interval in seconds in which the media ping events are tracked. Defaults to 30 seconds unless `pings` are disabled.
 * @param maxPausedPings Maximum number of consecutive ping events to send when playback is paused. Defaults to 1 unless `pings` are disabled.
 * @param session Whether to track the media player session context entity along with media events. Defaults to true. The session entity contain the `id` identifier as well as statistics about the media playback.
 * @param boundaries Percentage boundaries of the video to track percent progress events at.
 * @param entities Additional context entities to attach to media events.
 * @param captureEvents List of event types to allow tracking. If not specified (`null`), all tracked events will be allowed and tracked. Otherwise, tracked event types not present in the list will be discarded.
 */
data class MediaTrackingConfiguration @JvmOverloads constructor(
    val id: String,
    var player: MediaPlayerEntity? = null,
    var pings: Boolean = true,
    var pingInterval: Int? = null,
    var maxPausedPings: Int? = null,
    var session: Boolean = true,
    var boundaries: List<Int>? = null,
    var entities: List<SelfDescribingJson>? = null,
    var captureEvents: List<KClass<*>>? = null,
    ) {
    fun setCaptureEvents(captureEvents: List<Class<*>>?): MediaTrackingConfiguration {
        this.captureEvents = captureEvents?.map { it.kotlin }
        return this
    }
}
