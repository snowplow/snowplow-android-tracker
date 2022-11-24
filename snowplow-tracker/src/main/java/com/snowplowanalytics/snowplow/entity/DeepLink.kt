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

class DeepLink(url: String) : SelfDescribingJson(SCHEMA_DEEPLINK) {
    private val parameters = HashMap<String, Any>()

    init {
        parameters[PARAM_DEEPLINK_URL] = url
        setData(parameters)
        // Set here further checks about the arguments.
    }

    // Builder methods
    fun referrer(referrer: String?): DeepLink {
        referrer?.let { parameters[PARAM_DEEPLINK_REFERRER] = it }
        setData(parameters)
        return this
    }

    val url: String?
        get() = parameters[PARAM_DEEPLINK_URL] as String?
    val referrer: String?
        get() = parameters[PARAM_DEEPLINK_REFERRER] as String?

    companion object {
        const val SCHEMA_DEEPLINK = "iglu:com.snowplowanalytics.mobile/deep_link/jsonschema/1-0-0"
        const val PARAM_DEEPLINK_REFERRER = "referrer"
        const val PARAM_DEEPLINK_URL = "url"
    }
}
