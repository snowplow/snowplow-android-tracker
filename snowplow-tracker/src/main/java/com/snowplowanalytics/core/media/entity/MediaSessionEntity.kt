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

package com.snowplowanalytics.core.media.entity

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.core.media.controller.MediaSessionTrackingStats
import com.snowplowanalytics.core.utils.Util.getDateTimeFromDate
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.Date
import kotlin.time.Duration
import kotlin.time.DurationUnit

class MediaSessionEntity(
    var id: String,
    var startedAt: Date = Date(),
    var pingInterval: Int? = null,
) {
    fun entity(stats: MediaSessionTrackingStats): SelfDescribingJson {
        return SelfDescribingJson(
            schema = MediaSchemata.sessionSchema,
            data = mapOf(
                "mediaSessionId" to id,
                "startedAt" to getDateTimeFromDate(startedAt),
                "pingInterval" to pingInterval,
                "timePlayed" to roundDuration(stats.timePlayed),
                "timePlayed" to roundDuration(stats.timePlayed),
                "timePlayedMuted" to roundDuration(stats.timePlayedMuted),
                "timeSpentAds" to roundDuration(stats.timeSpentAds),
                "timeBuffering" to roundDuration(stats.timeBuffering),
                "ads" to if (stats.ads > 0) { stats.ads } else { null },
                "adBreaks" to if (stats.adBreaks > 0) { stats.adBreaks } else { null },
                "adsSkipped" to if (stats.adsSkipped > 0) { stats.adsSkipped } else { null },
                "adsClicked" to if (stats.adsClicked > 0) { stats.adsClicked } else { null },
                "avgPlaybackRate" to roundStat(stats.avgPlaybackRate),
                "contentWatched" to roundDuration(stats.contentWatched),
            ).filterValues { it != null }
        )
    }

    private fun roundStat(stat: Double?): Double? {
        return stat?.let { return (it * 1000).toInt() / 1000.0 }
    }

    private fun roundDuration(duration: Duration): Double? {
        if (duration > Duration.ZERO) {
            return roundStat(duration.toDouble(DurationUnit.SECONDS))
        } else {
            return null
        }
    }
}
