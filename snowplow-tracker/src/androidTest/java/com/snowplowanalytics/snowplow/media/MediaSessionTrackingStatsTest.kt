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

package com.snowplowanalytics.snowplow.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.media.controller.MediaSessionTrackingStats
import com.snowplowanalytics.core.media.entity.MediaSessionEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakType
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplow.util.TimeTraveler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
class MediaSessionTrackingStatsTest {

    @Test
    fun calculatesPlayedDuration() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(60.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(Duration.ZERO, stats.timePlayedMuted)
        assertEquals(Duration.ZERO, stats.timePaused)
        assertEquals(1.0, stats.avgPlaybackRate, 0.0)
    }

    @Test
    fun considersPauses() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        player.currentTime = 10.0
        stats.update(event = null, player = player)
        player.paused = true
        stats.update(event = MediaPauseEvent(), player = player)

        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        player.paused = false
        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(50.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePlayedMuted)
        assertEquals(10.toDuration(DurationUnit.SECONDS), stats.timePaused)
        assertEquals(1.0, stats.avgPlaybackRate, 0.0)
    }

    @Test
    fun calculatesPlayOnMute() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        player.muted = true
        stats.update(event = MediaVolumeChangeEvent(newVolume = 50), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(30.toDuration(DurationUnit.SECONDS), stats.timePlayedMuted)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePaused)
        assertEquals(1.0, stats.avgPlaybackRate, 0.0)
    }

    @Test
    fun calculatesAveragePlaybackRate() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        player.playbackRate = 2.0
        stats.update(event = MediaPlaybackRateChangeEvent(newRate = 2.0), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 90.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(91.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePlayedMuted)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePaused)
        assertEquals(1.5, stats.avgPlaybackRate, 0.0)
    }

    @Test
    fun calculatesStatsForLinearAds() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        stats.update(event = MediaAdStartEvent(), player = player)

        timeTraveler.travelBy(5.toDuration(DurationUnit.SECONDS))
        stats.update(event = MediaAdClickEvent(), player = player)

        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        stats.update(event = MediaAdCompleteEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(15.toDuration(DurationUnit.SECONDS), stats.timeSpentAds)
        assertEquals(1, stats.ads)
        assertEquals(1, stats.adsClicked)
        assertEquals(0, stats.adBreaks)
        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePlayedMuted)
        assertEquals(0.toDuration(DurationUnit.SECONDS), stats.timePaused)
        assertEquals(1.0, stats.avgPlaybackRate, 0.0)
    }

    @Test
    fun calculatesStatsForNonLinearAds() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)
        val adBreak = MediaAdBreakEntity(breakId = "1", breakType = MediaAdBreakType.NonLinear)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        stats.update(event = MediaAdBreakStartEvent(), player = player, adBreak = adBreak)
        stats.update(event = MediaAdStartEvent(), player = player, adBreak = adBreak)

        timeTraveler.travelBy(15.toDuration(DurationUnit.SECONDS))
        player.currentTime = 45.0
        stats.update(event = MediaAdCompleteEvent(), player = player, adBreak = adBreak)
        stats.update(event = MediaAdBreakEndEvent(), player = player, adBreak = adBreak)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 75.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(15.toDuration(DurationUnit.SECONDS), stats.timeSpentAds)
        assertEquals(1, stats.ads)
        assertEquals(1, stats.adBreaks)
        assertEquals(76.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(75.toDuration(DurationUnit.SECONDS), stats.timePlayed)
    }

    @Test
    fun countsRewatchedContentOnceInContentWatched() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        stats.update(event = MediaSeekStartEvent(), player = player)
        player.currentTime = 15.0
        stats.update(event = MediaSeekEndEvent(), player = player)

        timeTraveler.travelBy(45.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(75.toDuration(DurationUnit.SECONDS), stats.timePlayed)
    }

    @Test
    fun considersChangesInPingEvents() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaPlayEvent(), player = player)

        for (i in 1 until 60) {
            timeTraveler.travelBy(1.toDuration(DurationUnit.SECONDS))
            player.currentTime = (player.currentTime ?: 0.0) + 1.0
            player.muted = i % 2 == 1
            stats.update(event = null, player = player)
        }

        timeTraveler.travelBy(1.toDuration(DurationUnit.SECONDS))
        player.currentTime = 60.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(61.toDuration(DurationUnit.SECONDS), stats.contentWatched)
        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(30.toDuration(DurationUnit.SECONDS), stats.timePlayedMuted)
    }

    @Test
    fun calculatesBufferingTime() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaBufferStartEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        stats.update(event = MediaBufferEndEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(30.toDuration(DurationUnit.SECONDS), stats.timeBuffering)
    }

    @Test
    fun endsBufferingWhenPlaybackTimeMoves() {
        val (stats, timeTraveler) = createStatsAndTraveler()
        val player = MediaPlayerEntity(paused = false)

        stats.update(event = MediaBufferStartEvent(), player = player)

        timeTraveler.travelBy(30.toDuration(DurationUnit.SECONDS))
        stats.update(event = null, player = player)

        timeTraveler.travelBy(1.toDuration(DurationUnit.SECONDS))
        player.currentTime = 1.0
        stats.update(event = null, player = player)

        timeTraveler.travelBy(29.toDuration(DurationUnit.SECONDS))
        player.currentTime = 30.0
        stats.update(event = MediaEndEvent(), player = player)

        assertEquals(60.toDuration(DurationUnit.SECONDS), stats.timePlayed)
        assertEquals(31.toDuration(DurationUnit.SECONDS), stats.timeBuffering)
    }

    private fun createStatsAndTraveler(): Pair<MediaSessionTrackingStats, TimeTraveler> {
        val timeTraveler = TimeTraveler()
        val session = MediaSessionEntity(id = "1")
        val stats = MediaSessionTrackingStats(
            session = session,
            dateGenerator = { timeTraveler.generateDate() }
        )
        return Pair(stats, timeTraveler)
    }
}
