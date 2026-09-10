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
package com.snowplowanalytics.snowplow.tracker

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.session.AppStateProvider
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Regression tests for AISP-1708 review point (4): every other [LifecycleStateMachineTest] test
 * drives [AppStateProvider] via [AppStateProvider.onStart]/[AppStateProvider.onStop] directly,
 * so [AppStateProvider.initialize] itself - the main-thread hop, the latch, the `currentState`
 * read - was never exercised by any test. These tests call [AppStateProvider.initialize] for real.
 */
@RunWith(AndroidJUnit4::class)
class AppStateProviderTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        // Restore the default (foreground) app state so other tests aren't affected by this one.
        AppStateProvider.onStart(ProcessLifecycleOwner.get())
    }

    @Test
    fun initializeFromBackgroundThreadSeedsFromRealProcessState() {
        AppStateProvider.resetForTests()

        val done = CountDownLatch(1)
        val thread = Thread {
            AppStateProvider.initialize(context)
            done.countDown()
        }
        thread.start()

        Assert.assertTrue(
            "initialize() must return once the main-thread seed has run, not hang",
            done.await(5, TimeUnit.SECONDS)
        )

        val expected = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        Assert.assertEquals(expected, AppStateProvider.isForeground)
    }

    @Test
    fun initializeDoesNotDeadlockWhenMainThreadCallsItConcurrentlyWithABackgroundSeed() {
        AppStateProvider.resetForTests()

        val mainThreadRunnableStarted = CountDownLatch(1)
        val backgroundThreadClaimed = CountDownLatch(1)
        val mainThreadReturned = CountDownLatch(1)

        // Simulates the main thread being synchronously busy with other startup work (e.g.
        // Session.init reading SharedPreferences inside Snowplow.createTracker) before it
        // reaches its own call to AppStateProvider.initialize() - the exact call stack from the
        // AISP-1708 review's deadlock scenario. It must not block the main looper forever even
        // though a background thread wins the race to seed first and is, at this point, blocked
        // waiting for the main looper to run the seed Runnable it posted.
        Handler(Looper.getMainLooper()).post {
            mainThreadRunnableStarted.countDown()
            backgroundThreadClaimed.await(3, TimeUnit.SECONDS)
            AppStateProvider.initialize(context)
            mainThreadReturned.countDown()
        }
        Assert.assertTrue(mainThreadRunnableStarted.await(2, TimeUnit.SECONDS))

        val backgroundThread = Thread {
            AppStateProvider.initialize(context)
        }
        backgroundThread.start()
        // Give the background thread a moment to win the initialize() race and post its seed
        // Runnable to the main looper before releasing the main-thread Runnable above.
        Thread.sleep(50)
        backgroundThreadClaimed.countDown()

        Assert.assertTrue(
            "A concurrent initialize() call on the main thread must not deadlock",
            mainThreadReturned.await(3, TimeUnit.SECONDS)
        )
        backgroundThread.join(3000)
        Assert.assertFalse("The background seeding thread must complete, not hang", backgroundThread.isAlive)
    }
}
