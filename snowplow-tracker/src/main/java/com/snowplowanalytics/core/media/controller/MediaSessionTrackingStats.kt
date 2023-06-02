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

import com.snowplowanalytics.core.media.entity.MediaSessionEntity
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakType
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import java.util.Date
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private data class Log(
    val time: Date,
    val contentTime: Double,
    val playbackRate: Double,
    val paused: Boolean,
    val muted: Boolean,
    val linearAd: Boolean
)

class MediaSessionTrackingStats(
    var session: MediaSessionEntity,
    private val dateGenerator: () -> Date = { Date() }
) {
    private var lastAdUpdateAt: Date? = null
    private var bufferingStartedAt: Date? = null
    private var bufferingStartTime: Double? = null
    private var playbackDurationWithPlaybackRate: Duration = Duration.ZERO
    private var playedSeconds: MutableSet<Int> = mutableSetOf()
    private var lastLog: Log? = null

    val contentWatched: Duration
        get() = playedSeconds.size.toDuration(DurationUnit.SECONDS)
    var timeSpentAds: Duration = Duration.ZERO
        private set
    var timePlayed: Duration = Duration.ZERO
    var timePlayedMuted: Duration = Duration.ZERO
    var timePaused: Duration = Duration.ZERO
    var timeBuffering: Duration = Duration.ZERO
    val avgPlaybackRate: Double
        get() = if (timePlayed > Duration.ZERO) {
            playbackDurationWithPlaybackRate / timePlayed
        } else {
            1.0
        }
    var adBreaks: Int = 0
    var ads: Int = 0
    var adsSkipped: Int = 0
    var adsClicked: Int = 0

    fun update(event: Event?, player: MediaPlayerEntity, adBreak: MediaAdBreakEntity? = null) {
        val log = Log(
            time = dateGenerator(),
            contentTime = player.currentTime ?: 0.0,
            playbackRate = player.playbackRate ?: 1.0,
            paused = player.paused ?: true,
            muted = player.muted ?: false,
            linearAd = (adBreak?.breakType ?: MediaAdBreakType.Linear) == MediaAdBreakType.Linear
        )

        updateDurationStats(log)
        updateAdStats(event, log)
        updateBufferingStats(event, log)

        lastLog = log
    }

    private fun updateDurationStats(log: Log) {
        val wasPlayingAd = lastAdUpdateAt != null
        val shouldCountStats = !wasPlayingAd || !log.linearAd

        if (!shouldCountStats) {
            return
        }

        lastLog?.let { lastLog ->
            // add the time diff since last event to duration stats
            val duration = timeDiff(lastLog.time, log.time)
            if (lastLog.paused) {
                timePaused += duration
            } else {
                timePlayed += duration
                playbackDurationWithPlaybackRate += duration * lastLog.playbackRate

                if (lastLog.muted) {
                    timePlayedMuted += duration
                }

                if (!log.paused && log.contentTime > lastLog.contentTime) {
                    for (i in lastLog.contentTime.toInt() until log.contentTime.toInt()) {
                        playedSeconds.add(i)
                    }
                }
            }

            if (!log.paused) {
                playedSeconds.add(log.contentTime.toInt())
            }
        }
    }

    private fun updateAdStats(event: Event?, log: Log) {
        // count ad actions
        when (event) {
            is MediaAdBreakStartEvent -> {
                adBreaks++
            }
            is MediaAdStartEvent -> {
                ads++
            }
            is MediaAdSkipEvent -> {
                adsSkipped++
            }
            is MediaAdClickEvent -> {
                adsClicked++
            }
        }

        // update ad playback duration
        when (event) {
            // ad start
            is MediaAdStartEvent, is MediaAdResumeEvent -> {
                if (lastAdUpdateAt == null) {
                    lastAdUpdateAt = log.time
                }
            }

            // ad progress
            is MediaAdClickEvent, is MediaAdFirstQuartileEvent, is MediaAdMidpointEvent, is MediaAdThirdQuartileEvent -> {
                lastAdUpdateAt?.let { lastAdUpdateAt ->
                    timeSpentAds += timeDiff(lastAdUpdateAt, log.time)
                }
                lastAdUpdateAt = log.time
            }

            // ad end
            is MediaAdCompleteEvent, is MediaAdSkipEvent, is MediaAdPauseEvent -> {
                lastAdUpdateAt?.let { lastAdUpdateAt ->
                    timeSpentAds += timeDiff(lastAdUpdateAt, log.time)
                }
                lastAdUpdateAt = null
            }
        }
    }

    private fun updateBufferingStats(event: Event?, log: Log) {
        if (event is MediaBufferStartEvent) {
            bufferingStartedAt = log.time
            bufferingStartTime = log.contentTime
        } else {
            val bufferingStartedAt = bufferingStartedAt ?: return
            val bufferingStartTime = bufferingStartTime ?: return

            if (
                (log.contentTime != bufferingStartTime && !log.paused) ||
                event is MediaBufferEndEvent ||
                event is MediaPlayEvent
            ) {
                // Either the playback moved or BufferEnd or Play events were tracked
                timeBuffering += timeDiff(bufferingStartedAt, log.time)
                this.bufferingStartedAt = null
                this.bufferingStartTime = null
            } else {
                // Still buffering, just update the ongoing duration
                timeBuffering += timeDiff(bufferingStartedAt, log.time)
                this.bufferingStartedAt = log.time
            }
        }
    }

    private fun timeDiff(since: Date, until: Date): Duration {
        return (until.time - since.time).toDuration(DurationUnit.MILLISECONDS)
    }
}
