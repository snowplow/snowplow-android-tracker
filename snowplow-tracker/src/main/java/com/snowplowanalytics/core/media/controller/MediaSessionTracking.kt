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

package com.snowplowanalytics.core.media.controller

import com.snowplowanalytics.core.media.entity.MediaSessionEntity
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

class MediaSessionTracking(
    id: String,
    startedAt: Date? = null,
    pingInterval: Int? = null,
    dateGenerator: () -> Date = { Date() }
) {
    var session: MediaSessionEntity
    var stats: MediaSessionTrackingStats

    val entity: SelfDescribingJson
        get() = session.entity(stats)

    init {
        session = MediaSessionEntity(id = id, startedAt = startedAt ?: Date(), pingInterval = pingInterval)
        stats = MediaSessionTrackingStats(session = session, dateGenerator = dateGenerator)
    }

    fun update(event: Event?, player: MediaPlayerEntity, adBreak: MediaAdBreakEntity?) {
        stats.update(event, player, adBreak)
    }
}
