package com.snowplowanalytics.snowplow.internal.tracker;

import static com.snowplowanalytics.snowplow.network.HttpMethod.GET;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.EmitterConfiguration;
import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.GdprController;
import com.snowplowanalytics.snowplow.controller.GlobalContextsController;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext;
import com.snowplowanalytics.snowplow.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.emitter.Emitter;
import com.snowplowanalytics.snowplow.internal.emitter.Executor;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.network.Request;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.BuildConfig;
import com.snowplowanalytics.snowplow.tracker.MockEventStore;
import com.snowplowanalytics.snowplow.tracker.MockNetworkConnection;
import com.snowplowanalytics.snowplow.util.Basis;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import junit.framework.TestCase;

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
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid");
        trackerConfiguration.platformContext = true;
        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration);

        assertNotNull(tracker);
        URI uri = URI.create(tracker.getNetwork().getEndpoint());
        assertNotNull(uri);
        String host = uri.getHost();
        String scheme = uri.getScheme();

        String protocol = networkConfiguration.getProtocol() == Protocol.HTTP ? "http" : "https";

        assertEquals(networkConfiguration.getEndpoint(), scheme + "://" + host);
        assertEquals(protocol, scheme);
        assertEquals(trackerConfiguration.appId, tracker.getAppId());
        assertEquals("namespace", tracker.getNamespace());
    }

    @Test
    public void sessionInitialization() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TimeMeasure expectedForeground = new TimeMeasure(42, TimeUnit.SECONDS);
        TimeMeasure expectedBackground = new TimeMeasure(24, TimeUnit.SECONDS);
        NetworkConfiguration networkConfig = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration( "appId");
        SessionConfiguration sessionConfig = new SessionConfiguration(expectedForeground, expectedBackground);
        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfig, trackerConfig, sessionConfig);
        TimeMeasure foreground = tracker.getSession().getForegroundTimeout();
        TimeMeasure background = tracker.getSession().getBackgroundTimeout();
        assertEquals(expectedForeground, foreground);
        assertEquals(expectedBackground, background);
    }

    @Test
    public void sessionControllerUnavailableWhenContextTurnedOff() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid");
        trackerConfiguration.sessionContext = true;
        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration);
        assertNotNull(tracker.getSession());

        trackerConfiguration.sessionContext = false;
        tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration);
        assertNull(tracker.getSession());
    }

    @Test
    public void sessionConfigurationCallback() throws InterruptedException {
        final Object expectation = new Object();
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Remove stored session data
        SharedPreferences sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS + "_namespace", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(TrackerConstants.SESSION_STATE).commit();

        // Configure tracker
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid")
                .sessionContext(true);
        SessionConfiguration sessionConfiguration = new SessionConfiguration(
                new TimeMeasure(100, TimeUnit.SECONDS),
                new TimeMeasure(100, TimeUnit.SECONDS))
                .onSessionUpdate(sessionState -> {
                    assertEquals(1, sessionState.getSessionIndex());
                    assertNull(sessionState.getPreviousSessionId());
                    callbackExecuted.set(true);
                    synchronized (expectation) {
                        expectation.notify();
                    }
                });

        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, sessionConfiguration);
        tracker.track(new Timing("cat", "var", 123));

        synchronized (expectation) {
            expectation.wait(10000);
        }
        assertTrue(callbackExecuted.get());
    }

    @Test
    public void sessionConfigurationCallbackAfterNewSession() throws InterruptedException {
        final Object expectation1 = new Object();
        final Object expectation2 = new Object();
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        AtomicReference<String> sessionId = new AtomicReference<>(null);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Remove stored session data
        SharedPreferences sharedPreferences = context.getSharedPreferences(TrackerConstants.SNOWPLOW_SESSION_VARS + "_namespace", Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(TrackerConstants.SESSION_STATE).commit();

        // Configure tracker
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid")
                .sessionContext(true);
        SessionConfiguration sessionConfiguration = new SessionConfiguration(
                new TimeMeasure(100, TimeUnit.SECONDS),
                new TimeMeasure(100, TimeUnit.SECONDS))
                .onSessionUpdate(sessionState -> {
                    if (sessionState.getSessionIndex() == 1) {
                        assertNull(sessionState.getPreviousSessionId());
                        sessionId.set(sessionState.getSessionId());
                        synchronized (expectation1) {
                            expectation1.notify();
                        }
                    } else {
                        assertEquals(2, sessionState.getSessionIndex());
                        assertEquals(sessionId.get(), sessionState.getPreviousSessionId());
                        callbackExecuted.set(true);
                        synchronized (expectation2) {
                            expectation2.notify();
                        }
                    }
                });

        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, sessionConfiguration);
        tracker.track(new Timing("cat", "var", 123));
        synchronized (expectation1) {
            // This delay is needed because the session manager doesn't manage correclty the sequence of the events
            // in a multithreading model when the throughput is high.
            // TODO: To fix this issue we have to refactor the session manager to work like ScreenStateMachine where
            // it correctly manage the state attached to the tracked events.
            expectation1.wait(3000);
        }

        tracker.getSession().startNewSession();

        tracker.track(new Timing("cat", "var", 123));
        synchronized (expectation2) {
            expectation2.wait(3000);
        }
        assertTrue(callbackExecuted.get());
    }

    // TODO: Flaky test to fix
    /*
    @Test
    public void emitterConfiguration() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid");
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .bufferOption(BufferOption.DefaultGroup)
                .byteLimitGet(10000)
                .emitRange(10);
        TrackerController tracker = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, emitterConfiguration);
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
    */

    @Test
    public void trackerVersionSuffix() throws InterruptedException {
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appId")
                .base64encoding(false)
                .installAutotracking(false)
                .trackerVersionSuffix("test With Space 1-2-3");

        // Setup tracker
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore)
                .threadPoolSize(10);
        TrackerController trackerController = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, emitterConfiguration);

        // Track fake event
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        Payload payload = events.get(0).payload;

        // Check v_tracker field
        String versionTracker = (String) payload.getMap().get("tv");
        String expected = BuildConfig.TRACKER_LABEL + " testWithSpace1-2-3";
        assertEquals(expected, versionTracker);
    }
    
    @Test
    public void gdprConfiguration() throws InterruptedException {
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid")
                .base64encoding(false);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore)
                .threadPoolSize(10);
        GdprConfiguration gdprConfiguration = new GdprConfiguration(Basis.CONSENT, "id", "ver", "desc");
        TrackerController trackerController = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, gdprConfiguration, emitterConfiguration);
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
    public void withoutGdprConfiguration() throws InterruptedException {
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid")
                .base64encoding(false);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore)
                .threadPoolSize(10);
        TrackerController trackerController = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, emitterConfiguration);
        GdprController gdprController = trackerController.getGdpr();

        // Check gdpr settings
        assertFalse(gdprController.isEnabled());

        // Check gdpr context not added
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        List<EmitterEvent> events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        Payload payload = events.get(0).payload;
        String contexts = (String) payload.getMap().get("co");
        assertFalse(contexts.contains("\"basisForProcessing\""));

        // Check gdpr can be enabled again
        gdprController.reset(Basis.CONTRACT, "id2", "1", "desc");
        gdprController.enable();
        assertEquals(Basis.CONTRACT, gdprController.getBasisForProcessing());
        assertEquals("id2", gdprController.getDocumentId());
        assertTrue(gdprController.isEnabled());

        // Check gdpr context added
        trackerController.track(new Structured("category", "action"));
        for (int i = 0; eventStore.getSize() < 1 && i < 10; i++) {
            Thread.sleep(1000);
        }
        events = eventStore.getEmittableEvents(10);
        eventStore.removeAllEvents();
        assertEquals(1, events.size());
        payload = events.get(0).payload;
        contexts = (String) payload.getMap().get("co");
        assertTrue(contexts.contains("\"basisForProcessing\":\"contract\""));
        assertTrue(contexts.contains("\"documentId\":\"id2\""));
    }

    @Test
    public void globalContextsConfiguration() throws InterruptedException {
        MockEventStore eventStore = new MockEventStore();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfiguration = new NetworkConfiguration("fake-url", HttpMethod.POST);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration("appid")
                .base64encoding(false);
        EmitterConfiguration emitterConfiguration = new EmitterConfiguration()
                .eventStore(eventStore);
        GlobalContextsConfiguration gcConfiguration = new GlobalContextsConfiguration(null);
        gcConfiguration.add("k1", new GlobalContext(Collections.singletonList(new SelfDescribingJson("schema", new HashMap<String, Object>() {{
            put("key", "value1");
        }}))));
        TrackerController trackerController = Snowplow.createTracker(context, "namespace", networkConfiguration, trackerConfiguration, gcConfiguration, emitterConfiguration);
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

    @Test
    public void activatesServerAnonymisationInEmitter() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfig = new NetworkConfiguration("example.com");
        EmitterConfiguration emitterConfig = new EmitterConfiguration();
        emitterConfig.serverAnonymisation(true);

        TrackerController tracker = Snowplow.createTracker(context, String.valueOf(Math.random()), networkConfig, emitterConfig);
        assertTrue(tracker.getEmitter().isServerAnonymisation());
    }

    @Test
    public void anonymisesUserIdentifiersIfAnonymousUserTracking() throws InterruptedException, JSONException {
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,200);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        NetworkConfiguration networkConfig = new NetworkConfiguration(networkConnection);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("app1");
        trackerConfig.userAnonymisation = true;
        trackerConfig.sessionContext = true;
        trackerConfig.platformContext = true;
        trackerConfig.base64encoding = false;

        Snowplow.removeAllTrackers();
        TrackerController tracker = Snowplow.createTracker(context, String.valueOf(Math.random()), networkConfig, trackerConfig);
        assertTrue(tracker.isUserAnonymisation());

        tracker.track(new Structured("category", "action"));

        for (int i = 0; i < 10 && (networkConnection.countRequests() == 0); i++) {
            Thread.sleep(1000);
        }

        TestCase.assertEquals(1, networkConnection.countRequests());
        Request request = networkConnection.getAllRequests().get(0);
        JSONArray entities = new JSONObject((String) request.payload.getMap().get("co"))
                .getJSONArray("data");
        JSONObject sessionContext = null;
        JSONObject platformContext = null;
        for (int i = 0; i < entities.length(); i++) {
            if (entities.getJSONObject(i).getString("schema").equals(TrackerConstants.SESSION_SCHEMA)) {
                sessionContext = entities.getJSONObject(i).getJSONObject("data");
            } else if (entities.getJSONObject(i).getString("schema").equals(TrackerConstants.MOBILE_SCHEMA)) {
                platformContext = entities.getJSONObject(i).getJSONObject("data");
            }
        }

        assertEquals("00000000-0000-0000-0000-000000000000", sessionContext.getString("userId"));
        assertFalse(platformContext.has("androidIdfa"));
    }

    @Test
    public void trackerReturnsTrackedEventId() throws InterruptedException, JSONException {
        // Setup tracker
        MockNetworkConnection networkConnection = new MockNetworkConnection(GET,200);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        NetworkConfiguration networkConfig = new NetworkConfiguration(networkConnection);
        Snowplow.removeAllTrackers();
        TrackerController tracker = Snowplow.createTracker(context, String.valueOf(Math.random()), networkConfig);

        // Track event
        UUID eventId = tracker.track(new Structured("category", "action"));
        for (int i = 0; i < 10 && (networkConnection.countRequests() == 0); i++) {
            Thread.sleep(1000);
        }
        TestCase.assertEquals(1, networkConnection.countRequests());
        Request request = networkConnection.getAllRequests().get(0);

        // Check eid field
        String trackedEventId = (String) request.payload.getMap().get("eid");
        assertEquals(eventId.toString(), trackedEventId);
    }
}
