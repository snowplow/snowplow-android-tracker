package com.snowplowanalytics.snowplow.globalcontexts

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.tracker.SchemaRule.Companion.build
import com.snowplowanalytics.snowplow.globalcontexts.SchemaRuleSet.Companion.buildRuleSet
import com.snowplowanalytics.snowplow.globalcontexts.SchemaRuleSet.Companion.buildRuleSetWithAllowedList
import com.snowplowanalytics.snowplow.globalcontexts.SchemaRuleSet.Companion.buildRuleSetWithDeniedList
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class SchemaRuleSetTest {
    @Test
    fun testSchemaRule() {
        val twoPartVendor = build("iglu:com.acme/*/jsonschema/*-*-*")
        Assert.assertNotNull(twoPartVendor)

        // version and event wildcard
        Assert.assertTrue(twoPartVendor!!.matchWithSchema("iglu:com.acme/event/jsonschema/1-0-0"))
        Assert.assertFalse(twoPartVendor.matchWithSchema("iglu:com.snowplow/event/jsonschema/1-0-0"))
        val equalRule = build("iglu:com.acme/*/jsonschema/*-*-*")
        Assert.assertEquals(twoPartVendor, equalRule)
        val threePartVendor = build("iglu:com.acme.marketing/*/jsonschema/*-*-*")
        Assert.assertNotNull(threePartVendor)
        val validVendorWildcard = build("iglu:com.acme.*/*/jsonschema/*-*-*")
        Assert.assertNotNull(validVendorWildcard)
        val invalidVendorWildcard = build("iglu:com.acme.*.whoops/*/jsonschema/*-*-*")
        Assert.assertNull(invalidVendorWildcard)

        // vendor matching
        Assert.assertTrue(validVendorWildcard!!.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"))
        Assert.assertFalse(validVendorWildcard.matchWithSchema("iglu:com.snowplow/event/jsonschema/1-0-0"))

        // vendor parts need to match in length, i.e. com.acme.* will not match com.acme.marketing.foo, only vendors of the form com.acme.x
        Assert.assertFalse(validVendorWildcard.matchWithSchema("iglu:com.acme.marketing.foo/event/jsonschema/1-0-0"))
    }

    @Test
    fun testSchemaRuleSet() {
        val acme = "iglu:com.acme.*/*/jsonschema/*-*-*"
        val snowplow = "iglu:com.snowplow.*/*/jsonschema/*-*-*"
        val snowplowTest = "iglu:com.snowplow.test/*/jsonschema/*-*-*"
        val ruleset = buildRuleSet(listOf(acme, snowplow), listOf(snowplowTest))
        val allowed = listOf(acme, snowplow)
        Assert.assertEquals(ruleset.allowed, allowed)
        val denied = listOf(snowplowTest)
        Assert.assertEquals(ruleset.denied, denied)

        // matching
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"))
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"))
        Assert.assertFalse(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"))
        Assert.assertFalse(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"))
    }

    @Test
    fun testSchemaRulesetOnlyDenied() {
        val snowplowTest = "iglu:com.snowplow.test/*/jsonschema/*-*-*"
        val ruleset = buildRuleSetWithDeniedList(listOf(snowplowTest))
        val allowed: List<String> = ArrayList()
        Assert.assertEquals(ruleset.allowed, allowed)
        val denied = listOf(snowplowTest)
        Assert.assertEquals(ruleset.denied, denied)

        // matching
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"))
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"))
        Assert.assertFalse(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"))
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"))
    }

    @Test
    fun testSchemaRulesetOnlyAllowed() {
        val acme = "iglu:com.acme.*/*/jsonschema/*-*-*"
        val snowplow = "iglu:com.snowplow.*/*/jsonschema/*-*-*"
        val ruleset = buildRuleSetWithAllowedList(listOf(acme, snowplow))
        val allowed = listOf(acme, snowplow)
        Assert.assertEquals(ruleset.allowed, allowed)
        val denied: List<String> = ArrayList()
        Assert.assertEquals(ruleset.denied, denied)

        // matching
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.acme.marketing/event/jsonschema/1-0-0"))
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.marketing/event/jsonschema/1-0-0"))
        Assert.assertTrue(ruleset.matchWithSchema("iglu:com.snowplow.test/event/jsonschema/1-0-0"))
        Assert.assertFalse(ruleset.matchWithSchema("iglu:com.brand/event/jsonschema/1-0-0"))
    }
}
