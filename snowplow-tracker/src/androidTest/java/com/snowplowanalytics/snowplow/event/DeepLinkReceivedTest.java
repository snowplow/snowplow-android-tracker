package com.snowplowanalytics.snowplow.event;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.MockEventStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class DeepLinkReceivedTest {

    @Before
    public void setUp() throws Exception {
        ExecutorService es = Executor.shutdown();
        if (es != null) {
            es.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testExpectedForm() {
        DeepLinkReceived event = new DeepLinkReceived("url")
                .referrer("referrer");

        Map<String, Object> payload = event.getDataPayload();
        assertNotNull(payload);
        assertEquals("url", payload.get(DeepLinkReceived.PARAM_URL));
        assertEquals("referrer", payload.get(DeepLinkReceived.PARAM_REFERRER));
    }

    @Test
    public void testWorkaroundForCampaignAttributionEnrichment() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Prepare DeepLinkReceived event
        DeepLinkReceived event = new DeepLinkReceived("url")
                .referrer("referrer");

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

        // Check url and referrer fields for atomic table
        String url = (String)payload.getMap().get(Parameters.PAGE_URL);
        String referrer = (String)payload.getMap().get(Parameters.PAGE_REFR);
        assertEquals("url", url);
        assertEquals("referrer", referrer);
    }

    @Test
    public void testDeepLinkContextAndAtomicPropertiesAddedToScreenView() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Prepare DeepLinkReceived event
        DeepLinkReceived event = new DeepLinkReceived("the_url")
                .referrer("the_referrer");

        // Prepare Screen View event
        ScreenView screenView = new ScreenView("SV");

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

        // Track events
        trackerController.track(event);
        UUID screenViewEventId = trackerController.track(screenView);
        for (int i=0; eventStore.getSize() < 2 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(2, events.size());

        Map screenViewPayload = null;
        for (EmitterEvent emitterEvent : events) {
            if (Objects.equals(emitterEvent.payload.getMap().get("eid"), screenViewEventId.toString())) {
                screenViewPayload = emitterEvent.payload.getMap();
                break;
            }
        }
        assertNotNull(screenViewPayload);

        // Check the DeepLink context entity properties
        String screenViewContext = (String) screenViewPayload.get(Parameters.CONTEXT);
        assertNotNull(screenViewContext);
        assertTrue(screenViewContext.contains("\"referrer\":\"the_referrer\""));
        assertTrue(screenViewContext.contains("\"url\":\"the_url\""));

        // Check url and referrer fields for atomic table
        String url = (String)screenViewPayload.get(Parameters.PAGE_URL);
        String referrer = (String)screenViewPayload.get(Parameters.PAGE_REFR);
        assertEquals("the_url", url);
        assertEquals("the_referrer", referrer);
    }
}
