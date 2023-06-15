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
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing

/**
 * Media player event fired when 75% of ad is reached after continuous ad playback at normal speed.
 */
class MediaAdThirdQuartileEvent : AbstractSelfDescribing() {
    override val schema: String
        get() = MediaSchemata.eventSchema("ad_quartile")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "percentProgress" to 75,
        )
}
