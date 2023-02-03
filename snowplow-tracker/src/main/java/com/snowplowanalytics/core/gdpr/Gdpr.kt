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
package com.snowplowanalytics.core.gdpr

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.util.Basis

import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
class Gdpr(
    val basisForProcessing: Basis,
    val documentId: String?,
    val documentVersion: String?,
    val documentDescription: String?
) {
    val context: SelfDescribingJson
        get() {
            val map: MutableMap<String, Any?> = HashMap()
            map["basisForProcessing"] =
                basisForProcessing.toString().lowercase(Locale.getDefault())
            map["documentId"] = documentId
            map["documentVersion"] = documentVersion
            map["documentDescription"] = documentDescription
            return SelfDescribingJson(TrackerConstants.SCHEMA_GDPR, map)
        }
}
