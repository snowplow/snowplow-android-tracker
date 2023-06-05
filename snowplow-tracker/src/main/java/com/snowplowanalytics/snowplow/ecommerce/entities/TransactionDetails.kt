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

data class TransactionDetails(
    /**
     * The ID of the transaction
     */
    val transactionId: String,
    
    /**
     * The total value of the transaction
     */
    val revenue: Number,
    
    /**
     * The currency used for the transaction
     */
    val currency: String,
    
    /**
     * The payment method used for the transaction
     */
    val paymentMethod: String,
    
    /**
     * Total quantity of items in the transaction
     */
    val totalQuantity: Int? = null,
    
    /**
     * Total amount of tax on the transaction
     */
    val tax: Number? = null,
    
    /**
     * Total cost of shipping on the transaction
     */
    val shipping: Number? = null,
    
    /**
     * Discount code used
     */
    val discountCode: String? = null,
    
    /**
     * Discount amount taken off
     */
    val discountAmount: Number? = null,
    
    /**
     * Whether the transaction is a credit order or not
     */
    val creditOrder: Boolean? = null
)
