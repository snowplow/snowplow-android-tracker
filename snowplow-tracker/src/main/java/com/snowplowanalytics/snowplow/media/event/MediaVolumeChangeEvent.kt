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

package com.snowplowanalytics.snowplow.media.event

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.core.media.event.MediaPlayerUpdatingEvent
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity

/**
 * Media player event sent when the volume has changed.
 *
 * @param previousVolume Volume percentage before the change. If not set, the previous volume is taken from the last setting in media player.
 * @param newVolume Volume percentage after the change.
 */
class MediaVolumeChangeEvent(
    var previousVolume: Int? = null,
    var newVolume: Int,
) : AbstractSelfDescribing(), MediaPlayerUpdatingEvent {
    override val schema: String
        get() = MediaSchemata.eventSchema("volume_change")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "previousVolume" to previousVolume,
            "newVolume" to newVolume,
        ).filterValues { it != null }

    override fun update(player: MediaPlayerEntity) {
        if (previousVolume == null) {
            player.volume?.let { previousVolume = it }
        }
        player.volume = newVolume
    }
}
