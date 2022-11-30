/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
