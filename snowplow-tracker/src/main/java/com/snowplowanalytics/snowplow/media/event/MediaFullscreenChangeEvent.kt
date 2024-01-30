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

package com.snowplowanalytics.snowplow.media.event

import com.snowplowanalytics.core.media.MediaSchemata
import com.snowplowanalytics.core.media.event.MediaPlayerUpdatingEvent
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity

/**
 * Media player event fired immediately after the browser switches into or out of full-screen mode.
 *
 * @param fullscreen Whether the video element is fullscreen after the change.
 */
class MediaFullscreenChangeEvent(
    var fullscreen: Boolean
) : AbstractSelfDescribing(), MediaPlayerUpdatingEvent {
    override val schema: String
        get() = MediaSchemata.eventSchema("fullscreen_change")

    override val dataPayload: Map<String, Any?>
        get() = mapOf(
            "fullscreen" to fullscreen
        )

    override fun update(player: MediaPlayerEntity) {
        player.fullscreen = fullscreen
    }
}
