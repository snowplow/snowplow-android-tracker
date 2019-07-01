package com.snowplowanalytics.snowplow.tracker.contexts.global;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants.SCHEMA_APPLICATION_INSTALL;

public class GlobalContextTest extends AndroidTestCase {

    private Tracker tracker;

    private GlobalContext mobileContext;
    private GlobalContext appContext;

    private RuleSet acceptRuleSet1;
    private RuleSet rejectRuleSet1;

    @Override
    protected void setUp() {

        rejectRuleSet1 =
                new RuleSet(null, "iglu:com.snowplowanalytics.snowplow/*/jsonschema/1-0-1");

        acceptRuleSet1 =
                new RuleSet("iglu:com.snowplowanalytics.snowplow/*/jsonschema/*-*-*", null);

        mobileContext = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1");

        appContext = new ContextGenerator() {
            @Override
            public String tag() {
                return "appContext";
            }

            @Override
            public SelfDescribingJson generate(TrackerPayload payload, String eventType, String eventSchema) {
                return new SelfDescribingJson("iglu:com.snowplowanalytics.mobile/application/jsonschema/1-0-0");
            }
        };

        // prepare a tracker
        Tracker.close();

        Emitter emitter = new Emitter
                .EmitterBuilder("testUrl", getContext())
                .tick(0)
                .emptyLimit(0)
                .build();

        Subject subject = new Subject
                .SubjectBuilder()
                .context(getContext())
                .build();

        tracker = new Tracker.TrackerBuilder(emitter, "GlobalContextTest", "myAppId", getContext())
                .subject(subject)
                .platform(DevicePlatforms.ServerSideApp)
                .base64(false)
                .level(LogLevel.VERBOSE)
                .threadCount(1)
                .sessionContext(false)
                .mobileContext(false)
                .geoLocationContext(false)
                .foregroundTimeout(5)
                .backgroundTimeout(5)
                .sessionCheckInterval(15)
                .timeUnit(TimeUnit.SECONDS)
                .build();
    }

    public void testIsValidRule() {
        assertTrue(GlobalContextUtils.isValidRule(acceptRuleSet1.getAccept().get(0)));
        assertFalse(GlobalContextUtils.isValidRule(acceptRuleSet1.getReject().get(0)));
        assertTrue(GlobalContextUtils.isValidRule(rejectRuleSet1.getReject().get(0)));
        assertFalse(GlobalContextUtils.isValidRule(rejectRuleSet1.getAccept().get(0)));
    }

    public void testClearGlobalContexts() {
        tracker.addGlobalContexts(Arrays.asList(mobileContext, appContext));
        assertEquals(tracker.getGlobalContexts().size(), 2);
        tracker.clearGlobalContexts();
        assertEquals(tracker.getGlobalContexts().size(), 0);
    }

    public void testMatchSchemaAgainstRuleSet() {
        String mobileContext = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1";
        assertTrue(GlobalContextUtils.matchSchemaAgainstRuleSet(acceptRuleSet1, mobileContext));
        assertFalse(GlobalContextUtils.matchSchemaAgainstRuleSet(rejectRuleSet1, mobileContext));
    }

    public void testMatchSchemaAgainstRule() {
        String rule1 = "iglu:com.snowplowanalytics.*/*/jsonschema/1-*-*";
        String rule2 = "iglu:com.*.*/*/jsonschema/1-*-*";
        String rule3 = "iglu:com.snowplowanalytics.*/payload_data/jsonschema/1-*-4";
        String rule4 = "iglu:com.snowplowanalytics.snowplow/*/jsonschema/*-*-*";
        String firstClassSchema = "iglu:com.snowplowanalytics.snowplow/payload_data/jsonschema/1-0-4";

        assertTrue(GlobalContextUtils.matchSchemaAgainstRule(rule1, firstClassSchema));
        assertFalse(GlobalContextUtils.matchSchemaAgainstRule(rule2, firstClassSchema));
        assertFalse(GlobalContextUtils.matchSchemaAgainstRule(rule3, firstClassSchema));
        assertTrue(GlobalContextUtils.matchSchemaAgainstRule(rule4, firstClassSchema));
    }

