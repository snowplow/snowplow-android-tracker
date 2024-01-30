/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.ecommerce.entities

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Add ecommerce User details. It is designed to help in modeling guest/non-guest account activity.
 * Entity schema: iglu:com.snowplowanalytics.snowplow.ecommerce/user/jsonschema/1-0-0
 */
data class EcommerceUserEntity @JvmOverloads constructor(
    /**
     * The user ID.
     */
    var id: String,

    /**
     * Whether or not the user is a guest.
     */
    var isGuest: Boolean? = null,

    /**
     * The user's email address.
     */
    var email: String? = null,

) {
    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_USER,
            mapOf<String, Any?>(
                Parameters.ECOMM_USER_ID to id,
                Parameters.ECOMM_USER_GUEST to isGuest,
                Parameters.ECOMM_USER_EMAIL to email
            ).filter { it.value != null }
        )
}
