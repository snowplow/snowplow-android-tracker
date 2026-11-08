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

package com.snowplowanalytics.core.signals

import android.os.Handler
import android.os.Looper
import com.snowplowanalytics.core.session.AppStateProvider
import com.snowplowanalytics.core.tracker.Logger
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException

/**
 * Keeps a Server-Sent Events subscription to the Signals interventions endpoint open while it is
 * enabled (handlers are registered), the app is in the foreground, and both session identifiers
 * are known. Resubscribes when the identifiers change and reconnects with exponential backoff when
 * the stream fails or closes.
 *
 * The stream is read on OkHttp's dispatcher threads, so it doesn't hold a tracker executor thread.
 * Messages are passed to [onMessage] on that thread.
 */
internal class InterventionStream(
    private val clientFactory: () -> OkHttpClient?,
    private val urlFactory: (userId: String, sessionId: String) -> HttpUrl?,
    private val onMessage: (String) -> Unit,
    private val scheduler: Handler = Handler(Looper.getMainLooper())
) {
    private val lock = Any()

    private var isEnabled = false
    private var isForeground = AppStateProvider.isForeground
    private var userId: String? = null
    private var sessionId: String? = null

    private var call: Call? = null
    private var connectedUrl: HttpUrl? = null
    private var pendingReconnect: Runnable? = null
    private var generation = 0
    private var retryAttempt = 0

    /** Whether both session identifiers are known. */
    val hasIdentifiers: Boolean
        get() = synchronized(lock) { !userId.isNullOrEmpty() && !sessionId.isNullOrEmpty() }

    fun setEnabled(enabled: Boolean) = synchronized(lock) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        refresh()
    }

    fun setIdentifiers(userId: String?, sessionId: String?) = synchronized(lock) {
        if (this.userId == userId && this.sessionId == sessionId) return
        this.userId = userId
        this.sessionId = sessionId
        refresh()
    }

    fun setForeground(foreground: Boolean) = synchronized(lock) {
        if (isForeground == foreground) return
        isForeground = foreground
        if (foreground) retryAttempt = 0
        refresh()
    }

    /** Closes the current connection and opens a new one if it should be open. */
    fun restart() = synchronized(lock) {
        disconnect()
        retryAttempt = 0
        refresh()
    }

    fun close() = synchronized(lock) {
        isEnabled = false
        disconnect()
    }

    // Must hold the lock.
    private fun refresh() {
        val url = desiredUrl()
        val isOpenOrPending = call != null || pendingReconnect != null
        if (url == null) {
            if (isOpenOrPending) {
                Logger.d(TAG, "Unsubscribing from interventions")
                disconnect()
            }
            return
        }
        if (url == connectedUrl && isOpenOrPending) return
        connect(url)
    }

    // Must hold the lock.
    private fun desiredUrl(): HttpUrl? {
        if (!isEnabled || !isForeground) return null
        val userId = userId
        val sessionId = sessionId
        if (userId.isNullOrEmpty() || sessionId.isNullOrEmpty() || userId == ANONYMOUS_USER_ID) {
            return null
        }
        return urlFactory(userId, sessionId)
    }

    // Must hold the lock.
    private fun connect(url: HttpUrl) {
        disconnect()
        val client = clientFactory() ?: return
        val connectionGeneration = generation
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        val newCall = client.newCall(request)
        call = newCall
        connectedUrl = url
        Logger.d(TAG, "Subscribing to interventions")

        newCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) return
                Logger.e(TAG, "Interventions stream failed: %s", e.message)
                scheduleReconnect(connectionGeneration)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Logger.e(TAG, "Interventions stream failed with status %s", response.code)
                        scheduleReconnect(connectionGeneration)
                        return
                    }
                    synchronized(lock) {
                        if (connectionGeneration == generation) retryAttempt = 0
                    }
                    try {
                        readEvents(response.body.source(), connectionGeneration)
                    } catch (e: IOException) {
                        if (!call.isCanceled()) {
                            Logger.e(TAG, "Interventions stream interrupted: %s", e.message)
                        }
                    }
                }
                if (!call.isCanceled()) {
                    scheduleReconnect(connectionGeneration)
                }
            }
        })
    }

    /** Reads `data:` fields and passes each complete event's data to [onMessage]. */
    private fun readEvents(source: BufferedSource, connectionGeneration: Int) {
        val data = StringBuilder()
        var hasData = false
        while (true) {
            val line = source.readUtf8Line() ?: return
            if (!isCurrent(connectionGeneration)) return
            if (line.isEmpty()) {
                if (hasData) deliver(data.toString())
                data.setLength(0)
                hasData = false
                continue
            }
            if (line.startsWith(":")) continue // comment or keep-alive

            val separator = line.indexOf(':')
            val field = if (separator < 0) line else line.substring(0, separator)
            if (field != "data") continue
            var value = if (separator < 0) "" else line.substring(separator + 1)
            if (value.startsWith(" ")) value = value.substring(1)
            if (hasData) data.append('\n')
            data.append(value)
            hasData = true
        }
    }

    private fun deliver(data: String) {
        try {
            onMessage(data)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to process intervention: %s", e.message)
        }
    }

    private fun isCurrent(connectionGeneration: Int): Boolean {
        return synchronized(lock) { connectionGeneration == generation }
    }

    private fun scheduleReconnect(connectionGeneration: Int) {
        synchronized(lock) {
            if (connectionGeneration != generation) return
            call = null
            val delayMillis = minOf(MAX_RETRY_DELAY_MILLIS, INITIAL_RETRY_DELAY_MILLIS shl minOf(retryAttempt, 10))
            retryAttempt++
            Logger.d(TAG, "Reconnecting to interventions in %s ms", delayMillis)
            val reconnect = object : Runnable {
                override fun run() {
                    synchronized(lock) {
                        if (pendingReconnect !== this) return
                        pendingReconnect = null
                        refresh()
                    }
                }
            }
            pendingReconnect = reconnect
            scheduler.postDelayed(reconnect, delayMillis)
        }
    }

    // Must hold the lock.
    private fun disconnect() {
        generation++
        call?.cancel()
        call = null
        connectedUrl = null
        pendingReconnect?.let { scheduler.removeCallbacks(it) }
        pendingReconnect = null
    }

    companion object {
        private val TAG = InterventionStream::class.java.simpleName

        /** The `userId` sent in `client_session` when user anonymisation is on. */
        internal const val ANONYMOUS_USER_ID = "00000000-0000-0000-0000-000000000000"

        private const val INITIAL_RETRY_DELAY_MILLIS = 1000L
        private const val MAX_RETRY_DELAY_MILLIS = 60_000L
    }
}
