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
package com.snowplowanalytics.snowplow.ecommerce.events

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.core.ecommerce.EcommerceEvent
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Track a product or products being removed from cart.
 *
 * @param products - List of product(s) that were removed from the cart.
 * @param totalValue - Total value of the cart after the product(s) were removed.
 * @param currency - Currency used for the cart.
 * @param cartId - Cart identifier.
 */
class RemoveFromCart @JvmOverloads constructor(
    /**
     * List of product(s) that were removed from the cart.
     */
    var products: List<Product>,

    /**
     * The total value of the cart after this interaction.
     */
    var totalValue: Number,

    /**
     * The currency used for this cart (ISO 4217).
     */
    var currency: String,

    /**
     * The unique ID representing this cart.
     */
    var cartId: String? = null
) : AbstractSelfDescribing(), EcommerceEvent {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.ECOMM_TYPE] = EcommerceAction.remove_from_cart.toString()
            return payload
        }

    override val entitiesForProcessing: List<SelfDescribingJson>?
        get() {
            val entities = mutableListOf<SelfDescribingJson>()
            for (product in products) {
                entities.add(productToSdj(product))
            }
            entities.add(cartToSdj(cartId, totalValue, currency))
            return entities
        }
}
