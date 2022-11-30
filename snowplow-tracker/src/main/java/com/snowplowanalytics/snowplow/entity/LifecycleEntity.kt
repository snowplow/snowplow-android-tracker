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
package com.snowplowanalytics.snowplow.entity

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

class LifecycleEntity(isVisible: Boolean) : SelfDescribingJson(SCHEMA_LIFECYCLEENTITY) {
    private val parameters = HashMap<String, Any>()

    init {
        parameters[PARAM_LIFECYCLEENTITY_ISVISIBLE] = isVisible
        setData(parameters)
        // Set here further checks about the arguments.
    }

    // Builder methods
    fun index(index: Int?): LifecycleEntity {
        index?.let { parameters[PARAM_LIFECYCLEENTITY_INDEX] = it }
        setData(parameters)
        return this
    }

    companion object {
        const val SCHEMA_LIFECYCLEENTITY =
            "iglu:com.snowplowanalytics.mobile/application_lifecycle/jsonschema/1-0-0"
        const val PARAM_LIFECYCLEENTITY_INDEX = "index"
        const val PARAM_LIFECYCLEENTITY_ISVISIBLE = "isVisible"
    }
}
