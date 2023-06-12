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
 * Track a transaction event.
 *
 * @param transactionId The ID of the transaction.
 * @param revenue The total value of the transaction.
 * @param currency The currency used (ISO 4217).
 * @param paymentMethod The payment method used.
 * @param totalQuantity Total quantity of items in the transaction.
 * @param tax Total amount of tax on the transaction.
 * @param shipping Total cost of shipping on the transaction.
 * @param discountCode Discount code used.
 * @param discountAmount Discount amount taken off.
 * @param creditOrder Whether it is a credit order or not.
 * @param products The product(s) included in the transaction.
 */
class Transaction @JvmOverloads constructor(
    /**
    * The ID of the transaction.
    */
    var transactionId: String,
    
    /**
    * The total value of the transaction.
    */
    var revenue: Number,
    
    /**
    * The currency used for the transaction (ISO 4217).
    */
    var currency: String,
    
    /**
    * The payment method used for the transaction.
    */
    var paymentMethod: String,
    
    /**
    * Total quantity of items in the transaction.
    */
    var totalQuantity: Int,
    
    /**
    * Total amount of tax on the transaction.
    */
    var tax: Number? = null,
    
    /**
    * Total cost of shipping on the transaction.
    */
    var shipping: Number? = null,
    
    /**
    * Discount code used.
    */
    var discountCode: String? = null,
    
    /**
    * Discount amount taken off.
    */
    var discountAmount: Number? = null,
    
    /**
    * Whether the transaction is a credit order or not.
    */
    var creditOrder: Boolean? = null,
    
    /**
    * Products in the transaction.
    */
    var products: List<Product>? = null
) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload["type"] = EcommerceAction.transaction.toString()
            return payload
        }

    override val entitiesForProcessing: List<SelfDescribingJson>?
        get() {
            val entities = mutableListOf<SelfDescribingJson>()
            products?.let { 
                for (product in it) {
                    entities.add(product.entity)
                }
            }
            entities.add(entity)
            return entities
        }

    private val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION,
            mapOf<String, Any?>(
                Parameters.ECOMM_TRANSACTION_ID to transactionId,
                Parameters.ECOMM_TRANSACTION_REVENUE to revenue,
                Parameters.ECOMM_TRANSACTION_CURRENCY to currency,
                Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD to paymentMethod,
                Parameters.ECOMM_TRANSACTION_QUANTITY to totalQuantity,
                Parameters.ECOMM_TRANSACTION_TAX to tax,
                Parameters.ECOMM_TRANSACTION_SHIPPING to shipping,
                Parameters.ECOMM_TRANSACTION_DISCOUNT_CODE to discountCode,
                Parameters.ECOMM_TRANSACTION_DISCOUNT_AMOUNT to discountAmount,
                Parameters.ECOMM_TRANSACTION_CREDIT_ORDER to creditOrder
            ).filter { it.value != null }
        )
}
