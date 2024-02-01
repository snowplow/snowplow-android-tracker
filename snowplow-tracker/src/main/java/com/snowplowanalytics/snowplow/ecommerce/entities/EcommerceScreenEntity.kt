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
 * Add ecommerce Screen/Page details. It is designed to help with grouping insights by 
 * screen/page type, e.g. Product description, Product list, Home.
 */
data class EcommerceScreenEntity @JvmOverloads constructor(
    /**
     * The type of screen that was visited, e.g. homepage, product details, cart, checkout, etc.
     * Entity schema: iglu:com.snowplowanalytics.snowplow.ecommerce/page/jsonschema/1-0-0
     */
    var type: String,

    /**
     * The language that the screen is based in.
     */
    var language: String? = null,

    /**
     * The locale version of the app that is running.
     */
    var locale: String? = null,

) {
    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PAGE,
            mapOf<String, Any?>(
                Parameters.ECOMM_SCREEN_TYPE to type,
                Parameters.ECOMM_SCREEN_LANGUAGE to language,
                Parameters.ECOMM_SCREEN_LOCALE to locale
            ).filter { it.value != null }
        )
}
