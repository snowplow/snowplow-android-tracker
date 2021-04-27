package com.snowplowanalytics.snowplow.globalcontexts;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.AbstractPrimitive;
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.event.Timing;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProvider;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerEvent;
import com.snowplowanalytics.snowplow.network.HttpMethod;
import com.snowplowanalytics.snowplow.network.Protocol;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

class GlobalContextGenerator implements ContextGenerator {

    @Override
    public boolean filterEvent(@NonNull InspectableEvent event) {
        return "StringToMatch".equals(event.getPayload().get(Parameters.SE_CATEGORY));
    }

    @NonNull
    @Override
    public List<SelfDescribingJson> generateContexts(@NonNull InspectableEvent event) {
        return Collections.singletonList(new SelfDescribingJson("schema", new HashMap<String, String>() {{
            put("key", "value");
        }}));
    }
}


@RunWith(AndroidJUnit4.class)
public class GlobalContextTest {

    @Test
    public void testGlobalContexts() {
        SelfDescribingJson sdj = new SelfDescribingJson("schema", new HashMap<String, String>() {{ put("key", "value"); }});
        GlobalContext staticGC = new GlobalContext(Collections.singletonList(sdj));
        GlobalContext generatorGC = new GlobalContext(new GlobalContextGenerator());
        GlobalContext blockGC = new GlobalContext(new FunctionalGenerator() {
            @Nullable
            @Override
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return Collections.singletonList(
                        new SelfDescribingJson("schemaBlock", new HashMap<String, String>() {{ put("key", "value"); }})
                );
            }
        });
        TrackerController tracker = getTracker(new HashMap<String,GlobalContext>() {{
            put("static", staticGC);
            put("generator", generatorGC);
            put("block", blockGC);
        }});

        Set<String> result = tracker.getGlobalContexts().getTags();
        Set<String> expected = Set.of("static", "generator", "block");
        assertEquals(expected, result);

        // Can't remove a not existing tag
        GlobalContext removedGC = tracker.getGlobalContexts().remove("notExistingTag");
        assertNull(removedGC);
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of("static","generator","block");
        assertEquals(expected, result);

        // Remove an existing tag
        removedGC = tracker.getGlobalContexts().remove("static");
        assertNotNull(removedGC);
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of("generator","block");
        assertEquals(expected, result);

        // Add a not existing tag
        assertTrue(tracker.getGlobalContexts().add("static", staticGC));
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of("static","generator","block");
        assertEquals(expected, result);

