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

import com.snowplowanalytics.core.media.event.MediaPercentProgressEvent
import com.snowplowanalytics.core.media.event.MediaPingEvent
import com.snowplowanalytics.core.media.event.MediaPlayerUpdatingEvent
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.controller.MediaTracking
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdEntity
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.MediaSeekEndEvent
import com.snowplowanalytics.snowplow.media.event.MediaSeekStartEvent
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import kotlin.reflect.KClass

class MediaTrackingImpl (
    override val id: String,
    private val tracker: TrackerController,
    player: MediaPlayerEntity? = null,
    private var session: MediaSessionTracking? = null,
    private var pingInterval: MediaPingInterval? = null,
    private var boundaries: List<Int>? = null,
    private var captureEvents: List<KClass<*>>? = null,
    private var customEntities: List<SelfDescribingJson>? = null,
) : MediaTracking {
    private var player: MediaPlayerEntity = MediaPlayerEntity()
    private val adTracking: MediaAdTracking = MediaAdTracking()
    private val sentBoundaries: MutableList<Int> = mutableListOf()
    private var seeking = false

    private val entities: List<SelfDescribingJson>
        get() = listOfNotNull(
            player.entity,
            session?.entity,
        ) + adTracking.entities + (customEntities ?: emptyList())

    init {
        player?.let { this.player.update(it) }

        pingInterval?.subscribe { track(MediaPingEvent()) }
    }

    fun end() {
        pingInterval?.end()
    }

    override fun update(
        player: MediaPlayerEntity?,
        ad: MediaAdEntity?,
        adBreak: MediaAdBreakEntity?
    ) {
        updateAndTrack(null, player, ad, adBreak)
    }

    override fun track(
        event: Event,
        player: MediaPlayerEntity?,
        ad: MediaAdEntity?,
        adBreak: MediaAdBreakEntity?
    ) {
        updateAndTrack(event, player, ad, adBreak)
    }

    private fun updateAndTrack(
        event: Event?,
        player: MediaPlayerEntity?,
        ad: MediaAdEntity?,
        adBreak: MediaAdBreakEntity?
    ) {
        synchronized(this) {
            // update state
            player?.let { this.player.update(it) }
            (event as? MediaPlayerUpdatingEvent)?.update(this.player)
            adTracking.updateForThisEvent(
                event = event,
                player = this.player,
                ad = ad,
                adBreak = adBreak
            )
            session?.update(
                event = event,
                player = this.player,
                adBreak = adBreak
            )
            pingInterval?.update(this.player)

            // track events
            event?.let { addEntitiesAndTrack(it) }
            if (shouldSendPercentProgressEvent()) {
                addEntitiesAndTrack(MediaPercentProgressEvent(this.player.percentProgress))
            }

            // update state for events after this one
            adTracking.updateForNextEvent(event)
        }
    }

    private fun addEntitiesAndTrack(event: Event) {
        if (!shouldTrackEvent(event)) { return }

        event.entities.addAll(entities)

        tracker.track(event)
    }

    private fun shouldSendPercentProgressEvent(): Boolean {
        if (player.paused ?: true) {
            return false
        }

        val boundaries = boundaries ?: return false
        val percentProgress = player.percentProgress ?: return false

        val achievedBoundaries = boundaries.filter { it <= percentProgress }
        if (achievedBoundaries.isEmpty()) { return false }

        val boundary = achievedBoundaries.max()
        if (sentBoundaries.contains(boundary)) { return false }

        sentBoundaries.add(boundary)
        return true
    }

    private fun shouldTrackEvent(event: Event): Boolean {
        if (event is MediaSeekStartEvent) {
            if (seeking) {
                return false
            }
            seeking = true
        } else if (event is MediaSeekEndEvent) {
            seeking = false
        }

        val captureEvents = captureEvents ?: return true
        return captureEvents.contains(event::class)
    }
}
