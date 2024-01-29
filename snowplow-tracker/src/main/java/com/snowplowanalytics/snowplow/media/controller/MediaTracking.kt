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

package com.snowplowanalytics.snowplow.media.controller

import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdEntity
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity

/**
 * Media tracking instance with methods to track media events.
 */
interface MediaTracking {
    /**
     * Unique identifier for the media tracking instance.
     The same ID is used for media player session if enabled.
     */
    val id: String

    /**
     * Updates stored attributes of the media player such as the current playback.
     * Use this function to continually update the player attributes so that they can be sent in the background ping events.
     *
     * @param player Updates to the properties for the media player context entity attached to media events.
     * @param ad Updates to the properties for the ad context entity attached to media events during ad playback.
     * @param adBreak Updates to the properties for the ad break context entity attached to media events during ad break playback.
     */
    fun update(
        player: MediaPlayerEntity? = null,
        ad: MediaAdEntity? = null,
        adBreak: MediaAdBreakEntity? = null
    )

    /**
     * Tracks a media player event along with the media entities (e.g., player, session, ad).
     *
     * @param event The media player event to track.
     * @param player Updates to the properties for the media player context entity attached to media events.
     * @param ad Updates to the properties for the ad context entity attached to media events during ad playback.
     * @param adBreak Updates to the properties for the ad break context entity attached to media events during ad break playback.
     */
    fun track(
        event: Event,
        player: MediaPlayerEntity? = null,
        ad: MediaAdEntity? = null,
        adBreak: MediaAdBreakEntity? = null
    )
}
