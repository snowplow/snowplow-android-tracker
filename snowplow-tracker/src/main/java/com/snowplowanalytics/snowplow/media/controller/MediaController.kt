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

import com.snowplowanalytics.snowplow.media.configuration.MediaTrackingConfiguration
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity

/**
 * Controller for managing media tracking instances and tracking media events.
 */
interface MediaController {
    /**
     * Starts media tracking for a single media content tracked in a media player.
     *
     * @param id Unique identifier for the media tracking instance. The same ID will be used for media player session if enabled.
     * @param player Properties for the media player context entity attached to media events.
     */
    fun startMediaTracking(id: String, player: MediaPlayerEntity? = null): MediaTracking

    /**
     * Starts media tracking for a single media content tracked in a media player.
     *
     * @param configuration Configuration for the media tracking instance.
     */
    fun startMediaTracking(configuration: MediaTrackingConfiguration): MediaTracking

    /**
     * Returns a media tracking instance for the given ID.
     *
     * @param id Unique identifier for the media tracking instance.
     */
    fun getMediaTracking(id: String): MediaTracking?

    /**
     * Ends autotracked events and cleans the media tracking instance.
     *
     * @param id Unique identifier for the media tracking instance.
     */
    fun endMediaTracking(id: String)
}
