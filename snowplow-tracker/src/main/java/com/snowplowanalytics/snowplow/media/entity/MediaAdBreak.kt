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

/**
 * Properties for the ad break context entity attached to media events during ad break playback.
 * Entity schema: `iglu:com.snowplowanalytics.snowplow.media/ad_break/jsonschema/1-0-0`
 *
 * @param name Ad break name (e.g., pre-roll, mid-roll, and post-roll)
 * @param breakId An identifier for the ad break
 * @param startTime Playback time in seconds at the start of the ad break
 * @param breakType Type of ads within the break
 * @param podSize The number of ads to be played within the ad break
 */
data class MediaAdBreak(
    var name: String? = null,
    var breakId: String,
    var startTime: Double? = null,
    var breakType: MediaAdBreakType? = null,
    var podSize: Int? = null,
) {
    constructor(breakId: String) : this(name = null, breakId = breakId)
}
