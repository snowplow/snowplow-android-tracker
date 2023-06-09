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
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Track a product or products being added to cart.
 *
 * @param products - List of product(s) that were added to the cart.
 * @param totalValue - Total value of the cart after the product(s) were added.
 * @param currency - Currency used for the cart.
 * @param cartId - Cart identifier.
 */
class AddToCart @JvmOverloads constructor(

    /**
     * List of product(s) that were added to the cart.
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
    ) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.ECOMM_TYPE] = EcommerceAction.add_to_cart.toString()
            return payload
        }

    override val entitiesForProcessing: List<SelfDescribingJson>?
        get() {
            val entities = mutableListOf<SelfDescribingJson>()
            for (product in products) {
                entities.add(product.entity)
            }
            entities.add(entity)
            return entities
        }

    private val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_CART,
            mapOf<String, Any?>(
                Parameters.ECOMM_CART_ID to cartId,
                Parameters.ECOMM_CART_VALUE to totalValue,
                Parameters.ECOMM_CART_CURRENCY to currency,
            ).filter { it.value != null }
        )
}
