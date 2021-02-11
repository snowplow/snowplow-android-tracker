package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.EmitterController;
import com.snowplowanalytics.snowplow.controller.GdprController;
import com.snowplowanalytics.snowplow.controller.GlobalContextsController;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.emitter.EventStore;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.internal.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.MockEventStore;
import com.snowplowanalytics.snowplow.util.Basis;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest {

    @Before
    public void setUp() throws Exception {
        ExecutorService es = Executor.shutdown();
        if (es != null) {
            es.awaitTermination(60, TimeUnit.SECONDS);
        }
    }

    // Tests

    @Test
    public void basicInitialization() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid");
        trackerConfiguration.platformContext = true;
        TrackerController tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);

        assertNotNull(tracker);
        URI uri = URI.create(tracker.getNetwork().getEndpoint());
        assertNotNull(uri);
        String host = uri.getHost();
        String scheme = uri.getScheme();

        String protocol = networkConfiguration.getProtocol() == Protocol.HTTP ? "http" : "https";

        assertEquals(networkConfiguration.getEndpoint(), host);
        assertEquals(protocol, scheme);
        assertEquals(trackerConfiguration.appId, tracker.getAppId());
        assertEquals(trackerConfiguration.namespace, tracker.getNamespace());
    }

    @Test
    public void sessionInitialization() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TimeMeasure expectedForeground = new TimeMeasure(42, TimeUnit.SECONDS);
        TimeMeasure expectedBackground = new TimeMeasure(24, TimeUnit.SECONDS);
        NetworkConfiguration networkConfig = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("namespace", "appId");
        SessionConfiguration sessionConfig = new SessionConfiguration(expectedForeground, expectedBackground);
        TrackerController tracker = Tracker.setup(context, networkConfig, trackerConfig, Collections.singletonList(sessionConfig));
        TimeMeasure foreground = tracker.getSession().getForegroundTimeout();
        TimeMeasure background = tracker.getSession().getBackgroundTimeout();
        assertEquals(expectedForeground, foreground);
        assertEquals(expectedBackground, background);
    }

    @Test
    public void sessionControllerUnavailableWhenContextTurnedOff() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid");
        trackerConfiguration.sessionContext = true;
        TrackerController tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);
        assertNotNull(tracker.getSession());

        trackerConfiguration.sessionContext = false;
        tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration);
        assertNull(tracker.getSession());

        tracker.setSessionContext(true);
        assertNotNull(tracker.getSession());

        tracker.setSessionContext(false);
        assertNotNull(tracker.getSession());
    }

    @Test
    public void emitterConfiguration() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid");
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .bufferOption(BufferOption.DefaultGroup)
                .byteLimitGet(10000)
                .emitRange(10);
        TrackerController tracker = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration, Collections.singletonList(emitterConfiguration));
        tracker.pause(); // To block the flush operation that would turn isRunning flag on.
        EmitterController emitterController = tracker.getEmitter();

        assertEquals(BufferOption.DefaultGroup, emitterController.getBufferOption());
        assertEquals(10000, emitterController.getByteLimitGet());
        assertEquals(10, emitterController.getEmitRange());

        // Thread.sleep(1000);  // To allow the tracker completing the flush operation and turning isRunning off

        emitterController.setBufferOption(BufferOption.HeavyGroup);
        emitterController.setByteLimitGet(100);
        emitterController.setEmitRange(1);
        assertEquals(BufferOption.HeavyGroup, emitterController.getBufferOption());
        assertEquals(100, emitterController.getByteLimitGet());
        assertEquals(1, emitterController.getEmitRange());
    }

    @Test
    public void gdprConfiguration() throws InterruptedException {
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid")
                .base64encoding(false);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore)
                .threadPoolSize(10);
        GdprConfiguration gdprConfiguration = new GdprConfiguration(Basis.CONSENT, "id", "ver", "desc");
        TrackerController trackerController = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration, Arrays.asList(gdprConfiguration, emitterConfiguration));
        GdprController gdprController = trackerController.getGdpr();

        // Check gdpr settings
        assertEquals(Basis.CONSENT, gdprController.getBasisForProcessing());
        assertEquals("id", gdprController.getDocumentId());

        // Check gdpr settings reset
        gdprController.reset(Basis.CONTRACT, "id1", "ver1", "desc1");
        assertEquals(Basis.CONTRACT, gdprController.getBasisForProcessing());
        assertEquals("id1", gdprController.getDocumentId());
        assertTrue(gdprController.isEnabled());

        // Check gdpr context added
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        Payload payload = events.get(0).payload;
        String contexts = (String) payload.getMap().get("co");
        assertTrue(contexts.contains("\"basisForProcessing\":\"contract\""));
        assertTrue(contexts.contains("\"documentId\":\"id1\""));

        // Check gdpr disabled
        gdprController.disable();
        assertFalse(gdprController.isEnabled());
        assertEquals(Basis.CONTRACT, gdprController.getBasisForProcessing());
        assertEquals("id1", gdprController.getDocumentId());

        // Check gdpr context not added
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        payload = events.get(0).payload;
        contexts = (String) payload.getMap().get("co");
        assertFalse(contexts.contains("\"basisForProcessing\":\"contract\""));
        assertFalse(contexts.contains("\"documentId\":\"id1\""));

        // Check gdpr enabled again
        gdprController.enable();
        assertTrue(gdprController.isEnabled());
    }

    @Test
    public void globalContextsConfiguration() throws InterruptedException {
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", Protocol.HTTPS, HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("namespace", "appid")
                .base64encoding(false);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore);
        GlobalContextsConfiguration gcConfiguration = new GlobalContextsConfiguration(null);
        gcConfiguration.add("k1", new GlobalContext(Collections.singletonList(new SelfDescribingJson("schema", new HashMap<String, Object>() {{
            put("key", "value1");
        }}))));
        TrackerController trackerController = ServiceProvider.setup(context, networkConfiguration, trackerConfiguration, Arrays.asList(gcConfiguration, emitterConfiguration));
        GlobalContextsController gcController = trackerController.getGlobalContexts();

        // Check global contexts settings
        assertEquals(Set.of("k1"), gcController.getTags());

        // Add new global context
        gcController.add("k2", new GlobalContext(Collections.singletonList(new SelfDescribingJson("schema", new HashMap<String, Object>() {{
            put("key", "value2");
        }}))));
        assertEquals(Set.of("k1", "k2"), gcController.getTags());

        // Check global context added to event
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        Payload payload = events.get(0).payload;
        String contexts = (String) payload.getMap().get("co");
        assertTrue(contexts.contains("value1"));
        assertTrue(contexts.contains("value2"));
    }
}
