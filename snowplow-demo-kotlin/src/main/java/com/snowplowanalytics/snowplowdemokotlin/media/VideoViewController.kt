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

package com.snowplowanalytics.snowplowdemokotlin.media

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
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
import java.util.*

class VideoViewController(activity: Activity, uri: Uri) {
    private val videoView: VideoView = activity.findViewById<View>(R.id.videoView) as VideoView
    private val mediaController: MediaController = MediaController(activity)
    private var loaded = false
    private var seeking = false
    private val audio: AudioManager = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var updateThread: UpdateThread? = null
    private var mediaTracking: MediaTracking? = null

    /**
     * Converts the volume to percentage.
     */
    private val volume: Int
        get() {
            val volumeLevel = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolumeLevel = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            return (volumeLevel.toFloat() / maxVolumeLevel * 100).toInt()
        }

    /**
     * Constructs the player entity using information from the videoView.
     */
    private val player: MediaPlayerEntity
        get() = MediaPlayerEntity(
            currentTime = videoView.currentPosition.toDouble() / 1000,
            duration = videoView.duration.toDouble() / 1000,
            paused = !videoView.isPlaying || seeking,
            volume = volume
        )

    init {
        // initialize video view
        videoView.setVideoPlayer(this)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
        videoView.requestFocus()

        // subscribe listeners
        videoView.setOnPreparedListener { onPrepared(it) }
        videoView.setOnInfoListener { _, what, _ -> onInfo(what); true }
        videoView.setOnCompletionListener { onComplete(it) }

        videoView.setVideoURI(uri)
    }

    fun onPlay() {
        load()
        track(MediaPlayEvent())
    }

    fun onPause() {
        track(MediaPauseEvent())
    }

    fun onSeekStart() {
        load()
        seeking = true
        track(MediaSeekStartEvent())
    }

    private fun onSeekEnd() {
        seeking = false
        track(MediaSeekEndEvent())
    }

    private fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaController.show(0)
        mediaPlayer.setOnSeekCompleteListener { onSeekEnd() }
    }

    private fun onInfo(what: Int) {
        when (what) {
            MEDIA_INFO_BUFFERING_START -> track(MediaBufferStartEvent())
            MEDIA_INFO_BUFFERING_END -> track(MediaBufferEndEvent())
        }
    }

    private fun onComplete(player: MediaPlayer) {
        mediaController.show(0)

        if (loaded) {
            track(MediaEndEvent())
            mediaTracking?.id?.let { Snowplow.defaultTracker?.media?.endMediaTracking(it) }
            updateThread?.invalidate()
            reset()
        }
    }

    fun destroy() {
        updateThread?.quit()
        updateThread = null
    }

    private fun load() {
        if (!loaded) {
            reset()
            loaded = true

            mediaTracking = Snowplow.defaultTracker?.media?.startMediaTracking(
                id = UUID.randomUUID().toString(),
                player = player
            )

            updateThread = UpdateThread()

            track(MediaReadyEvent())
        }
    }

    private fun reset() {
        loaded = false
        seeking = false
        updateThread?.quit()
        updateThread = null
        mediaTracking = null
    }

    private fun track(event: Event) {
        Log.v(TAG, "Tracking media event: $event")
        mediaTracking?.track(event, player = player)
    }

    private inner class UpdateThread : HandlerThread("UpdatePlayerThread") {
        private var shouldStop = false

        init {
            start()
            val handler = Handler(looper)
            handler.post(object : Runnable {
                override fun run() {
                    if (shouldStop) { return }
                    mediaTracking?.update(player = player)
                    handler.postDelayed(this, 1000L)
                }
            })
        }

        fun invalidate() {
            shouldStop = true
        }
    }

    companion object {
        private val TAG = VideoViewController::class.java.simpleName
    }
}
