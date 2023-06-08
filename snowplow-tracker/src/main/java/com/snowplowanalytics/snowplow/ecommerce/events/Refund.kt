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
class Refund @JvmOverloads constructor(
    /** The ID of the relevant transaction. */
    var transactionId: String,
    
    /** The monetary amount refunded. */
    var refundAmount: Number,

    /** The currency in which the product is being priced (ISO 4217). */
    var currency: String,
    
    /** Reason for refunding the whole or part of the transaction. */
    var refundReason: String? = null, 
    
    /** The products to be refunded. */
    var products: List<Product>? = null
) : AbstractSelfDescribing(), EcommerceEvent {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload["type"] = EcommerceAction.refund.toString()
            return payload
        }

    override val entitiesForProcessing: List<SelfDescribingJson>?
        get() {
            val entities = mutableListOf<SelfDescribingJson>()
            products?.let {
                for (product in it) {
                    entities.add(productToSdj(product))
                }
            }
            entities.add(refundToSdj(transactionId, currency, refundAmount, refundReason))
            return entities
        }

    private fun refundToSdj(transactionId: String,
                                currency: String,
                                refundAmount: Number,
                                refundReason: String?
    ) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_REFUND_ID to transactionId,
            Parameters.ECOMM_REFUND_CURRENCY to currency,
            Parameters.ECOMM_REFUND_AMOUNT to refundAmount,
            Parameters.ECOMM_REFUND_REASON to refundReason
        )
        map.values.removeAll(sequenceOf(null))

        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_REFUND,
            map
        )
    }
}
