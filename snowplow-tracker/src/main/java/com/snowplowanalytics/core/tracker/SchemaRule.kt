/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.tracker

import androidx.annotation.RestrictTo
import java.util.*
import java.util.regex.Pattern

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SchemaRule private constructor(val rule: String, private val ruleParts: List<String>) {
    
    fun matchWithSchema(schema: String): Boolean {
        val uriParts = getParts(schema, URI_PATTERN)
        if (uriParts == null || uriParts.size < ruleParts.size) {
            return false
        }
        // Check vendor part
        val ruleVendor = ruleParts[0].split(".").toTypedArray()
        val uriVendor = uriParts[0].split(".").toTypedArray()
        if (uriVendor.size != ruleVendor.size) {
            return false
        }
        var index = 0
        for (ruleVendorPart in ruleVendor) {
            if ("*" != ruleVendorPart && uriVendor[index] != ruleVendorPart) {
                return false
            }
            index++
        }
        // Check the rest of the rule
        index = 1
        for (rulePart in ruleParts.subList(1, ruleParts.size)) {
            if ("*" != rulePart && uriParts[index] != rulePart) {
                return false
            }
            index++
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as? SchemaRule
        return rule == that?.rule
    }

    override fun hashCode(): Int {
        return rule.hashCode()
    }

    companion object {
        private const val RULE_PATTERN =
            "^iglu:((?:(?:[a-zA-Z0-9-_]+|\\*)\\.)+(?:[a-zA-Z0-9-_]+|\\*))\\/([a-zA-Z0-9-_\\.]+|\\*)\\/([a-zA-Z0-9-_\\.]+|\\*)\\/([1-9][0-9]*|\\*)-(0|[1-9][0-9]*|\\*)-(0|[1-9][0-9]*|\\*)$"
        private const val URI_PATTERN =
            "^iglu:((?:(?:[a-zA-Z0-9-_]+)\\.)+(?:[a-zA-Z0-9-_]+))\\/([a-zA-Z0-9-_]+)\\/([a-zA-Z0-9-_]+)\\/([1-9][0-9]*)\\-(0|[1-9][0-9]*)\\-(0|[1-9][0-9]*)$"

        @JvmStatic
        fun build(rule: String): SchemaRule? {
            if (rule.isEmpty()) {
                return null
            }
            
            val parts = getParts(rule, RULE_PATTERN)
            return if (parts == null || parts.isEmpty() || !validateVendor(
                    parts[0]
                )
            ) {
                null
            } else SchemaRule(rule, parts)
        }

        // Private methods
        private fun getParts(uri: String, regex: String): List<String>? {
            val result: MutableList<String> = ArrayList(6)
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(uri)
            if (!matcher.find()) {
                return null
            }
            
            for (i in 1 until matcher.groupCount()) {
                if (i > 6) return null
                
                val part = matcher.group(i)
                part?.let { result.add(it) }
            }
            return result
        }

        private fun validateVendor(vendor: String): Boolean {
            // the components array will be generated like this from vendor:
            // "com.acme.marketing" => ["com", "acme", "marketing"]
            val components = vendor.split(".").toTypedArray()
            // check that vendor doesn't begin or end with period
            // e.g. ".snowplowanalytics.snowplow." => ["", "snowplowanalytics", "snowplow", ""]
            if (components.size > 1 && (components[0].isEmpty() || components[components.size - 1].isEmpty())) {
                return false
            }
            // reject vendors with criteria that are too broad & don't make sense, i.e. "*.*.marketing"
            if ("*" == components[0] || "*" == components[1]) {
                return false
            }
            // now validate the remaining parts, vendors should follow matching that never breaks trailing specificity
            // in other words, once we use an asterisk, we must continue using asterisks for parts or stop
            // e.g. "com.acme.marketing.*.*" is allowed, but "com.acme.*.marketing.*" or "com.acme.*.marketing" is forbidden
            if (components.size <= 2) return true
            // trailingComponents are the remaining parts after the first two
            val trailingComponents = components.copyOfRange(2, components.size)
            var asterisk = false
            for (part in trailingComponents) {
                if ("*" == part) { // mark when we've found a wildcard
                    asterisk = true
                } else if (asterisk) { // invalid when alpha parts come after wildcard
                    return false
                }
            }
            return true
        }
    }
}
