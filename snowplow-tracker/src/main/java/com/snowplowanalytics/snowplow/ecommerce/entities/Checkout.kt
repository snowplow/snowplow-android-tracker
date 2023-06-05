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

/**
 * Used for a checkout_step event in Ecommerce.
 */
data class Checkout(
    /* Checkout step index */
    val step: Number,
    
    /* Shipping address postcode */
    val shippingPostcode: String? = null,
    
    /* Billing address postcode */
    val billingPostcode: String? = null,
    
    /* Full shipping address */
    val shippingFullAddress: String? = null,
    
    /* Full billing address */
    val billingFullAddress: String? = null,
    
    /* Can be used to discern delivery providers DHL, PostNL etc. */
    val deliveryProvider: String? = null,
    
    /* Store pickup, standard delivery, express delivery, international */
    val deliveryMethod: String? = null,
    
    /* Coupon applied at checkout */
    val couponCode: String? = null,
    
    /* Selection of 'existing user' or 'guest checkout' */
    val accountType: String? = null,
    
    /* Any kind of payment method the user selected to proceed. Card, PayPal, Alipay etc. */
    val paymentMethod: String? = null,
    
    /* Invoice or receipt */
    val proofOfPayment: String? = null,
    
    /* If opted in to marketing campaigns to the email address */
    val marketingOptIn: Boolean? = null
)