    public void testGetUriSubparts() {
        String uri = "iglu:com.snowplowanalytics.snowplow/mobile_context/jsonschema/1-0-1";
        String[] uriSubparts = GlobalContextUtils.getUriSubparts(uri);
        assertEquals(uriSubparts[0], "com.snowplowanalytics.snowplow");
        assertEquals(uriSubparts[1], "mobile_context");
        assertEquals(uriSubparts[2], "1");
        assertEquals(uriSubparts[3], "0");
        assertEquals(uriSubparts[4], "1");

        String rule = "iglu:com.snowplowanalytics.*/mobile_context/jsonschema/*-*-*";
        String[] ruleSubparts = GlobalContextUtils.getUriSubparts(rule);
        assertEquals(ruleSubparts[0], "com.snowplowanalytics.*");
        assertEquals(ruleSubparts[1], "mobile_context");
        assertEquals(ruleSubparts[2], "*");
        assertEquals(ruleSubparts[3], "*");
        assertEquals(ruleSubparts[4], "*");
    }

    public void testValidateVersion() {
        assertTrue(GlobalContextUtils.validateVersion(new String[]{"*", "*", "*"}));
        assertTrue(GlobalContextUtils.validateVersion(new String[]{"1", "*", "*"}));
        assertTrue(GlobalContextUtils.validateVersion(new String[]{"1", "0", "*"}));
        assertTrue(GlobalContextUtils.validateVersion(new String[]{"1", "0", "0"}));

        assertFalse(GlobalContextUtils.validateVersion(new String[]{"1", "*", "0"}));
        assertFalse(GlobalContextUtils.validateVersion(new String[]{"*", "0", "0"}));
        assertFalse(GlobalContextUtils.validateVersion(new String[]{"*", "*", "0"}));
    }

    public void testValidateVendor() {
        // A valid vendor without wildcard is accepted
        assertTrue(GlobalContextUtils.validateVendor("com.acme.marketing"));

        // A valid vendor with wildcard after the 2 leftmost sub-part is accepted
        assertTrue(GlobalContextUtils.validateVendor("com.acme.*"));

        // A wildcard can not be used in the 2 leftmost sub-part of a vendor
        assertFalse(GlobalContextUtils.validateVendor("*.acme.*"));

        // A vendor with asterisk out of order is rejected
        assertFalse(GlobalContextUtils.validateVendor("com.acme.*.marketing"));
    }

    public void testValidateVendorParts() {
        assertTrue(GlobalContextUtils.validateVendorParts(new String[]{"com", "acme", "*"}));
        assertTrue(GlobalContextUtils.validateVendorParts(new String[]{"com", "acme"}));
        assertTrue(GlobalContextUtils.validateVendorParts(new String[]{"com", "acme", "marketing"}));
        assertTrue(GlobalContextUtils.validateVendorParts(new String[]{"com", "acme", "marketing", "*", "*"}));

        assertFalse(GlobalContextUtils.validateVendorParts(new String[]{"com", "*", "marketing"}));
        assertFalse(GlobalContextUtils.validateVendorParts(new String[]{"*", "acme"}));
        assertFalse(GlobalContextUtils.validateVendorParts(new String[]{"com", "acme", "*", "en"}));
    }

    public void testMatchVendor() {
        assertTrue(GlobalContextUtils.matchVendor("com.acme.marketing", "com.acme.marketing"));
        assertTrue(GlobalContextUtils.matchVendor("com.acme.*", "com.acme.marketing"));

        assertFalse(GlobalContextUtils.matchVendor("com.acme", "com.acme.marketing"));
        assertFalse(GlobalContextUtils.matchVendor("*.*", "com.acme"));
        assertFalse(GlobalContextUtils.matchVendor("*.*.*", "com.acme.marketing"));
    }

    public void testMatchPart() {
        assertFalse(GlobalContextUtils.matchPart(null, ""));
        assertFalse(GlobalContextUtils.matchPart("", null));

        assertTrue(GlobalContextUtils.matchPart("*", "com"));
        assertTrue(GlobalContextUtils.matchPart("acme", "acme"));
    }

