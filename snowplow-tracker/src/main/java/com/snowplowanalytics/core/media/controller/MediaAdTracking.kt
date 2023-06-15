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

package com.snowplowanalytics.core.media.controller

import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdEntity
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

class MediaAdTracking {

    var ad: MediaAdEntity? = null
    var adBreak: MediaAdBreakEntity? = null
    private var podPosition = 0

    val entities: List<SelfDescribingJson>
        get() = listOfNotNull(ad?.entity, adBreak?.entity)

    fun updateForThisEvent(event: Event?, player: MediaPlayerEntity, ad: MediaAdEntity? = null, adBreak: MediaAdBreakEntity? = null) {
        when (event) {
            is MediaAdStartEvent -> {
                this.ad = null
                podPosition += 1
            }
            is MediaAdBreakStartEvent -> {
                this.adBreak = null
                podPosition = 0
            }
        }

        ad?.let {
            this.ad?.update(it)
            this.ad = this.ad ?: it
            if (podPosition > 0) {
                this.ad?.podPosition = podPosition
            }
        }

        adBreak?.let {
            this.adBreak?.update(it)
            this.adBreak = this.adBreak ?: it
            this.adBreak?.update(player)
        }
    }

    fun updateForNextEvent(event: Event?) {
        when (event) {
            is MediaAdBreakEndEvent -> {
                adBreak = null
                podPosition = 0
            }

            is MediaAdCompleteEvent, is MediaAdSkipEvent -> {
                ad = null
            }
        }
    }
}
