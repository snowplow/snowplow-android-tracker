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
 * The cache is seeded once on [initialize] (hopping to the main thread if needed, since
 * [ProcessLifecycleOwner] must only be touched from there) and kept live afterwards by
 * ON_START/ON_STOP callbacks, so consumers can read [isForeground] from any thread without
 * ever touching [ProcessLifecycleOwner] directly.
 *
 * This object deliberately does NOT implement [DefaultLifecycleObserver] itself: that interface
 * (and [ProcessLifecycleOwner], which lives in the `compileOnly` lifecycle-extensions artifact)
 * is only touched inside the [initialize] try block, mirroring [ProcessObserver]'s pattern of
 * instantiating a throwaway observer instance inside `try` rather than declaring the optional
 * supertype on a class/object that gets loaded (and its supertypes resolved) as soon as any of
 * its members are referenced.
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
        // Only claim the seeding job under the lock, and release it immediately - never hold it
        // across the main-thread hop below. AppStateProvider.initialize() can be called from
        // several places (Session.init, Tracker.initializeLifecycleTracking) on whichever thread
        // the caller happens to be on, including the main thread itself. If the lock were held
        // for the whole method (as with a plain @Synchronized fun), a background thread that
        // wins the race would post a Runnable to the main looper and block on it while holding
        // the monitor; a main thread that is itself still synchronously unwinding through
        // Snowplow.createTracker (and hasn't returned to its Looper yet) would then deadlock
        // trying to acquire that same monitor - it can never pump the message queue to run the
        // posted seed, and the background thread can never release the monitor. ANR.
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
                // ProcessLifecycleOwner alone cannot answer this question at seed time.
                // It dispatches ON_CREATE unconditionally for every process start (it is wired
                // up via a ContentProvider that attaches before Application.onCreate, regardless
                // of which component started the process), and only reaches STARTED once an
                // Activity actually starts - which, on a normal launch, happens *after* the
                // Application.onCreate() in which most apps create the tracker. So at seed time
                // CREATED means "foreground launch, Activity not started yet" and "background-only
                // launch" alike, and neither `isAtLeast(STARTED)` nor `isAtLeast(CREATED)` can
                // separate them: the former marks every normal cold start as backgrounded, the
                // latter marks every background launch as foregrounded.
                //
                // The process's own importance does separate them, synchronously, at that exact
                // moment: Android has already decided why it started this process. A process
                // started for an Activity is IMPORTANCE_FOREGROUND (100) while still at CREATED;
                // one started for a broadcast/service (WorkManager, FCM) is IMPORTANCE_CACHED
                // (400) or IMPORTANCE_SERVICE (300). This is the Android analogue of the
                // .inactive-vs-.background distinction iOS relies on.
                //
                // Verified on API 36 with the tracker created in Application.onCreate():
                //   foreground launch (am start)     -> state=CREATED, importance=100
                //   background launch (am broadcast) -> state=CREATED, importance=400
                //
                // ProcessLifecycleOwner is still authoritative once it has actually observed an
                // Activity, so prefer it whenever it has already reached STARTED, and fall back
                // to importance only for the ambiguous pre-STARTED window.
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
            // Block until the initial state is seeded on the main thread, so that a
            // background-only process launch (e.g. WorkManager, FCM) doesn't read a stale
            // default before the very first event is tracked. This must NOT be a bounded wait:
            // if the caller gave up after a timeout while the posted seed was still queued,
            // that seed would later run and silently overwrite `_isForeground` at an
            // arbitrary point in the future (e.g. mid-way through unrelated code that had
            // already read/relied on the stale value, or explicitly set it itself). Since only
            // one caller ever wins the `shouldSeed` race above, there is never a second,
            // late-arriving seed to guard against.
            val latch = CountDownLatch(1)
            Handler(context.mainLooper).post {
                seedAndRegister()
                latch.countDown()
            }
            latch.await()
        }
    }

    /**
     * Whether this process was started for a foreground component, read from the process's own
     * importance. Used only to disambiguate the pre-STARTED window in [initialize], where
     * [ProcessLifecycleOwner] reports CREATED for both a normal launch and a background-only one.
     *
     * IMPORTANCE_VISIBLE (200) is included so that a process kept alive by a visible-but-not-
     * focused component is not misreported as backgrounded. Anything at or above
     * IMPORTANCE_FOREGROUND_SERVICE (125) that is not visible - a foreground service doing
     * background work, a cached or service process - is treated as not foregrounded, matching
     * the "no user-visible UI" meaning of `isVisible`.
     *
     * Defaults to `true` if the state can't be read, preserving the tracker's behaviour from
     * before the state was read at all.
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
     * Resets the seeding state so the next [initialize] call runs the seed path again as if the
     * process had just started. Tests that need to exercise [initialize] itself - rather than
     * driving state directly via [onStart]/[onStop] - must call this first: [AppStateProvider] is
     * a process-wide singleton, so once any earlier test (or the tracker itself) has seeded it,
     * further [initialize] calls are no-ops.
     */
    @VisibleForTesting
    @JvmStatic
    fun resetForTests() {
        synchronized(lock) {
            initializationState = InitializationState.NONE
        }
    }
}
