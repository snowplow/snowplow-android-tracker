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

package com.snowplowanalytics.snowplow.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.media.controller.MediaAdTracking
import com.snowplowanalytics.snowplow.media.entity.MediaAdBreakEntity
import com.snowplowanalytics.snowplow.media.entity.MediaAdEntity
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.MediaAdBreakStartEvent
import com.snowplowanalytics.snowplow.media.event.MediaAdStartEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaAdTrackingTest {

    @Test
    fun updatesStartTimeOfAdBreak() {
        val adTracking = MediaAdTracking()

        adTracking.updateForThisEvent(
            event = MediaAdBreakStartEvent(),
            player = MediaPlayerEntity(currentTime = 33.0),
            adBreak = MediaAdBreakEntity(breakId = "b1")
        )
        adTracking.updateForNextEvent(
            event = MediaAdBreakStartEvent()
        )

        adTracking.updateForThisEvent(
            event = MediaAdStartEvent(),
            player = MediaPlayerEntity(currentTime = 44.0),
            ad = MediaAdEntity(adId = "a1")
        )
        adTracking.updateForNextEvent(
            event = MediaAdStartEvent()
        )

        assertEquals("b1", adTracking.adBreak?.breakId)
        assertEquals(33.0, adTracking.adBreak?.startTime ?: 0.0, 0.0)
    }

    @Test
    fun updatesPodPositionOfAds() {
        val adTracking = MediaAdTracking()

        adTracking.updateForThisEvent(
            event = MediaAdBreakStartEvent(),
            player = MediaPlayerEntity(),
            adBreak = MediaAdBreakEntity(breakId = "b1")
        )
        adTracking.updateForNextEvent(
            event = MediaAdBreakStartEvent()
        )

        adTracking.updateForThisEvent(
            event = MediaAdStartEvent(),
            player = MediaPlayerEntity(),
            ad = MediaAdEntity(adId = "a1")
        )

        assertEquals(1, adTracking.ad?.podPosition)

        adTracking.updateForNextEvent(
            event = MediaAdStartEvent()
        )

        adTracking.updateForThisEvent(
            event = MediaAdStartEvent(),
            player = MediaPlayerEntity(),
            ad = MediaAdEntity(adId = "a2")
        )

        assertEquals(2, adTracking.ad?.podPosition)
    }
}
