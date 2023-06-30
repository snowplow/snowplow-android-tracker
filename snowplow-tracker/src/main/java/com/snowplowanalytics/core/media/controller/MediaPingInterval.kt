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

package com.snowplowanalytics.core.media.controller

import com.snowplowanalytics.snowplow.media.entity.MediaPlayerEntity
import java.util.*

interface TimerInterface {
    fun schedule(task: TimerTask, delay: Long, period: Long)
    fun cancel()
}

class MediaPingInterval(
    pingInterval: Int? = null,
    maxPausedPings: Int? = null,
    private var createTimer: (() -> TimerInterface)? = null,
) {
    val pingInterval = pingInterval ?: 30

    private var paused: Boolean? = null
    private var numPausedPings: Int = 0
    private val maxPausedPings = maxPausedPings ?: 1
    private val isPaused: Boolean
        get() = paused == true
    private var timer: TimerInterface? = null

    fun update(player: MediaPlayerEntity) {
        paused = player.paused ?: true

        if (paused == false) {
            numPausedPings = 0
        }
    }

    fun subscribe(callback: () -> Unit) {
        timer = this.createTimer?.let { it() } ?: object : TimerInterface {
            private var timer: Timer? = null

            override fun schedule(task: TimerTask, delay: Long, period: Long) {
                timer = Timer()
                timer?.schedule(task, delay, period)
            }

            override fun cancel() {
                timer?.cancel()
            }
        }

        timer?.schedule(
            object : TimerTask() {
                override fun run() {
                    if (!isPaused || numPausedPings < maxPausedPings) {
                        if (isPaused) {
                            numPausedPings += 1
                        }
                        callback()
                    }
                }
            },
            pingInterval * 1000L,
            pingInterval * 1000L
        )
    }

    fun end() {
        timer?.cancel()
        timer = null
    }
}
