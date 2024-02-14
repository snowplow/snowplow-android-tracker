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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.emitter.Executor
import com.snowplowanalytics.core.media.MediaSchemata.eventSchema
import com.snowplowanalytics.core.media.MediaSchemata.playerSchema
import com.snowplowanalytics.core.media.MediaSchemata.sessionSchema
import com.snowplowanalytics.core.media.controller.MediaPingInterval
import com.snowplowanalytics.core.media.controller.MediaSessionTracking
import com.snowplowanalytics.core.media.controller.MediaTrackingImpl
import com.snowplowanalytics.core.media.controller.TimerInterface
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.Snowplow.removeAllTrackers
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.SelfDescribing
import com.snowplowanalytics.snowplow.media.configuration.MediaTrackingConfiguration
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import com.snowplowanalytics.snowplow.util.TimeTraveler
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
class TestMediaController {

    private val trackedEvents: MutableList<InspectableEvent> = mutableListOf()
    private val firstEvent: InspectableEvent
        get() = trackedEvents.first()
    private val firstPlayer: Map<*, *>?
        get() = firstEvent.entities.find { it.map["schema"] == playerSchema }?.map?.get("data") as? Map<*, *>
    private val secondPlayer: Map<*, *>?
        get() = trackedEvents[1].entities.find { it.map["schema"] == playerSchema }?.map?.get("data") as? Map<*, *>
    private val firstSession: Map<*, *>?
        get() = firstEvent.entities.find { it.map["schema"] == sessionSchema }?.map?.get("data") as? Map<*, *>
    private var tracker: TrackerController? = null

    @Before
    fun setUp() {
        tracker = createTracker()
    }

    @After
    fun tearDown() {
        tracker?.media?.endMediaTracking("media1")
        tracker?.pause()
        tracker = null
        removeAllTrackers()
        trackedEvents.clear()
        Executor.shutdown()
    }

    // --- MEDIA PLAYER EVENT TESTS

