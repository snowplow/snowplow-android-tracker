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

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.Product


class RemoveFromCart(
    /**
     * The unique ID representing this cart
     */
    val cartId: String? = null,

    /**
     * The total value of the cart after this interaction
     */
    val totalValue: Number,

    /**
     * The currency used for this cart (ISO 4217)
     */
    val currency: String,
    val products: List<Product>
) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.ECOMM_TYPE] = EcommerceAction.remove_from_cart
            payload[Parameters.ECOMM_CART_ID] = cartId
            payload[Parameters.ECOMM_CART_VALUE] = totalValue
            payload[Parameters.ECOMM_CART_CURRENCY] = currency
            payload[Parameters.ECOMM_PRODUCTS] = products
            return payload
        }
    
}
