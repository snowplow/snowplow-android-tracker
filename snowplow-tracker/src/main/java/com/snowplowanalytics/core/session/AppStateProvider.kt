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
package com.snowplowanalytics.core.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.snowplowanalytics.core.tracker.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Caches whether the app process is currently foregrounded, backed by [ProcessLifecycleOwner].
 * The cache is seeded once on [initialize] (hopping to the main thread if needed, since
 * [ProcessLifecycleOwner] must only be touched from there) and kept live afterwards by
 * ON_START/ON_STOP callbacks, so consumers can read [isForeground] from any thread without
 * ever touching [ProcessLifecycleOwner] directly.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object AppStateProvider : DefaultLifecycleObserver {
    private enum class InitializationState {
        NONE, IN_PROGRESS, COMPLETE
    }

    private val TAG = AppStateProvider::class.java.simpleName
    private var initializationState = InitializationState.NONE

    @Volatile
    private var _isForeground: Boolean = true

    /** Whether the app process is currently foregrounded. Safe to read from any thread. */
    val isForeground: Boolean
        get() = _isForeground

    override fun onStart(owner: LifecycleOwner) {
        _isForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        _isForeground = false
    }

    @JvmStatic
    @Synchronized
    fun initialize(context: Context) {
        if (initializationState != InitializationState.NONE) return
        initializationState = InitializationState.IN_PROGRESS

        val seedAndRegister = {
            try {
                val lifecycle = ProcessLifecycleOwner.get().lifecycle
                _isForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                lifecycle.addObserver(this)
                initializationState = InitializationState.COMPLETE
            } catch (e: NoClassDefFoundError) {
                initializationState = InitializationState.NONE
                Logger.e(TAG, "Class 'ProcessLifecycleOwner' not found. The tracker can't track app state.")
            }
        }

        if (Looper.myLooper() == context.mainLooper) {
            seedAndRegister()
        } else {
            // Block (with a timeout guard) until the initial state is seeded on the main thread,
            // so that a background-only process launch (e.g. WorkManager, FCM) doesn't read a
            // stale default before the very first event is tracked.
            val latch = CountDownLatch(1)
            Handler(context.mainLooper).post {
                seedAndRegister()
                latch.countDown()
            }
            latch.await(1, TimeUnit.SECONDS)
        }
    }
}
