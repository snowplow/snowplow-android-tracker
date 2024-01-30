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
import com.snowplowanalytics.core.utils.Util.getDateTimeFromDate
import com.snowplowanalytics.snowplow.media.entity.*
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplow.util.TimeTraveler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
class MediaEventAndEntitySerializationTest {

    @Test
    fun schemaForMediaEventTypes() {
        assertEquals(mediaSchema("play_event"), MediaPlayEvent().schema)
        assertEquals(mediaSchema("playback_rate_change_event"), MediaPlaybackRateChangeEvent(newRate = 1.0).schema)
        assertEquals(mediaSchema("ready_event"), MediaReadyEvent().schema)
        assertEquals(mediaSchema("ad_resume_event"), MediaAdResumeEvent().schema)

        assertEquals(mediaSchema("ad_quartile_event"), MediaAdFirstQuartileEvent().schema)
        assertEquals(mediaSchema("ad_quartile_event"), MediaAdMidpointEvent().schema)
        assertEquals(mediaSchema("ad_quartile_event"), MediaAdThirdQuartileEvent().schema)
        assertEquals(mediaSchema("ad_complete_event"), MediaAdCompleteEvent().schema)
    }

    @Test
    fun buildsEntityWithDefaultValuesForEmptyMediaPlayer() {
        val entity = MediaPlayerEntity().entity

        assertEquals(mediaPlayerSchema, entity.map["schema"] as? String)
        assertEquals(0.0, (entity.map["data"] as? Map<*, *>)?.get("currentTime"))
        assertEquals(true, (entity.map["data"] as? Map<*, *>)?.get("paused"))
        assertEquals(false, (entity.map["data"] as? Map<*, *>)?.get("ended"))
    }

    @Test
    fun buildsEntityForMediaPlayer() {
        val entity = MediaPlayerEntity(
            currentTime = 33.3,
            duration = 100.0,
            ended = true,
            fullscreen = true,
            livestream = true,
            label = "The Video",
            loop = true,
            mediaType = MediaType.Video,
            muted = true,
            paused = false,
            pictureInPicture = false,
            playerType = "AVPlayer",
            playbackRate = 2.5,
            quality = "1080p",
            volume = 80,
        )

        assertEquals(mediaPlayerSchema, entity.entity.map["schema"] as? String)
        val data = entity.entity.map["data"] as? Map<*, *>
        assertEquals(33.3, data?.get("currentTime"))
        assertEquals(100.0, data?.get("duration"))
        assertEquals(true, data?.get("ended"))
        assertEquals(true, data?.get("fullscreen"))
        assertEquals(true, data?.get("livestream"))
        assertEquals("The Video", data?.get("label"))
        assertEquals(true, data?.get("loop"))
        assertEquals("video", data?.get("mediaType"))
        assertEquals(true, data?.get("muted"))
        assertEquals(false, data?.get("paused"))
        assertEquals(false, data?.get("pictureInPicture"))
        assertEquals("AVPlayer", data?.get("playerType"))
        assertEquals(2.5, data?.get("playbackRate"))
        assertEquals("1080p", data?.get("quality"))
        assertEquals(80, data?.get("volume"))
    }

    @Test
    fun buildsMediaSessionEntity() {
        val date = Date()
        val timeTraveler = TimeTraveler()
        val session = MediaSessionEntity(id = "xxx", startedAt = date, pingInterval = 13)
        val stats = MediaSessionTrackingStats(
            session = session,
            dateGenerator = { timeTraveler.generateDate() },
        )

        stats.update(
            event = MediaPlayEvent(),
            player = MediaPlayerEntity(currentTime = 0.0, paused = false),
        )
        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        stats.update(
            event = MediaPauseEvent(),
            player = MediaPlayerEntity(currentTime = 10.0, paused = true),
        )

        val entity = session.entity(stats)
        assertEquals(mediaSchema("session"), entity.map["schema"] as? String)
        val data = entity.map["data"] as? Map<*, *>
        assertEquals("xxx", data?.get("mediaSessionId"))
        assertEquals(getDateTimeFromDate(date), data?.get("startedAt"))
        assertEquals(13, data?.get("pingInterval"))
        assertEquals(10.0, data?.get("timePlayed"))
        assertFalse(data?.containsKey("timePaused") ?: true)
    }

    @Test
    fun buildsAdEntity() {
        val ad = MediaAdEntity(
            name = "Name",
            adId = "yyy",
            creativeId = "zzz",
            duration = 11.0,
            podPosition = 2,
            skippable = true
        )
        val entity = ad.entity

        assertEquals(mediaSchema("ad"), entity.map["schema"] as? String)
        val data = entity.map["data"] as? Map<*, *>
        assertEquals("Name", data?.get("name"))
        assertEquals("yyy", data?.get("adId"))
        assertEquals("zzz", data?.get("creativeId"))
        assertEquals(11.0, data?.get("duration"))
        assertEquals(2, data?.get("podPosition"))
        assertEquals(true, data?.get("skippable"))
    }

    @Test
    fun buildsAdBreakEntity() {
        val adBreak = MediaAdBreakEntity(
            breakId = "xxx",
            name = "Break 1",
            breakType = MediaAdBreakType.NonLinear,
            podSize = 3
        )
        adBreak.startTime = 100.1
        val entity = adBreak.entity

        assertEquals(mediaSchema("ad_break"), entity.map["schema"] as? String)
        val data = entity.map["data"] as? Map<*, *>
        assertEquals("xxx", data?.get("breakId"))
        assertEquals("Break 1", data?.get("name"))
        assertEquals("nonlinear", data?.get("breakType"))
        assertEquals(3, data?.get("podSize"))
        assertEquals(100.1, data?.get("startTime"))
    }

    private fun mediaSchema(name: String, version: String = "1-0-0"): String {
        return "iglu:com.snowplowanalytics.snowplow.media/" + name + "/jsonschema/" + version
    }

    private val mediaPlayerSchema: String
        get() = "iglu:com.snowplowanalytics.snowplow/media_player/jsonschema/2-0-0"
}
