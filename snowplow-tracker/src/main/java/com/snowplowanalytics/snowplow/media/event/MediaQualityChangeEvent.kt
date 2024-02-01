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

package com.snowplowanalytics.snowplow.media.event

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.core.media.event.MediaPlayerUpdatingEvent
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity

/**
 * Media player event tracked when the video playback quality changes.
 *
 * @param newQuality Quality level after the change (e.g., 1080p).
 * @param previousQuality Quality level before the change (e.g., 1080p). If not set, the previous quality is taken from the last setting in media player.
 * @param bitrate The current bitrate in bits per second.
 * @param framesPerSecond The current number of frames per second.
 * @param automatic Whether the change was automatic or triggered by the user.
 */
class MediaQualityChangeEvent @JvmOverloads constructor (
    var newQuality: String? = null,
    var previousQuality: String? = null,
    var bitrate: Int? = null,
    var framesPerSecond: Int? = null,
    var automatic: Boolean? = null
) : AbstractSelfDescribing(), MediaPlayerUpdatingEvent {
    override val schema: String
        get() = MediaSchemata.eventSchema("quality_change")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "previousQuality" to previousQuality,
            "newQuality" to newQuality,
            "bitrate" to bitrate,
            "framesPerSecond" to framesPerSecond,
            "automatic" to automatic
        ).filterValues { it != null }

    override fun update(player: MediaPlayerEntity) {
        if (previousQuality == null) {
            player.quality?.let { previousQuality = it }
        }
        player.quality = newQuality
    }
}
