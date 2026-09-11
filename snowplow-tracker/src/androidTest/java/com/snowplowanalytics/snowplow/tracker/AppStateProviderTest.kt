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

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.session.AppStateProvider
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import org.junit.After
import org.junit.Assert
import org.junit.Assume
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

        // The seed must match what the process actually is. The instrumentation harness runs at
        // IMPORTANCE_FOREGROUND_SERVICE (125) - no Activity of its own - so it is deliberately
        // NOT foreground here; asserting a hardcoded `true` would bake in the old, wrong cutoff.
        Assert.assertEquals(expectedForegroundForThisProcess(), AppStateProvider.isForeground)
    }

    /**
     * What [AppStateProvider] should seed for the process these tests run in, derived from the
     * same inputs the implementation uses but written out independently, so it pins the contract
     * (STARTED means visible; otherwise only a genuinely foreground/visible process counts)
     * rather than the expression.
     */
    private fun expectedForegroundForThisProcess(): Boolean {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return true
        }
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
            info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
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

    @Test
    fun seedsFromProcessImportanceWhileStillBelowStarted() {
        // Regression for AISP-1708 review point (3). ProcessLifecycleOwner reports CREATED both
        // for a normal launch whose Activity has not started yet (the state during
        // Application.onCreate, where most apps create the tracker) and for a background-only
        // launch, so seeding purely from `isAtLeast(STARTED)` marked every normal cold start as
        // backgrounded and produced a spurious application_foreground once ON_START arrived.
        // The seed now falls back to process importance for that ambiguous window.
        AppStateProvider.onStop(ProcessLifecycleOwner.get())
        AppStateProvider.resetForTests()

        Assume.assumeFalse(
            "Only meaningful while the process has not reached STARTED",
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )

        AppStateProvider.initialize(context)

        Assert.assertEquals(
            "Below STARTED the seed must come from process importance, not the stale default",
            expectedForegroundForThisProcess(),
            AppStateProvider.isForeground
        )
    }

    @Test
    fun doesNotTreatAForegroundServiceProcessAsVisible() {
        // Regression for Copilot review point C4. IMPORTANCE_FOREGROUND_SERVICE (125) sits
        // BETWEEN IMPORTANCE_FOREGROUND (100) and IMPORTANCE_VISIBLE (200), so the original
        // `importance <= IMPORTANCE_VISIBLE` bound silently accepted a process whose only
        // foreground component is a service - an FCM push doing upload work, say - and reported
        // it as visible, which is the misclassification this seed exists to prevent.
        //
        // The instrumentation harness itself runs at 125, so it is a real example of that case.
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        Assume.assumeTrue(
            "Only meaningful when this process is at IMPORTANCE_FOREGROUND_SERVICE",
            info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
        )

        AppStateProvider.onStart(ProcessLifecycleOwner.get())
        AppStateProvider.resetForTests()
        Assume.assumeFalse(
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )

        AppStateProvider.initialize(context)

        Assert.assertFalse(
            "A process running only a foreground service must not be seeded as visible",
            AppStateProvider.isForeground
        )
    }

    @Test
    fun concurrentInitializeDoesNotLetASecondCallerReadTheStaleDefault() {
        // Regression for Copilot review point C1. A caller that arrives while another thread is
        // mid-seed used to return immediately, so it could read the `true` default before the
        // real seed landed - exactly the background-launch misreport this class exists to fix.
        AppStateProvider.resetForTests()

        val results = java.util.Collections.synchronizedList(mutableListOf<Boolean>())
        val done = CountDownLatch(4)
        repeat(4) {
            Thread {
                AppStateProvider.initialize(context)
                results.add(AppStateProvider.isForeground)
                done.countDown()
            }.start()
        }

        Assert.assertTrue("concurrent initialize() calls must all return", done.await(10, TimeUnit.SECONDS))
        // Every caller must observe the same, fully-seeded value - not a mix of seeded and stale.
        Assert.assertEquals(1, results.toSet().size)
    }

    @Test
    fun seedDoesNotDeadlockWhenCalledFromTheSessionConstructionPath() {
        // Regression for Copilot review point C2. Session.init runs inside the @Synchronized
        // Session.getInstance; seeding from there blocked on the main looper while holding the
        // Session class monitor, so a main thread wanting that monitor could never pump the queue
        // that would release it. The seed now happens in Snowplow.createTracker, before any lock.
        AppStateProvider.resetForTests()

        val created = CountDownLatch(1)
        Thread {
            Snowplow.createTracker(
                context,
                namespace = "deadlock" + Math.random(),
                network = NetworkConfiguration("http://snowplow-fake-url.com", HttpMethod.POST),
                TrackerConfiguration("app").sessionContext(true).lifecycleAutotracking(true)
            )
            created.countDown()
        }.start()

        // Keep the main looper busy so a seed that still blocked on it would be visible as a hang.
        Handler(Looper.getMainLooper()).post { Thread.sleep(300) }

        Assert.assertTrue(
            "createTracker off the main thread must not deadlock against the main looper",
            created.await(10, TimeUnit.SECONDS)
        )
        Snowplow.removeAllTrackers()
    }
}
