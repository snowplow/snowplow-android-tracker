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
package com.snowplowanalytics.snowplow.tracker.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.emitter.BufferOption
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlushTest {

    @After
    fun tearDown() {
        // Remove any trackers registered during the test so they don't leak threads/state
        // into later instrumentation tests.
        Snowplow.removeAllTrackers()
    }

    /**
     * Events buffered below the buffer threshold are not sent until flush() is called on the
     * public EmitterController. This is the "flush on logout" use case.
     */
    @Test
    fun testFlushEventsViaEmitterController() {
        val networkConnection = MockNetworkConnection(HttpMethod.POST, 200)
        val networkConfig = NetworkConfiguration(networkConnection)
        // SmallGroup buffer (10) means a single tracked event is not sent automatically.
        val emitterConfig = EmitterConfiguration().bufferOption(BufferOption.SmallGroup)

        val tracker = Snowplow.createTracker(
            InstrumentationRegistry.getInstrumentation().targetContext,
            "flush" + Math.random().toString(),
            networkConfig,
            emitterConfig
        )

        tracker.track(ScreenView("screenName"))

        // Wait until the tracked event has actually been persisted to the store (tracking is
        // async), so the flush below has something to send. The exact count may be higher than 1
        // because creating a tracker can auto-track events (e.g. session start). The buffer
        // threshold (10) is not reached, so nothing is sent automatically.
        var waited = 0
        while (tracker.emitter.dbCount < 1 && waited < 20) {
            Thread.sleep(200)
            waited++
        }
        Assert.assertTrue("expected at least one buffered event", tracker.emitter.dbCount >= 1)
        Assert.assertEquals(0, networkConnection.countRequests())

        // Public flush() API - what the customer calls on logout.
        tracker.emitter.flush()

        // Flush should drain the store; wait for it to reach zero.
        var counter = 0
        while (tracker.emitter.dbCount > 0 && counter < 25) {
            Thread.sleep(200)
            counter++
        }

        Assert.assertTrue("expected at least one request sent", networkConnection.countRequests() >= 1)
        Assert.assertEquals(0, tracker.emitter.dbCount)
    }
}
