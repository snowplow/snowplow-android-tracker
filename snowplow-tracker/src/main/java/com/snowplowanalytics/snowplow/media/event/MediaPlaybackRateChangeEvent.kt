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
 * Media player event sent when the playback rate has changed.
 *
 * @param newRate Playback rate after the change (1 is normal).
 * @param previousRate Playback rate before the change (1 is normal). If not set, the previous rate is taken from the last setting in media player.
 */
class MediaPlaybackRateChangeEvent @JvmOverloads constructor (
    var newRate: Double,
    var previousRate: Double? = null
) : AbstractSelfDescribing(), MediaPlayerUpdatingEvent {
    override val schema: String
        get() = MediaSchemata.eventSchema("playback_rate_change")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "previousRate" to previousRate,
            "newRate" to newRate
        ).filterValues { it != null }

    override fun update(player: MediaPlayerEntity) {
        if (previousRate == null) {
            player.playbackRate?.let { previousRate = it }
        }
        player.playbackRate = newRate
    }
}
