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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.utils.Preconditions

/** A timing event.  */
class Timing(category: String, variable: String, timing: Int) : AbstractSelfDescribing() {
    @JvmField
    val category: String
    @JvmField
    val variable: String
    @JvmField
    val timing: Int
    @JvmField
    var label: String? = null

    init {
        Preconditions.checkArgument(category.isNotEmpty(), "category cannot be empty")
        Preconditions.checkArgument(variable.isNotEmpty(), "variable cannot be empty")
        this.category = category
        this.variable = variable
        this.timing = timing
    }

    // Builder methods
    
    fun label(label: String?): Timing {
        this.label = label
        return this
    }

    // Tracker methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.UT_CATEGORY] = this.category
            payload[Parameters.UT_VARIABLE] = variable
            payload[Parameters.UT_TIMING] = timing
            if (label != null && label!!.isNotEmpty()) {
                payload[Parameters.UT_LABEL] = label
            }
            return payload
        }
    override val schema: String
        get() = TrackerConstants.SCHEMA_USER_TIMINGS
}