    public void testComputeContextPrimitive() {
        ArrayList<SelfDescribingJson> computedGlobalContexts = new ArrayList<>();
        ContextPrimitive primitive = (ContextPrimitive) mobileContext;
        TrackerPayload payload = new TrackerPayload();
        payload.add("e", "pv");
        String eventType = "pv";
        String eventSchema = TrackerConstants.SCHEMA_PAYLOAD_DATA;

        GlobalContextUtils.computeContextPrimitive(payload, computedGlobalContexts, primitive, eventType, eventSchema);
        assertEquals(computedGlobalContexts.size(), 1);
        assertEquals(computedGlobalContexts.get(0), primitive);

        final SelfDescribingJson appCtx = new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/application/jsonschema/1-0-1");
        ContextPrimitive primitive2 = new ContextGenerator() {
            @Override
            public String tag() {
                return "primitive2";
            }

            @Override
            public SelfDescribingJson generate(TrackerPayload payload, String eventType, String eventSchema) {
                return appCtx;
            }
        };

        GlobalContextUtils.computeContextPrimitive(payload, computedGlobalContexts, primitive2, eventType, eventSchema);
        assertEquals(computedGlobalContexts.size(), 2);
        assertEquals(computedGlobalContexts.get(1), appCtx);
    }

    public void testGetSchema() {
        TrackerPayload payload = new TrackerPayload();
        assertEquals(GlobalContextUtils.getSchema(payload), TrackerConstants.SCHEMA_PAYLOAD_DATA);

        SelfDescribingJson sdj =
                new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0",
                        new SelfDescribingJson("iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1"));
        payload.add(Parameters.UNSTRUCTURED, sdj.toString());

        assertEquals(GlobalContextUtils.getSchema(payload), "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1");

        TrackerPayload payload2 = new TrackerPayload();
        String encodedPayload = "eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd2FuYWx5dGljcy5zbm93cGxvd1wvdW5zdHJ1Y3RfZXZlbn" +
                        "RcL2pzb25zY2hlbWFcLzEtMC0wIiwiZGF0YSI6eyJzY2hlbWEiOiJpZ2x1OmNvbS5zbm93cGxvd" +
                        "2FuYWx5dGljcy5zbm93cGxvd1wvbGlua19jbGlja1wvanNvbnNjaGVtYVwvMS0wLTEiLCJkYXRhIjp7fX19";
        payload2.add(Parameters.UNSTRUCTURED_ENCODED, encodedPayload);

        assertEquals(GlobalContextUtils.getSchema(payload2), "iglu:com.snowplowanalytics.snowplow/link_click/jsonschema/1-0-1");
    }

    public void testRemoveContext() {

        tracker.addGlobalContext(
                new RuleSetProvider(
                        "ruleSetExample",
                        new RuleSet("iglu:com.snowplowanalytics.*/*/jsonschema/*-*-*", null),
                        (ContextPrimitive) mobileContext
                )
        );

        Map<String, String> attributes = new HashMap<>();
        attributes.put("test-key-1", "test-value-1");
        SelfDescribingJson testSDJ = new SelfDescribingJson("sdjExample", "iglu:com.snowplowanalytics.snowplow/test_sdj/jsonschema/1-0-1", attributes);
        tracker.addGlobalContext(testSDJ);

        tracker.addGlobalContext(
                new ContextGenerator() {
                    @Override
                    public SelfDescribingJson generate(TrackerPayload payload, String eventType, String eventSchema) {
                        return new SelfDescribingJson(SCHEMA_APPLICATION_INSTALL);
                    }

                    @Override
                    public String tag() {
                        return "testCtx";
                    }
                }
        );

        assertEquals(tracker.getGlobalContexts().size(), 3);

        tracker.removeGlobalContext("ruleSetExample");

        assertEquals(tracker.getGlobalContexts().size(), 2);

        tracker.removeGlobalContexts(Arrays.asList("testCtx", "sdjExample"));

        assertEquals(tracker.getGlobalContexts().size(), 0);
    }
}