        // Can't add an existing tag
        assertFalse(tracker.getGlobalContexts().add("static", staticGC));
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of("static","generator","block");
        assertEquals(expected, result);
    }

    @Test
    public void testAddRemoveGlobalContexts() {
        SelfDescribingJson sdj = new SelfDescribingJson("schema", new HashMap<String, String>() {{ put("key", "value"); }});
        GlobalContext staticGC = new GlobalContext(Collections.singletonList(sdj));
        TrackerController tracker = getTracker(null);

        Set<String> result = tracker.getGlobalContexts().getTags();
        Set<String> expected = Set.of();
        assertEquals(expected, result);

        // Can't remove a not existing tag
        GlobalContext removedGC = tracker.getGlobalContexts().remove("notExistingTag");
        assertNull(removedGC);

        // Add a not existing tag
        assertTrue(tracker.getGlobalContexts().add("static", staticGC));
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of("static");
        assertEquals(expected, result);

        // Remove an existing tag
        removedGC = tracker.getGlobalContexts().remove("static");
        assertNotNull(removedGC);
        result = tracker.getGlobalContexts().getTags();
        expected = Set.of();
        assertEquals(expected, result);
    }

    @Test
    public void testStaticGenerator() {
        SelfDescribingJson sdj = new SelfDescribingJson("schema", new HashMap<String, String>() {{ put("key", "value"); }});
        GlobalContext staticGC = new GlobalContext(Collections.singletonList(sdj));

        AbstractPrimitive event = new Structured("Category", "Action");
        TrackerEvent trackerEvent = new TrackerEvent(event);

        List<SelfDescribingJson> contexts = staticGC.generateContexts(trackerEvent);
        assertEquals(1, contexts.size());
        assertEquals("schema", contexts.get(0).getMap().get("schema"));
    }

    @Test
    public void testStaticGeneratorWithFilter() {
        String stringToMatch = "StringToMatch";
        SelfDescribingJson sdj = new SelfDescribingJson("schema", new HashMap<String, String>() {{ put("key", "value"); }});
        GlobalContext filterMatchingGC = new GlobalContext(Collections.singletonList(sdj), new FunctionalFilter() {
            @Override
            public boolean apply(@NonNull InspectableEvent event) {
                return stringToMatch.equals(event.getPayload().get(Parameters.SE_CATEGORY));
            }
        });

        AbstractPrimitive event = new Structured(stringToMatch, "Action");
        TrackerEvent trackerEvent = new TrackerEvent(event);

        List<SelfDescribingJson> contexts = filterMatchingGC.generateContexts(trackerEvent);
        assertEquals(1, contexts.size());
        assertEquals("schema", contexts.get(0).getMap().get("schema"));

        // Not Matching
        GlobalContext filterNotMatchingGC = new GlobalContext(Collections.singletonList(sdj), new FunctionalFilter() {
            @Override
            public boolean apply(@NonNull InspectableEvent event) {
                return false;
            }
        });

        contexts = filterNotMatchingGC.generateContexts(trackerEvent);
        assertEquals(0, contexts.size());
    }

    @Test
    public void testStaticGeneratorWithRuleSet() {
        String allowed = "iglu:com.snowplowanalytics.*/*/jsonschema/*-*-*";
        String denied = "iglu:com.snowplowanalytics.mobile/*/jsonschema/*-*-*";
        SchemaRuleSet ruleset = SchemaRuleSet.buildRuleSet(List.of(allowed), List.of(denied));

        SelfDescribingJson sdj = new SelfDescribingJson("schema", new HashMap<String, String>() {{ put("key", "value"); }});
        GlobalContext rulesetGC = new GlobalContext(Collections.singletonList(sdj), ruleset);

        // Not matching primitive event
        AbstractPrimitive event = new Structured("Category", "Action");
        TrackerEvent trackerEvent = new TrackerEvent(event);
        List<SelfDescribingJson> contexts = rulesetGC.generateContexts(trackerEvent);
        assertEquals(0, contexts.size());

        // Not matching self-describing event with mobile schema
        AbstractSelfDescribing selfDescribingEvent = new ScreenView("Name", null)
                .type("Type");
        trackerEvent = new TrackerEvent(selfDescribingEvent);
        contexts = rulesetGC.generateContexts(trackerEvent);
        assertEquals(0, contexts.size());

        // Matching self-describing event with general schema
        selfDescribingEvent = new Timing("Category", "Variable", 123)
                .label("Label");
        trackerEvent = new TrackerEvent(selfDescribingEvent);
        contexts = rulesetGC.generateContexts(trackerEvent);
        assertEquals(1, contexts.size());
        assertEquals("schema", contexts.get(0).getMap().get("schema"));
    }

    @Test
    public void testBlockGenerator() {
        GlobalContext blockGC = new GlobalContext(new FunctionalGenerator() {
            @Nullable
            @Override
            public List<SelfDescribingJson> apply(@NonNull InspectableEvent event) {
                return Collections.singletonList(
                        new SelfDescribingJson("schemaBlock", new HashMap<String, String>() {{ put("key", "value"); }})
                );
            }
        });

        AbstractPrimitive event = new Structured("Category", "Action");
        TrackerEvent trackerEvent = new TrackerEvent(event);

        List<SelfDescribingJson> contexts = blockGC.generateContexts(trackerEvent);
        assertEquals(1, contexts.size());
        assertEquals("schemaBlock", contexts.get(0).getMap().get("schema"));
    }

    @Test
    public void testContextGenerator() {
        GlobalContext contextGeneratorGC = new GlobalContext(new GlobalContextGenerator());
        AbstractPrimitive event = new Structured("StringToMatch", "Action");
        TrackerEvent trackerEvent = new TrackerEvent(event);

        List<SelfDescribingJson> contexts = contextGeneratorGC.generateContexts(trackerEvent);
        assertEquals(1, contexts.size());
        assertEquals("schema", contexts.get(0).getMap().get("schema"));
    }

    // Service methods

    private TrackerController getTracker(Map<String, GlobalContext> generators) {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        NetworkConfiguration networkConfig = new NetworkConfiguration("com.acme.fake", HttpMethod.POST);
        TrackerConfiguration trackerConfig = new TrackerConfiguration("anAppId")
                .platformContext(true)
                .geoLocationContext(false)
                .base64encoding(false)
                .sessionContext(true);
        GlobalContextsConfiguration gcConfig = new GlobalContextsConfiguration(generators);
        return Snowplow.createTracker(context, "aNamespace", networkConfig, trackerConfig, gcConfig);
    }
}
