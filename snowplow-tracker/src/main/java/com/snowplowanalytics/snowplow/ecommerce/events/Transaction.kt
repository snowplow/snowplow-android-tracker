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
 * Track a transaction event.
 *
 * @param transactionId The ID of the transaction.
 * @param revenue The total value of the transaction.
 * @param currency The currency used for the transaction.
 * @param paymentMethod The payment method used for the transaction.
 * @param totalQuantity Total quantity of items in the transaction.
 * @param tax Total amount of tax on the transaction.
 * @param shipping Total cost of shipping on the transaction.
 * @param discountCode Discount code used.
 * @param discountAmount Discount amount taken off.
 * @param creditOrder Whether the transaction is a credit order or not.
 * @param products The products included in the transaction.
 */
class Transaction(
    /**
    * The ID of the transaction
    */
    var transactionId: String,
    
    /**
    * The total value of the transaction
    */
    var revenue: Number,
    
    /**
    * The currency used for the transaction
    */
    var currency: String,
    
    /**
    * The payment method used for the transaction
    */
    var paymentMethod: String,
    
    /**
    * Total quantity of items in the transaction
    */
    var totalQuantity: Int? = null,
    
    /**
    * Total amount of tax on the transaction
    */
    var tax: Number? = null,
    
    /**
    * Total cost of shipping on the transaction
    */
    var shipping: Number? = null,
    
    /**
    * Discount code used
    */
    var discountCode: String? = null,
    
    /**
    * Discount amount taken off
    */
    var discountAmount: Number? = null,
    
    /**
    * Whether the transaction is a credit order or not
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
            payload["type"] = EcommerceAction.transaction
            payload["products"] = products
            payload[Parameters.ECOMM_TRANSACTION_ID] = transactionId
            payload[Parameters.ECOMM_TRANSACTION_REVENUE] = revenue
            payload[Parameters.ECOMM_TRANSACTION_CURRENCY] = currency
            payload[Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD] = paymentMethod
            payload[Parameters.ECOMM_TRANSACTION_QUANTITY] = totalQuantity
            payload[Parameters.ECOMM_TRANSACTION_TAX] = tax
            payload[Parameters.ECOMM_TRANSACTION_SHIPPING] = shipping
            payload[Parameters.ECOMM_TRANSACTION_DISCOUNT_CODE] = discountCode
            payload[Parameters.ECOMM_TRANSACTION_DISCOUNT_AMOUNT] = discountAmount
            payload[Parameters.ECOMM_TRANSACTION_CREDIT_ORDER] = creditOrder
            return payload
        }
    
}
