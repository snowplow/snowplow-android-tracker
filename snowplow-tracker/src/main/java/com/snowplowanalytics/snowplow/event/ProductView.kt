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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.ecommerce.EcommerceProduct

/** A background transition event.  */
class ProductView(var id: String, var name: String? = null) : AbstractSelfDescribing() {
    

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION

    // Builder methods

//    /** Index indicating the current transition.  */
//    fun name(id: Int?): Background {
//        this.id = id
//        return this
//    }

    // Tracker methods

    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            id.let { payload["ecomm_id"] = it }
            name?.let { payload["ecomm_name"] = it }

            return payload
        }
    
}