    @Test
    fun setsPausedToFalseWhenPlayEventIsTracked() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(paused = true)
        )
        media?.track(MediaPlayEvent())

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertEquals(eventSchema("play"), firstEvent.schema)
        assertFalse(firstPlayer?.get("paused") as Boolean)
    }

    @Test
    fun setsPausedAndEndedToTrueWhenEndIsTracked() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(paused = false)
        )
        media?.track(MediaEndEvent())

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertEquals(eventSchema("end"), firstEvent.schema)
        assertEquals(true, firstPlayer?.get("paused"))
        assertEquals(true, firstPlayer?.get("ended"))
    }

    @Test
    fun testDoesntTrackSeekStartMultipleTimes() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(event = MediaSeekStartEvent(), player = MediaPlayerEntity(currentTime = 1.0))
        media?.track(event = MediaSeekStartEvent(), player = MediaPlayerEntity(currentTime = 2.0))
        media?.track(event = MediaSeekEndEvent(), player = MediaPlayerEntity(currentTime = 2.0))
        media?.track(event = MediaSeekStartEvent(), player = MediaPlayerEntity(currentTime = 3.0))

        Thread.sleep(100)

        assertEquals(3, trackedEvents.size)
        assertEquals(2, trackedEvents.filter { it.schema == eventSchema("seek_start") }.size)
    }

    @Test
    fun doesntTrackEventsExcludedFromCaptureEvents() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            captureEvents = listOf(MediaPlayEvent::class)
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPlayEvent())
        media?.track(MediaPauseEvent())

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertEquals(eventSchema("play"), firstEvent.schema)
    }

    @Test
    fun addsEntitiesFromConfigToEvents() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            entities = listOf(
                SelfDescribingJson("iglu:com.acme/track_type/jsonschema/1-0-0", mapOf("type" to "video"))
            )
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPlayEvent())

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertNotNull(
            firstEvent.entities.find {
                it.map["schema"] == "iglu:com.acme/track_type/jsonschema/1-0-0"
            }
        )
        assertNotNull(firstPlayer)
    }

    @Test
    fun addsEntitiesTrackedWithEvent() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(
            event = MediaPlayEvent()
                .entities(
                    listOf(
                        SelfDescribingJson("iglu:com.acme/track_type/jsonschema/1-0-0", mapOf("type" to "video"))
                    )
                )
        )

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertNotNull(
            firstEvent.entities.find {
                it.map["schema"] == "iglu:com.acme/track_type/jsonschema/1-0-0"
            }
        )
        assertNotNull(firstPlayer)
    }

    @Test
    fun trackingPlaybackRateChangeEventUpdatesThePlaybackRate() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(playbackRate = 0.5)
        )

        media?.track(MediaPlaybackRateChangeEvent(newRate = 1.5))
        media?.track(MediaPauseEvent())

        Thread.sleep(100)

        assertEquals(2, trackedEvents.size)
        val rateEvent = trackedEvents.find { it.schema == eventSchema("playback_rate_change") }
        assertEquals(0.5, rateEvent?.payload?.get("previousRate"))
        assertEquals(1.5, rateEvent?.payload?.get("newRate"))
        assertEquals(1.5, firstPlayer?.get("playbackRate"))
        assertEquals(1.5, secondPlayer?.get("playbackRate"))
    }

    @Test
    fun trackingVolumeChangeEventUpdatesTheVolume() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(volume = 80)
        )

        media?.track(MediaVolumeChangeEvent(newVolume = 90))
        media?.track(MediaPauseEvent())

        Thread.sleep(100)

        assertEquals(2, trackedEvents.size)
        val volumeEvent = trackedEvents.find { it.schema == eventSchema("volume_change") }
        assertEquals(80, volumeEvent?.payload?.get("previousVolume"))
        assertEquals(90, volumeEvent?.payload?.get("newVolume"))
        assertEquals(90, firstPlayer?.get("volume"))
        assertEquals(90, secondPlayer?.get("volume"))
    }

    @Test
    fun trackingFullscreenChangeEventUpdatesFullscreenInPlayer() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(fullscreen = false)
        )

        media?.track(MediaFullscreenChangeEvent(fullscreen = true))

        Thread.sleep(100)

        assertEquals(true, firstEvent.payload.get("fullscreen"))
        assertEquals(true, firstPlayer?.get("fullscreen"))
    }

    @Test
    fun trackingPictureInPictureChangeEventUpdatesPictureInPictureInPlayer() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(pictureInPicture = false)
        )

        media?.track(MediaPictureInPictureChangeEvent(pictureInPicture = true))

        Thread.sleep(100)

        assertEquals(true, firstEvent.payload.get("pictureInPicture"))
        assertEquals(true, firstPlayer?.get("pictureInPicture"))
    }

    @Test
    fun trackingAdFirstQuartileSetsPercentProgress() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaAdFirstQuartileEvent())

        Thread.sleep(100)

        assertEquals(25, firstEvent.payload.get("percentProgress"))
    }

    @Test
    fun trackingAdMidpointSetsPercentProgress() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaAdMidpointEvent())

        Thread.sleep(100)

        assertEquals(50, firstEvent.payload.get("percentProgress"))
    }

    @Test
    fun trackingAdThirdQuartileSetsPercentProgress() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaAdThirdQuartileEvent())

        Thread.sleep(100)

        assertEquals(75, firstEvent.payload.get("percentProgress"))
    }

    @Test
    fun addsPercentProgressPropertyToAdEvents() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaAdClickEvent(percentProgress = 15))
        media?.track(MediaAdSkipEvent(percentProgress = 30))
        media?.track(MediaAdResumeEvent(percentProgress = 40))
        media?.track(MediaAdPauseEvent(percentProgress = 50))

        Thread.sleep(100)

        val adClickEvent = trackedEvents.find { it.schema == eventSchema("ad_click") }
        assertEquals(15, adClickEvent?.payload?.get("percentProgress"))
        val adSkipEvent = trackedEvents.find { it.schema == eventSchema("ad_skip") }
        assertEquals(30, adSkipEvent?.payload?.get("percentProgress"))
        val adResumeEvent = trackedEvents.find { it.schema == eventSchema("ad_resume") }
        assertEquals(40, adResumeEvent?.payload?.get("percentProgress"))
        val adPauseEvent = trackedEvents.find { it.schema == eventSchema("ad_pause") }
        assertEquals(50, adPauseEvent?.payload?.get("percentProgress"))
    }

    @Test
    fun setsQualityPropertiesInQualityChangeEvent() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(quality = "720p")
        )

        media?.track(MediaQualityChangeEvent(
            newQuality = "1080p",
            bitrate = 3333,
            framesPerSecond = 60
        ))

        Thread.sleep(100)

        assertEquals("720p", firstEvent.payload.get("previousQuality"))
        assertEquals("1080p", firstEvent.payload.get("newQuality"))
        assertEquals(3333, firstEvent.payload.get("bitrate"))
        assertEquals(60, firstEvent.payload.get("framesPerSecond"))
    }

    @Test
    fun setsPropertiesOfErrorEvent() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaErrorEvent(
            errorCode = "501",
            errorName = "forbidden",
            errorDescription = "Failed to load media"
        ))

        Thread.sleep(100)

        assertEquals("501", firstEvent.payload.get("errorCode"))
        assertEquals("forbidden", firstEvent.payload.get("errorName"))
        assertEquals("Failed to load media", firstEvent.payload.get("errorDescription"))
    }

    @Test
    fun tracksCustomEvent() {
        val media = tracker?.media?.startMediaTracking(
            id = "media1",
            player = MediaPlayerEntity(label = "Video")
        )

        media?.track(SelfDescribing(
            "iglu:com.acme/video_played/jsonschema/1-0-0",
            mapOf("url" to "https://www.youtube.com/watch?v=12345")
        ))

        Thread.sleep(100)

        assertEquals("iglu:com.acme/video_played/jsonschema/1-0-0", firstEvent.schema)
        assertEquals("Video", firstPlayer?.get("label"))
    }

    // --- SESSION

    @Test
    fun addsSessionContextEntityWithinGivenID() {
        val media = tracker?.media?.startMediaTracking(id = "media1")

        media?.track(MediaPlayEvent())

        Thread.sleep(100)

        assertEquals("media1", firstSession?.get("mediaSessionId"))
    }

    @Test
    fun calculatesSessionStats() {
        val timeTraveler = TimeTraveler()
        val session = MediaSessionTracking(
            id = "media1",
            dateGenerator = { timeTraveler.generateDate() },
        )
        val media = MediaTrackingImpl(
            id = "media1",
            tracker = tracker!!,
            player = MediaPlayerEntity(duration = 10.0),
            session = session,
        )

        media.track(MediaPlayEvent())
        timeTraveler.travelBy(10.toDuration(DurationUnit.SECONDS))
        media.update(player = MediaPlayerEntity(currentTime = 10.0))
        media.track(MediaEndEvent())

        Thread.sleep(100)

        val endEvent = trackedEvents.find { it.schema == eventSchema("end") }
        val lastSession = endEvent?.entities?.find { it.map.get("schema") == sessionSchema }?.map?.get("data") as? Map<*, *>
        assertNotNull(lastSession)
        assertEquals(10.0, lastSession?.get("timePlayed"))
        assertEquals(11.0, lastSession?.get("contentWatched"))
    }

    // --- PING EVENTS

    @Test
    fun startsSendingPingEventsAfterSessionStarts() {
        val timer = createTimer()
        val pingInterval = MediaPingInterval(
            pingInterval = 10,
            createTimer = { timer },
        )
        MediaTrackingImpl(
            id = "media1",
            tracker = tracker!!,
            pingInterval = pingInterval,
        )

        timer.fire()

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
        assertEquals(10000L, timer.delay)
        assertEquals(10000L, timer.period)
        assertEquals(eventSchema("ping"), firstEvent.schema)
    }

    @Test
    fun shouldSendPingEventsRegardlessOfOtherEvents() {
        val timer = createTimer()
        val pingInterval = MediaPingInterval(
            createTimer = { timer },
        )
        val media = MediaTrackingImpl(
            id = "media1",
            tracker = tracker!!,
            pingInterval = pingInterval,
        )

        media.track(MediaPlayEvent())
        timer.fire()
        media.track(MediaPauseEvent())
        timer.fire()

        Thread.sleep(100)

        assertEquals(4, trackedEvents.size)
    }

    @Test
    fun shouldStopSendingPingEventsWhenPaused() {
        val timer = createTimer()
        val pingInterval = MediaPingInterval(
            createTimer = { timer },
            maxPausedPings = 2
        )
        val media = MediaTrackingImpl(
            id = "media1",
            tracker = tracker!!,
            pingInterval = pingInterval,
        )

        media.update(player = MediaPlayerEntity(paused = true))
        for (i in 0 until 5) {
            timer.fire()
        }

        Thread.sleep(100)

        assertEquals(2, trackedEvents.size)
    }

    @Test
    fun shouldNotStopSendingPingEventsWhenPlaying() {
        val timer = createTimer()
        val pingInterval = MediaPingInterval(
            createTimer = { timer },
            maxPausedPings = 2
        )
        val media = MediaTrackingImpl(
            id = "media1",
            tracker = tracker!!,
            pingInterval = pingInterval,
        )

        media.update(player = MediaPlayerEntity(paused = false))
        for (i in 0 until 5) {
            timer.fire()
        }

        Thread.sleep(100)

        assertEquals(5, trackedEvents.size)
    }

    // --- PERCENT PROGRESS

    @Test
    fun shouldSendProgressEventsWhenBoundariesReached() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            player = MediaPlayerEntity(duration = 100.0),
            boundaries = listOf(10, 50, 90),
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPlayEvent())
        for (i in 1 until 100) {
            media?.update(player = MediaPlayerEntity(currentTime = i.toDouble()))
        }

        Thread.sleep(100)

        assertEquals(4, trackedEvents.size)
        assertEquals(3, trackedEvents.filter { it.schema == eventSchema("percent_progress") }.size)
    }

    @Test
    fun progressEventShouldHavePercentValue() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            player = MediaPlayerEntity(duration = 100.0),
            boundaries = listOf(50),
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPlayEvent())
        for (i in 1 until 60) {
            media?.update(player = MediaPlayerEntity(currentTime = i.toDouble()))
        }

        Thread.sleep(100)

        assertEquals(2, trackedEvents.size)
        
        val progressEvents = trackedEvents.filter { it.schema == eventSchema("percent_progress") }
        assertEquals(1, progressEvents.size)
        assertEquals(50, progressEvents[0].payload["percentProgress"])
    }

    @Test
    fun doesntSendProgressEventsIfPaused() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            player = MediaPlayerEntity(duration = 100.0),
            boundaries = listOf(10, 50, 90),
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPauseEvent())
        for (i in 1 until 100) {
            media?.update(player = MediaPlayerEntity(currentTime = i.toDouble()))
        }

        Thread.sleep(100)

        assertEquals(1, trackedEvents.size)
    }

    @Test
    fun doesntSendProgressEventMultipleTimes() {
        val configuration = MediaTrackingConfiguration(
            id = "media1",
            player = MediaPlayerEntity(duration = 100.0),
            boundaries = listOf(10, 50, 90),
        )
        val media = tracker?.media?.startMediaTracking(configuration = configuration)

        media?.track(MediaPlayEvent())
        for (i in 1 until 100) {
            media?.update(player = MediaPlayerEntity(currentTime = i.toDouble()))
        }

        media?.track(MediaSeekStartEvent(), player = MediaPlayerEntity(currentTime = 0.0))
        for (i in 1 until 100) {
            media?.update(player = MediaPlayerEntity(currentTime = i.toDouble()))
        }

        Thread.sleep(100)

        assertEquals(5, trackedEvents.size)
        assertEquals(3, trackedEvents.filter { it.schema == eventSchema("percent_progress") }.size)
    }

    // --- PRIVATE
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun createTracker(): TrackerController {
        val namespace = "ns" + Math.random().toString()
        val networkConfig = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))
        val trackerConfig = TrackerConfiguration("appId")
            .installAutotracking(false)
            .lifecycleAutotracking(false)

        val plugin = PluginConfiguration("plugin")
        plugin.afterTrack {
            if (namespace == this.tracker?.namespace) {
                trackedEvents.add(it)
            }
        }

        return createTracker(
            context,
            namespace = namespace,
            network = networkConfig,
            trackerConfig,
            plugin
        )
    }

    private fun createTimer(): TestTimerInterface {
        return object : TestTimerInterface {
            private var task: TimerTask? = null
            override var delay: Long? = null
            override var period: Long? = null

            override fun schedule(task: TimerTask, delay: Long, period: Long) {
                this.task = task
                this.delay = delay
                this.period = period
            }

            override fun cancel() {
                throw NotImplementedError()
            }

            override fun fire() {
                task?.run()
            }
        }
    }
}

interface TestTimerInterface : TimerInterface {
    var delay: Long?
    var period: Long?
    fun fire()
}
