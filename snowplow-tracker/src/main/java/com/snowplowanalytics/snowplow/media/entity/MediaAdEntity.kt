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

package com.snowplowanalytics.snowplow.media.entity

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Properties for the ad context entity attached to media events during ad playback.
 * Entity schema: `iglu:com.snowplowanalytics.snowplow.media/ad/jsonschema/1-0-0`.
 *
 * @param adId Unique identifier for the ad.
 * @param name Friendly name of the ad.
 * @param creativeId The ID of the ad creative.
 * @param podPosition The position of the ad within the ad break, starting with 1. It is automatically assigned by the tracker based on the tracked ad break start and ad start events.
 * @param duration Length of the video ad in seconds.
 * @param skippable Indicating whether skip controls are made available to the end user.
 */
data class MediaAdEntity @JvmOverloads constructor(
    var adId: String,
    var name: String? = null,
    var creativeId: String? = null,
    var podPosition: Int? = null,
    var duration: Double? = null,
    var skippable: Boolean? = null,
) {
    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            MediaSchemata.adSchema,
            mapOf(
                "adId" to adId,
                "name" to name,
                "creativeId" to creativeId,
                "podPosition" to podPosition,
                "duration" to duration,
                "skippable" to skippable,
            )
                .filterValues { it != null }
        )

    internal fun update(fromAd: MediaAdEntity) {
        adId = fromAd.adId
        fromAd.name?.let { name = it }
        fromAd.creativeId?.let { creativeId = it }
        fromAd.podPosition?.let { podPosition = it }
        fromAd.duration?.let { duration = it }
        fromAd.skippable?.let { skippable = it }
    }
}
