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

/** A foreground transition event.  */
class Foreground : AbstractSelfDescribing() {
    /** Index indicating the current transition.  */
    @JvmField
    var foregroundIndex: Int? = null

    override val schema: String
        get() = Companion.schema
    
    // Builder methods
    
    /** Index indicating the current transition.  */
    fun foregroundIndex(foregroundIndex: Int?): Foreground {
        this.foregroundIndex = foregroundIndex
        return this
    }

    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            foregroundIndex?.let { payload[PARAM_INDEX] = it }
            return payload
        }

    companion object {
        const val schema = "iglu:com.snowplowanalytics.snowplow/application_foreground/jsonschema/1-0-0"
        const val PARAM_INDEX = "foregroundIndex"
    }
}
