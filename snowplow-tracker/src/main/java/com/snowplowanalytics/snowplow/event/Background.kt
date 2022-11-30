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

/** A background transition event.  */
class Background : AbstractSelfDescribing() {
    
    /** Index indicating the current transition.  */
    @JvmField
    var backgroundIndex: Int? = null
    
    override val schema: String
        get() = Companion.schema

    // Builder methods
    
    /** Index indicating the current transition.  */
    fun backgroundIndex(backgroundIndex: Int?): Background {
        this.backgroundIndex = backgroundIndex
        return this
    }
    
    // Tracker methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            backgroundIndex?.let { payload[PARAM_INDEX] = it }

            return payload
        }

    companion object {
        const val schema = "iglu:com.snowplowanalytics.snowplow/application_background/jsonschema/1-0-0"
        const val PARAM_INDEX = "backgroundIndex"
    }
}
