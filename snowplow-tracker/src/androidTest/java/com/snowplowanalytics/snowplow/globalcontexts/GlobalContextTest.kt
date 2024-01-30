/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.globalcontexts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.tracker.TrackerEvent
import com.snowplowanalytics.snowplow.Snowplow.createTracker
import com.snowplowanalytics.snowplow.configuration.GlobalContextsConfiguration
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.globalcontexts.SchemaRuleSet.Companion.buildRuleSet
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

internal class GlobalContextGenerator : ContextGenerator {
    override fun filterEvent(event: InspectableEvent): Boolean {
        return "StringToMatch" == event.payload[Parameters.SE_CATEGORY]
    }

    override fun generateContexts(event: InspectableEvent): List<SelfDescribingJson> {
        return listOf(SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        }))
    }
}

@RunWith(AndroidJUnit4::class)
class GlobalContextTest {
    @Test
    fun testGlobalContexts() {
        val sdj = SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        })
        val staticGC = GlobalContext(listOf(sdj))
        val generatorGC = GlobalContext(GlobalContextGenerator())
        val blockGC = GlobalContext(object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson> {
                return listOf(
                    SelfDescribingJson("schemaBlock", object : HashMap<String?, String?>() {
                        init {
                            put("key", "value")
                        }
                    })
                )
            }
        })
        val tracker = getTracker(object : HashMap<String, GlobalContext>() {
            init {
                put("static", staticGC)
                put("generator", generatorGC)
                put("block", blockGC)
            }
        })
        var result = tracker.globalContexts.tags
        var expected = mutableSetOf("static", "generator", "block")
        Assert.assertEquals(expected, result)

        // Can't remove a not existing tag
        var removedGC = tracker.globalContexts.remove("notExistingTag")
        Assert.assertNull(removedGC)
        result = tracker.globalContexts.tags
        expected = mutableSetOf("static", "generator", "block")
        Assert.assertEquals(expected, result)

        // Remove an existing tag
        removedGC = tracker.globalContexts.remove("static")
        Assert.assertNotNull(removedGC)
        result = tracker.globalContexts.tags
        expected = mutableSetOf("generator", "block")
        Assert.assertEquals(expected, result)

        // Add a not existing tag
        Assert.assertTrue(tracker.globalContexts.add("static", staticGC))
        result = tracker.globalContexts.tags
        expected = mutableSetOf("static", "generator", "block")
        Assert.assertEquals(expected, result)

        // Can't add an existing tag
        Assert.assertFalse(tracker.globalContexts.add("static", staticGC))
        result = tracker.globalContexts.tags
        expected = mutableSetOf("static", "generator", "block")
        Assert.assertEquals(expected, result)
    }

    @Test
    fun testAddRemoveGlobalContexts() {
        val sdj = SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        })
        val staticGC = GlobalContext(listOf(sdj))
        val tracker = getTracker(null)
        var result = tracker.globalContexts.tags
        var expected = mutableSetOf<String?>()
        Assert.assertEquals(expected, result)

        // Can't remove a not existing tag
        var removedGC = tracker.globalContexts.remove("notExistingTag")
        Assert.assertNull(removedGC)

        // Add a not existing tag
        Assert.assertTrue(tracker.globalContexts.add("static", staticGC))
        result = tracker.globalContexts.tags
        expected = mutableSetOf("static")
        Assert.assertEquals(expected, result)

        // Remove an existing tag
        removedGC = tracker.globalContexts.remove("static")
        Assert.assertNotNull(removedGC)
        result = tracker.globalContexts.tags
        expected = mutableSetOf()
        Assert.assertEquals(expected, result)
    }

    @Test
    fun testStaticGenerator() {
        val sdj = SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        })
        val staticGC = GlobalContext(listOf(sdj))
        val event: AbstractPrimitive = Structured("Category", "Action")
        val trackerEvent = TrackerEvent(event)
        val contexts = staticGC.generateContexts(trackerEvent)
        Assert.assertEquals(1, contexts.size.toLong())
        Assert.assertEquals("schema", contexts[0].map["schema"])
    }

    @Test
    fun testStaticGeneratorWithFilter() {
        val stringToMatch = "StringToMatch"
        val sdj = SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        })
        val filterMatchingGC = GlobalContext(listOf(sdj), object : FunctionalFilter() {
            override fun apply(event: InspectableEvent): Boolean {
                return stringToMatch == event.payload[Parameters.SE_CATEGORY]
            }
        })
        val event: AbstractPrimitive = Structured(stringToMatch, "Action")
        val trackerEvent = TrackerEvent(event)
        var contexts = filterMatchingGC.generateContexts(trackerEvent)
        Assert.assertEquals(1, contexts.size.toLong())
        Assert.assertEquals("schema", contexts[0].map["schema"])

        // Not Matching
        val filterNotMatchingGC = GlobalContext(listOf(sdj), object : FunctionalFilter() {
            override fun apply(event: InspectableEvent): Boolean {
                return false
            }
        })
        contexts = filterNotMatchingGC.generateContexts(trackerEvent)
        Assert.assertEquals(0, contexts.size.toLong())
    }

    @Test
    fun testStaticGeneratorWithRuleSet() {
        val allowed = "iglu:com.snowplowanalytics.*/*/jsonschema/*-*-*"
        val denied = "iglu:com.snowplowanalytics.mobile/*/jsonschema/*-*-*"
        val ruleset = buildRuleSet(listOf(allowed), listOf(denied))
        val sdj = SelfDescribingJson("schema", object : HashMap<String?, String?>() {
            init {
                put("key", "value")
            }
        })
        val rulesetGC = GlobalContext(listOf(sdj), ruleset)

        // Not matching primitive event
        val event: AbstractPrimitive = Structured("Category", "Action")
        var trackerEvent = TrackerEvent(event)
        var contexts = rulesetGC.generateContexts(trackerEvent)
        Assert.assertEquals(0, contexts.size.toLong())

        // Not matching self-describing event with mobile schema
        var selfDescribingEvent: AbstractSelfDescribing = ScreenView("Name", null)
            .type("Type")
        trackerEvent = TrackerEvent(selfDescribingEvent)
        contexts = rulesetGC.generateContexts(trackerEvent)
        Assert.assertEquals(0, contexts.size.toLong())

        // Matching self-describing event with general schema
        selfDescribingEvent = Timing("Category", "Variable", 123)
            .label("Label")
        trackerEvent = TrackerEvent(selfDescribingEvent)
        contexts = rulesetGC.generateContexts(trackerEvent)
        Assert.assertEquals(1, contexts.size.toLong())
        Assert.assertEquals("schema", contexts[0].map["schema"])
    }

    @Test
    fun testBlockGenerator() {
        val blockGC = GlobalContext(object : FunctionalGenerator() {
            override fun apply(event: InspectableEvent): List<SelfDescribingJson> {
                return listOf(
                    SelfDescribingJson("schemaBlock", object : HashMap<String?, String?>() {
                        init {
                            put("key", "value")
                        }
                    })
                )
            }
        })
        val event: AbstractPrimitive = Structured("Category", "Action")
        val trackerEvent = TrackerEvent(event)
        val contexts = blockGC.generateContexts(trackerEvent)
        Assert.assertEquals(1, contexts.size.toLong())
        Assert.assertEquals("schemaBlock", contexts[0].map["schema"])
    }

    @Test
    fun testContextGenerator() {
        val contextGeneratorGC = GlobalContext(GlobalContextGenerator())
        val event: AbstractPrimitive = Structured("StringToMatch", "Action")
        val trackerEvent = TrackerEvent(event)
        val contexts = contextGeneratorGC.generateContexts(trackerEvent)
        Assert.assertEquals(1, contexts.size.toLong())
        Assert.assertEquals("schema", contexts[0].map["schema"])
    }

    // Service methods
    private fun getTracker(generators: MutableMap<String, GlobalContext>?): TrackerController {
        val context = InstrumentationRegistry.getInstrumentation().context
        val networkConfig = NetworkConfiguration("com.acme.fake", HttpMethod.POST)
        val trackerConfig = TrackerConfiguration("anAppId")
            .platformContext(true)
            .geoLocationContext(false)
            .base64encoding(false)
            .sessionContext(true)
        val gcConfig = GlobalContextsConfiguration(generators)
        return createTracker(
            context,
            "aNamespace" + Math.random().toString(),
            networkConfig,
            trackerConfig,
            gcConfig
        )
    }
}
