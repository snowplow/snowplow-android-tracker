package com.snowplowanalytics.snowplow.globalcontexts;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.snowplowanalytics.snowplow.internal.tracker.SchemaRule;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class SchemaRuleSetTest {

    @Test
    public void testSchemaRule() {
        SchemaRule twoPartVendor = SchemaRule.build("iglu:com.acme/*/jsonschema/*-*-*");
        assertNotNull(twoPartVendor);

        // version and event wildcard
        assertTrue(twoPartVendor.matchWithSchema("iglu:com.acme/event/jsonschema/1-0-0"));
        assertFalse(twoPartVendor.matchWithSchema("iglu:com.snowplow/event/jsonschema/1-0-0"));

        SchemaRule equalRule = SchemaRule.build("iglu:com.acme/*/jsonschema/*-*-*");
        assertEquals(twoPartVendor, equalRule);

        SchemaRule threePartVendor = SchemaRule.build("iglu:com.acme.marketing/*/jsonschema/*-*-*");
        assertNotNull(threePartVendor);

        SchemaRule validVendorWildcard = SchemaRule.build("iglu:com.acme.*/*/jsonschema/*-*-*");
        assertNotNull(validVendorWildcard);

        SchemaRule invalidVendorWildcard = SchemaRule.build("iglu:com.acme.*.whoops/*/jsonschema/*-*-*");
        assertNull(invalidVendorWildcard);

        // vendor matching
        assertTrue(validVendorWildcard.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"));
        assertFalse(validVendorWildcard.matchWithSchema("iglu:com.snowplow/event/jsonschema/1-0-0"));

        // vendor parts need to match in length, i.e. com.acme.* will not match com.acme.marketing.foo, only vendors of the form com.acme.x
        assertFalse(validVendorWildcard.matchWithSchema("iglu:com.acme.marketing.foo/event/jsonschema/1-0-0"));
    }

    @Test
    public void testSchemaRuleSet() {
        String acme = "iglu:com.acme.*/*/jsonschema/*-*-*";
        String snowplow = "iglu:com.snowplow.*/*/jsonschema/*-*-*";
        String snowplowTest = "iglu:com.snowplow.test/*/jsonschema/*-*-*";
        SchemaRuleSet ruleset = SchemaRuleSet.buildRuleSet(Arrays.asList(acme, snowplow), Arrays.asList(snowplowTest));
        List<String> allowed = Arrays.asList(acme, snowplow);
        assertEquals(ruleset.getAllowed(), allowed);
        List<String> denied = Arrays.asList(snowplowTest);
        assertEquals(ruleset.getDenied(), denied);

        // matching
        assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"));
        assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"));
        assertFalse(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"));
        assertFalse(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"));
    }

    @Test
    public void testSchemaRulesetOnlyDenied() {
        String snowplowTest = "iglu:com.snowplow.test/*/jsonschema/*-*-*";
        SchemaRuleSet ruleset = SchemaRuleSet.buildRuleSetWithDeniedList(Arrays.asList(snowplowTest));
        List<String> allowed = new ArrayList<>();
        assertEquals(ruleset.getAllowed(), allowed);
        List<String> denied = Arrays.asList(snowplowTest);
        assertEquals(ruleset.getDenied(), denied);

        // matching
        assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"));
        assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"));
        assertFalse(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"));
        assertTrue(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"));
    }

    @Test
    public void testSchemaRulesetOnlyAllowed() {
        String acme = "iglu:com.acme.*/*/jsonschema/*-*-*";
        String snowplow = "iglu:com.snowplow.*/*/jsonschema/*-*-*";
        SchemaRuleSet ruleset = SchemaRuleSet.buildRuleSetWithAllowedList(Arrays.asList(acme, snowplow));
        List<String> allowed = Arrays.asList(acme, snowplow);
        assertEquals(ruleset.getAllowed(), allowed);
        List<String> denied = new ArrayList<>();
        assertEquals(ruleset.getDenied(), denied);

        // matching
        assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"));
        assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"));
        assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"));
        assertFalse(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"));
    }
}
