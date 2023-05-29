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

package com.snowplowanalytics.snowplow.media.entity

/**
 * Properties for the media player context entity attached to media events.
 * Entity schema: `iglu:com.snowplowanalytics.snowplow.media/player/jsonschema/1-0-0`
 *
 * @param currentTime The current playback time position within the media in seconds
 * @param duration Duration of the media in seconds
 * @param ended If playback of the media has ended
 * @param fullscreen Whether the video element is fullscreen
 * @param livestream Whether the media is a livestream
 * @param label Human readable name given to tracked media content
 * @param loop If the video should restart after ending
 * @param mediaType Type of media content
 * @param muted If the media element is muted
 * @param paused If the media element is paused
 * @param pictureInPicture Whether the video element is showing picture-in-picture
 * @param playerType Type of the media player (e.g., com.youtube-youtube, com.vimeo-vimeo, org.whatwg-media_element)
 * @param playbackRate Playback rate (1 is normal)
 * @param quality Quality level of the playback (e.g., 1080p, 720p)
 * @param volume Volume percent (0 is muted, 100 is max)
 */
data class MediaPlayer(
    var currentTime: Double? = null,
    var duration: Double? = null,
    var ended: Boolean? = null,
    var fullscreen: Boolean? = null,
    var livestream: Boolean? = null,
    var label: String? = null,
    var loop: Boolean? = null,
    var mediaType: MediaType? = null,
    var muted: Boolean? = null,
    var paused: Boolean? = null,
    var pictureInPicture: Boolean? = null,
    var playerType: String? = null,
    var playbackRate: Double? = null,
    var quality: String? = null,
    var volume: Int? = null
) {
    /** The percent of the way through the media" (0 to 100) */
    val percentProgress: Int?
        get() {
            return duration?.let { duration ->
                return ((currentTime ?: 0.0) / duration * 100).toInt()
            }
        }
}
