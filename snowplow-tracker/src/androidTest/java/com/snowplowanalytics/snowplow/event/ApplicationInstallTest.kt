package com.snowplowanalytics.snowplow.event

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.emitter.Executor.shutdown
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.MockEventStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ApplicationInstallTest {
    @Before
    @Throws(Exception::class)
    fun setUp() {
        val es = shutdown()
        es?.awaitTermination(60, TimeUnit.SECONDS)
    }

    // Tests
    @Test
    @Throws(InterruptedException::class)
    fun testApplicationInstall() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Prepare application install event
        val installEvent = SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL)
        val event = SelfDescribing(installEvent)
        val currentTimestamp = 12345L
        event.trueTimestamp = currentTimestamp

        // Setup tracker
        val trackerConfiguration = TrackerConfiguration("appId")
            .base64encoding(false)
            .installAutotracking(false)
        val eventStore = MockEventStore()
        val networkConfiguration = NetworkConfiguration("fake-url", HttpMethod.POST)
        val emitterConfiguration = EmitterConfiguration()
            .eventStore(eventStore)
            .threadPoolSize(10)
        val trackerController = createTracker(
            context,
            "namespace",
            networkConfiguration,
            trackerConfiguration,
            emitterConfiguration
        )

        // Track event
        trackerController.track(event)
        var i = 0
        while (eventStore.size < 1 && i < 10) {
            Thread.sleep(1000)
            i++
        }
        val events = eventStore.getEmittableEvents(10)
        eventStore.removeAllEvents()
        Assert.assertEquals(1, events.size.toLong())
        val payload = events[0]!!.payload

        // Check timestamp field
        val deviceTimestamp = payload.map["dtm"] as String?
        val expected = currentTimestamp.toString()
        Assert.assertEquals(expected, deviceTimestamp)
    }
}
