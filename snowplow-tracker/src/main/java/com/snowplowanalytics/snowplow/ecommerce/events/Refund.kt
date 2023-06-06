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

/**
 * Track a refund event. Use the same transaction ID as for the original Transaction event.
 * Provide a list of products to specify certain products to be refunded, otherwise the whole transaction 
 * will be marked as refunded.
 *
 * @param transactionId The ID of the relevant transaction.
 * @param currency The currency in which the product(s) are being priced (ISO 4217).
 * @param refundAmount The monetary amount refunded.
 * @param refundReason Reason for refunding the whole or part of the transaction.
 * @param products The products to be refunded.
 */
class Refund(
    /** The ID of the relevant transaction. */
    val transactionId: String,
    
    /** The currency in which the product is being priced (ISO 4217). */
    val currency: String,
    
    /** The monetary amount refunded. */
    val refundAmount: Number,
    
    /** Reason for refunding the whole or part of the transaction. */
    val refundReason: String? = null, 
    
    /** The products to be refunded. */
    val products: List<Product>? = null
) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload["type"] = EcommerceAction.refund
            payload["products"] = products
            payload[Parameters.ECOMM_REFUND_ID] = transactionId
            payload[Parameters.ECOMM_REFUND_CURRENCY] = currency
            payload[Parameters.ECOMM_REFUND_AMOUNT] = refundAmount
            payload[Parameters.ECOMM_REFUND_REASON] = refundReason
            return payload
        }
    
}
