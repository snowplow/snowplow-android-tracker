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

    /** Released once the seed has run, so callers that arrive mid-seed can wait for it. */
    @Volatile
    private var seedLatch = CountDownLatch(1)

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

    /**
     * Seeds the cached state and subscribes to lifecycle callbacks. Safe to call repeatedly; only
     * the first call does the work.
     *
     * MUST be called from outside any lock the main thread could also be waiting on - in practice
     * only from [com.snowplowanalytics.snowplow.Snowplow.createTracker], before any tracker,
     * session or service provider is constructed. Off the main thread it blocks on a main-looper
     * hop, so calling it from inside a monitor the main thread might acquire (as `Session.init`
     * once did, under the `@Synchronized Session.getInstance`) deadlocks: the background thread
     * holds the monitor while waiting for the looper, and the main thread blocks on that monitor
     * instead of pumping the queue that would release it.
     */
    @JvmStatic
    fun initialize(context: Context) {
        // Claim the seeding job under the lock and release it immediately - never hold it across
        // either wait below, for the same deadlock reason as above.
        val state = synchronized(lock) {
            val current = initializationState
            if (current == InitializationState.NONE) {
                initializationState = InitializationState.IN_PROGRESS
            }
            current
        }

        if (state == InitializationState.COMPLETE) return
        if (state == InitializationState.IN_PROGRESS) {
            // Another thread is mid-seed. Returning straight away would let this caller read the
            // stale default, so wait for that seed to land. Never blocks the main thread: the
            // seeding thread either IS the main thread (its seed is synchronous, so this state is
            // not observable from it) or has posted to the looper, which this thread does not block.
            if (Looper.myLooper() != context.mainLooper) {
                seedLatch.await()
            }
            return
        }

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
                // Leave the state at IN_PROGRESS rather than resetting to NONE: the class is not
                // going to appear later in the process, so re-running the seed on every
                // subsequent call would just repeat the failed lookup. `_isForeground` keeps its
                // `true` default, which is the documented fallback.
                Logger.e(TAG, "Class 'ProcessLifecycleOwner' not found. The tracker can't track app state.")
            } finally {
                // Release waiters on every path, including the failure above, so a caller
                // blocked in the IN_PROGRESS branch can never be stranded.
                seedLatch.countDown()
            }
        }

        if (Looper.myLooper() == context.mainLooper) {
            seedAndRegister()
        } else {
            // Block until seeded, so a background-only launch doesn't read the stale default
            // before the first event is tracked. Deliberately unbounded: a timed-out caller
            // would leave the queued seed to overwrite `_isForeground` at an arbitrary later
            // point, after other code had already read or set it.
            Handler(context.mainLooper).post { seedAndRegister() }
            seedLatch.await()
        }
    }

    /**
     * Whether this process was started for a component the user can see. Used only to
     * disambiguate the pre-STARTED window in [initialize].
     *
     * Only IMPORTANCE_FOREGROUND (100) and IMPORTANCE_VISIBLE (200) count. Note the constants are
     * NOT ordered by visibility: IMPORTANCE_FOREGROUND_SERVICE (125) sits *between* them, so a
     * plain `<= IMPORTANCE_VISIBLE` bound would also accept a process started only for a
     * foreground service - an FCM push doing background upload work, say - and report it as
     * visible, which is the very misclassification this seed exists to avoid.
     *
     * Defaults to `true` if unreadable, preserving the behaviour from before the state was read.
     */
    private fun isProcessImportanceForeground(): Boolean {
        return try {
            val info = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(info)
            info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
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
            seedLatch = CountDownLatch(1)
        }
    }

    /**
     * Marks the state as already seeded, so a later [initialize] (from `Snowplow.createTracker`,
     * say) cannot overwrite a value a test has set via [onStart]/[onStop]. Without this, a test
     * that forces the state and then creates a tracker races the real seed, and passes or fails
     * depending on whether anything else in the suite had already seeded the process.
     */
    @VisibleForTesting
    @JvmStatic
    fun markSeededForTests() {
        synchronized(lock) {
            initializationState = InitializationState.COMPLETE
            seedLatch.countDown()
        }
    }
}
