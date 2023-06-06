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

package com.snowplowanalytics.snowplowdemokotlin.media

import android.content.Context
import android.util.AttributeSet

class VideoView : android.widget.VideoView {
    private var _player: VideoPlayer? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun setVideoPlayer(player: VideoPlayer?) {
        _player = player
    }

    override fun start() {
        super.start()
        _player?.resumePlayback()
    }

    override fun pause() {
        super.pause()
        _player?.pausePlayback()
    }

    override fun seekTo(msec: Int) {
        super.seekTo(msec)
        _player?.seekStart()
    }
}

