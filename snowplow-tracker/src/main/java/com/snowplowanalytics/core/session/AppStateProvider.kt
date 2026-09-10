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

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.snowplowanalytics.core.tracker.Logger
import java.util.concurrent.CountDownLatch

/**
 * Caches whether the app process is currently foregrounded, backed by [ProcessLifecycleOwner].
 * Seeded once on [initialize] and kept up to date by ON_START/ON_STOP, so [isForeground] is
 * readable from any thread without touching [ProcessLifecycleOwner] (which is main-thread only).
 *
 * Deliberately does not implement [DefaultLifecycleObserver] itself: that interface comes from
 * the `compileOnly` lifecycle-extensions artifact, so declaring it as a supertype would throw
 * [NoClassDefFoundError] at class-load - outside the try block - for apps without that
 * dependency. Same reason [ProcessObserver] instantiates its observer inside `try`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
object AppStateProvider {
    private enum class InitializationState {
        NONE, IN_PROGRESS, COMPLETE
    }

    private class Observer : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) = AppStateProvider.onStart(owner)
        override fun onStop(owner: LifecycleOwner) = AppStateProvider.onStop(owner)
    }

    private val TAG = AppStateProvider::class.java.simpleName
    private val lock = Any()

    @Volatile
    private var initializationState = InitializationState.NONE

    @Volatile
    private var _isForeground: Boolean = true

    /** Whether the app process is currently foregrounded. Safe to read from any thread. */
    val isForeground: Boolean
        get() = _isForeground

    fun onStart(owner: LifecycleOwner) {
        _isForeground = true
    }

    fun onStop(owner: LifecycleOwner) {
        _isForeground = false
    }

    @JvmStatic
    fun initialize(context: Context) {
        // Claim the seeding job under the lock and release it immediately - never hold it across
        // the main-thread hop below. Holding it (e.g. a plain @Synchronized fun) deadlocks: a
        // background thread that wins the race would block on the main looper while holding the
        // monitor, and the main thread - still unwinding through Snowplow.createTracker, so not
        // yet pumping its message queue - would block acquiring it. ANR.
        val shouldSeed = synchronized(lock) {
            if (initializationState != InitializationState.NONE) {
                false
            } else {
                initializationState = InitializationState.IN_PROGRESS
                true
            }
        }
        if (!shouldSeed) return

        val seedAndRegister = {
            try {
                val lifecycle = ProcessLifecycleOwner.get().lifecycle
                // Before the first Activity starts, the lifecycle state cannot tell a foreground
                // launch from a background-only one: ON_CREATE is dispatched unconditionally for
                // every process start, and STARTED only arrives after the Application.onCreate()
                // in which most apps create the tracker. Both read CREATED there.
                //
                // Process importance does distinguish them at that point - Android has already
                // decided why it started the process (Activity -> FOREGROUND, broadcast/service
                // -> CACHED/SERVICE). Verified on API 36 from Application.onCreate(): foreground
                // launch reads importance 100, background launch 400, both at CREATED.
                //
                // So trust the lifecycle once it has actually seen an Activity, and fall back to
                // importance only for that ambiguous pre-STARTED window.
                _isForeground = if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    true
                } else {
                    isProcessImportanceForeground()
                }
                lifecycle.addObserver(Observer())
                initializationState = InitializationState.COMPLETE
            } catch (e: NoClassDefFoundError) {
                initializationState = InitializationState.NONE
                Logger.e(TAG, "Class 'ProcessLifecycleOwner' not found. The tracker can't track app state.")
            }
        }

        if (Looper.myLooper() == context.mainLooper) {
            seedAndRegister()
        } else {
            // Block until seeded, so a background-only launch doesn't read the stale default
            // before the first event is tracked. Deliberately unbounded: a timed-out caller
            // would leave the queued seed to overwrite `_isForeground` at an arbitrary later
            // point, after other code had already read or set it.
            val latch = CountDownLatch(1)
            Handler(context.mainLooper).post {
                seedAndRegister()
                latch.countDown()
            }
            latch.await()
        }
    }

    /**
     * Whether this process was started for a foreground component. Used only to disambiguate the
     * pre-STARTED window in [initialize].
     *
     * The cutoff includes IMPORTANCE_VISIBLE so a visible-but-unfocused process isn't reported as
     * backgrounded; a foreground *service* doing background work falls outside it, matching the
     * "no user-visible UI" meaning of `isVisible`. Defaults to `true` if unreadable, preserving
     * the behaviour from before the state was read at all.
     */
    private fun isProcessImportanceForeground(): Boolean {
        return try {
            val info = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(info)
            info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        } catch (e: Exception) {
            Logger.e(TAG, "Could not read process importance: %s", e)
            true
        }
    }

    /**
     * Lets the next [initialize] run the seed path again. Required by tests that exercise
     * [initialize] itself: this is a process-wide singleton, so once anything has seeded it,
     * further calls are no-ops.
     */
    @VisibleForTesting
    @JvmStatic
    fun resetForTests() {
        synchronized(lock) {
            initializationState = InitializationState.NONE
        }
    }
}
