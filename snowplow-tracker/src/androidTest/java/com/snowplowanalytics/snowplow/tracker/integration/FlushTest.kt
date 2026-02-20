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
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlushTest {

    @Test
    fun testFlushEventsViaTracker() {
        val networkConnection = MockNetworkConnection(HttpMethod.GET, 200)
        val networkConfig = NetworkConfiguration(networkConnection)
        val emitterConfig = EmitterConfiguration().bufferOption(BufferOption.SmallGroup)

        val tracker = Snowplow.createTracker(
            InstrumentationRegistry.getInstrumentation().targetContext,
            "flush" + Math.random().toString(),
            networkConfig,
            emitterConfig
        )
        
        tracker.track(ScreenView("screenName"))
        Thread.sleep(200)
        Assert.assertEquals(0, networkConnection.countRequests())
        
        tracker.emitter.flush()

        var counter = 0
        while (networkConnection.countRequests() == 0) {
            Thread.sleep(500)
            counter++
            if (counter > 10) {
                return
            }
        }

        Assert.assertEquals(1, networkConnection.countRequests())
    }
}
