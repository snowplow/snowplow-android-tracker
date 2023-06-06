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

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.View
import android.widget.MediaController
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.media.controller.MediaTracking
import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import com.snowplowanalytics.snowplow.media.event.*
import com.snowplowanalytics.snowplowdemokotlin.R
import java.util.UUID

class VideoPlayer(activity: Activity) {
    private val mediaController: MediaController = MediaController(activity)
    private val videoView: VideoView = activity.findViewById<View>(R.id.videoView) as VideoView
    private var videoLoaded = false
    private var seeking = false
    private val audio: AudioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var clock: Clock? = null
    private var mediaTracking: MediaTracking? = null
    private val player: MediaPlayerEntity
        get() = MediaPlayerEntity(
            currentTime = videoView.currentPosition.toDouble() / 1000,
            duration = videoView.duration.toDouble() / 1000,
            paused = !videoView.isPlaying || seeking,
            volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        )

    init {
        videoView.setVideoPlayer(this)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
        videoView.requestFocus()
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaController.show(0)
            mediaPlayer.setOnSeekCompleteListener {
                seeking = false
                track(MediaSeekEndEvent())
            }
        }
        videoView.setOnInfoListener { _, what, _ ->
            when (what) {
                MEDIA_INFO_BUFFERING_START -> {
                    track(MediaBufferStartEvent())
                }
                MEDIA_INFO_BUFFERING_END -> {
                    track(MediaBufferEndEvent())
                }
            }
            true
        }
        videoView.setOnCompletionListener {
            mediaController.show(0)
            completeVideo()
        }
    }

    fun destroy() {
        clock?.quit()
        clock = null
    }

    fun loadContent(uri: Uri?) {
        if (videoLoaded) {
            unloadVideo()
        }
        videoView.setVideoURI(uri)
    }

    fun resumePlayback() {
        openVideoIfNecessary()
        videoView
        track(MediaPlayEvent())
    }

    fun pausePlayback() {
        track(MediaPauseEvent())
    }

    fun seekStart() {
        openVideoIfNecessary()
        seeking = true
        track(MediaSeekStartEvent())
    }

    private fun openVideoIfNecessary() {
        if (!videoLoaded) {
            resetInternalState()
            startVideo()
            clock = Clock()
        }
    }

    private fun completeVideo() {
        if (videoLoaded) {
            track(MediaEndEvent())
            unloadVideo()
        }
    }

    private fun unloadVideo() {
        mediaTracking?.id?.let {
            Snowplow.defaultTracker?.media?.endMediaTracking(it)
        }
        mediaTracking = null
        clock?.invalidate()
        resetInternalState()
    }

    private fun resetInternalState() {
        videoLoaded = false
        seeking = false
        clock?.quit()
        clock = null
    }

    private fun startVideo() {
        videoLoaded = true

        mediaTracking = Snowplow.defaultTracker?.media?.startMediaTracking(
            id = UUID.randomUUID().toString(),
            player = player
        )

        track(MediaReadyEvent())
    }

    private fun track(event: Event) {
        Log.v(TAG, "Tracking media event: $event")
        mediaTracking?.track(event, player = player)
    }

    private inner class Clock : HandlerThread("VideoPlayerClock") {
        private var handler: Handler? = null
        private var shouldStop = false

        init {
            start()
            val looper = looper
            if (looper == null) {
                Log.e(TAG, "Unable to obtain looper thread.")
            } else {
                handler = Handler(getLooper())
                handler?.let { handler ->
                    handler.post(object : Runnable {
                        override fun run() {
                            if (!shouldStop) {
                                mediaTracking?.update(player = player)
                                handler.postDelayed(this, CHECK_INTERVAL)
                            }
                        }
                    })
                }
            }
        }

        fun invalidate() {
            shouldStop = true
        }
    }

    companion object {
        private val TAG = VideoPlayer::class.java.simpleName

        private const val CHECK_INTERVAL = 1000L
    }
}

