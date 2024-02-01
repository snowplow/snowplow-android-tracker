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

import com.snowplowanalytics.core.tracker.SchemaRule
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

/**
 * Use this class to add a [GlobalContext] to specific events only, based on the event schema.
 * New SchemaRuleSets are created using the companion object `build` methods.
 * 
 */
class SchemaRuleSet private constructor(allowed: List<String?>, denied: List<String?>) {
    private val rulesAllowed: MutableList<SchemaRule> = ArrayList()
    private val rulesDenied: MutableList<SchemaRule> = ArrayList()

    init {
        for (rule in allowed) {
            val schemaRule = rule?.let { SchemaRule.build(it) }
            schemaRule?.let { rulesAllowed.add(schemaRule) }
        }
        for (rule in denied) {
            val schemaRule = rule?.let { SchemaRule.build(it) }
            schemaRule?.let { rulesDenied.add(schemaRule) }
        }
    }

    val allowed: List<String>
        get() {
            val result: MutableList<String> = ArrayList(rulesAllowed.size)
            for (schemaRule in rulesAllowed) {
                result.add(schemaRule.rule)
            }
            return result
        }
    
    val denied: List<String>
        get() {
            val result: MutableList<String> = ArrayList(rulesDenied.size)
            for (schemaRule in rulesDenied) {
                result.add(schemaRule.rule)
            }
            return result
        }
    
    val filter: FunctionalFilter
        get() = object : FunctionalFilter() {
            override fun apply(event: InspectableEvent): Boolean {
                return matchWithSchema(event.schema)
            }
        }

    fun matchWithSchema(schema: String?): Boolean {
        if (schema == null) return false
        
        for (rule in rulesDenied) {
            if (rule.matchWithSchema(schema)) return false
        }
        
        if (rulesAllowed.size == 0) return true
        
        for (rule in rulesAllowed) {
            if (rule.matchWithSchema(schema)) return true
        }
        return false
    }

    /**
     * Build a [SchemaRuleSet]. Provide schema strings for events that entities will either be attached to
     * (`allowed` schemas) or not (`denied` schemas).
     * Schema strings can contain wildcards, e.g. 
     * `iglu:com.acme.marketing / * / jsonschema / *-*-*` (without spaces).
     * 
     * They follow the same five-part format as an Iglu URI `iglu:vendor/event_name/format/version`,
     * with the exception that a wildcard can be used to refer to all cases.
     * 
     * The parts of a rule are wildcarded with certain guidelines:
     * - asterisks cannot be used for the protocol (i.e. schemas always start with `iglu:`)
     * - version matching must be specified like so: `––`, where any part of the versioning can be defined, e.g. `1-–`, 
     * but only sequential parts can be wildcarded, e.g. `1--1` is invalid but `1-1–*` is valid
     * - vendors cannot be defined with non-wildcarded parts between wildcarded parts: `com.acme.*.marketing.*` is invalid, while `com.acme.*.*` is valid
     */
    companion object {
        @JvmStatic
        fun buildRuleSet(allowed: List<String>, denied: List<String>): SchemaRuleSet {
            return SchemaRuleSet(allowed, denied)
        }

        @JvmStatic
        fun buildRuleSetWithAllowedList(allowed: List<String>): SchemaRuleSet {
            return buildRuleSet(allowed, ArrayList())
        }

        @JvmStatic
        fun buildRuleSetWithDeniedList(denied: List<String>): SchemaRuleSet {
            return buildRuleSet(ArrayList(), denied)
        }
    }
}
