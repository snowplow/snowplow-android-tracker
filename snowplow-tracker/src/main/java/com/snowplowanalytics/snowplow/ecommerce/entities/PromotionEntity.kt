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
package com.snowplowanalytics.snowplow.ecommerce.entities

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Provided to certain Ecommerce events. The Promotion properties will be sent with the event as a
 * Promotion entity.
 * Entity schema: iglu:com.snowplowanalytics.snowplow.ecommerce/promotion/jsonschema/1-0-0
 */
data class PromotionEntity @JvmOverloads constructor(
    /**
     * The ID of the promotion.
     */
    var id: String,
    
    /**
     * The name of the promotion.
     */
    var name: String? = null,
    
    /**
     * List of SKUs or product IDs showcased in the promotion.
     */
    var productIds: List<String>? = null,
    
    /**
     * The position the promotion was presented in a list of promotions such as a banner or slider, e.g. 2.
     */
    var position: Int? = null,
    
    /**
     * Identifier, name, or url for the creative presented on the promotion.
     */
    var creativeId: String? = null,
    
    /**
     * Type of the promotion delivery mechanism. E.g. popup, banner, intra-content.
     */
    var type: String? = null,
    
    /**
     * The UI slot in which the promotional content was added to.
     */
    var slot: String? = null
) {
    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PROMOTION,
            mapOf(
                Parameters.ECOMM_PROMO_ID to id,
                Parameters.ECOMM_PROMO_NAME to name,
                Parameters.ECOMM_PROMO_PRODUCT_IDS to productIds,
                Parameters.ECOMM_PROMO_POSITION to position,
                Parameters.ECOMM_PROMO_CREATIVE_ID to creativeId,
                Parameters.ECOMM_PROMO_TYPE to type,
                Parameters.ECOMM_PROMO_SLOT to slot
            ).filter { it.value != null }
        )
}
