package com.snowplowanalytics.snowplow.event;

import android.content.Context;
import android.test.AndroidTestCase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.MockEventStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ApplicationInstallTest extends AndroidTestCase {

    @Before
    public void setUp() throws Exception {
        ExecutorService es = Executor.shutdown();
        if (es != null) {
            es.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    // Tests

    @Test
    public void testApplicationInstall() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Prepare application install event
        SelfDescribingJson installEvent = new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL);
        SelfDescribing event = new SelfDescribing(installEvent);
        long currentTimestamp = 12345L;
        event.trueTimestamp = currentTimestamp;

        // Setup tracker
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appId")
                .base64encoding(false)
                .installAutotracking(false);
        MockEventStore eventStore = new MockEventStore();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore)
                .threadPoolSize(10);
        TrackerController trackerController = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, emitterConfiguration);

        // Track event
        trackerController.track(event);
        for (int i=0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        Payload payload = events.get(0).payload;

        // Check timestamp field
        String deviceTimestamp = (String)payload.getMap().get("dtm");
        String expected = Long.toString(currentTimestamp);
        assertEquals(expected, deviceTimestamp);
    }
}
