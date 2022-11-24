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

/** A Structured event.  */
class Structured(category: String, action: String) : AbstractPrimitive() {
    @JvmField
    val category: String
    @JvmField
    val action: String
    @JvmField
    var label: String? = null
    @JvmField
    var property: String? = null
    @JvmField
    var value: Double? = null

    init {
        Preconditions.checkArgument(category.isNotEmpty(), "category cannot be empty")
        Preconditions.checkArgument(action.isNotEmpty(), "action cannot be empty")
        this.category = category
        this.action = action
    }

    // Builder methods
    
    fun label(label: String?): Structured {
        this.label = label
        return this
    }

    fun property(property: String?): Structured {
        this.property = property
        return this
    }

    fun value(value: Double?): Structured {
        this.value = value
        return this
    }

    // Tracker methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>(5)
            payload[Parameters.SE_CATEGORY] = category
            payload[Parameters.SE_ACTION] = action
            label?.let { payload[Parameters.SE_LABEL] = it }
            property?.let { payload[Parameters.SE_PROPERTY] = it }
            value?.let { payload[Parameters.SE_VALUE] = it.toString() }
            return payload
        }
    
    override val name: String
        get() = TrackerConstants.EVENT_STRUCTURED
}
