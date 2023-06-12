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

package com.snowplowanalytics.snowplow.media.entity

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Properties for the ad break context entity attached to media events during ad break playback.
 * Entity schema: `iglu:com.snowplowanalytics.snowplow.media/ad_break/jsonschema/1-0-0`
 *
 * @param breakId An identifier for the ad break
 * @param name Ad break name (e.g., pre-roll, mid-roll, and post-roll)
 * @param breakType Type of ads within the break
 * @param podSize The number of ads to be played within the ad break
 */
data class MediaAdBreakEntity @JvmOverloads constructor(
    var breakId: String,
    var name: String? = null,
    var breakType: MediaAdBreakType? = null,
    var podSize: Int? = null,
) {
    /**
     * Playback time in seconds at the start of the ad break.
     * Set automatically from the player entity.
     */
    var startTime: Double? = null

    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            MediaSchemata.adBreakSchema,
            mapOf(
                "breakId" to breakId,
                "name" to name,
                "startTime" to startTime,
                "breakType" to breakType?.toString(),
                "podSize" to podSize,
            )
                .filterValues { it != null }
        )

    internal fun update(fromAdBreak: MediaAdBreakEntity) {
        breakId = fromAdBreak.breakId
        fromAdBreak.name?.let { name = it }
        fromAdBreak.breakType?.let { breakType = it }
        fromAdBreak.podSize?.let { podSize = it }
    }

    internal fun update(fromPlayer: MediaPlayerEntity) {
        if (startTime == null) {
            startTime = fromPlayer.currentTime ?: 0.0
        }
    }
}
