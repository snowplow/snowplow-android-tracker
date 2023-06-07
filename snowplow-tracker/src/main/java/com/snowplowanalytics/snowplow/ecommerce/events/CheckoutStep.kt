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
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing

/**
 * Track a checkout step.
 *
 * @param step Checkout step index.
 * @param shippingPostcode Shipping address postcode.
 * @param billingPostcode Billing address postcode.
 * @param shippingFullAddress Full shipping address.
 * @param billingFullAddress Full billing address.
 * @param deliveryProvider Can be used to discern delivery providers e.g. DHL, PostNL etc.
 * @param deliveryMethod Store pickup, standard delivery, express delivery, international...
 * @param couponCode Coupon applied at checkout.
 * @param accountType Type of account used on checkout, e.g. existing user, guest.
 * @param paymentMethod Any kind of payment method the user selected to proceed. Card, PayPal, Alipay etc.
 * @param proofOfPayment E.g. invoice or receipt
 * @param marketingOptIn If opted in to marketing campaigns to the email address. * 
 */
class CheckoutStep(
    /** Checkout step index */
    var step: Number,
    
    /** Shipping address postcode */
    var shippingPostcode: String? = null,
    
    /** Billing address postcode */
    var billingPostcode: String? = null,
    
    /** Full shipping address */
    var shippingFullAddress: String? = null,
    
    /** Full billing address */
    var billingFullAddress: String? = null,
    
    /** Can be used to discern delivery providers DHL, PostNL etc. */
    var deliveryProvider: String? = null,
    
    /** E.g. store pickup, standard delivery, express delivery, international */
    var deliveryMethod: String? = null,
    
    /** Coupon applied at checkout */
    var couponCode: String? = null,
    
    /** Type of account used on checkout, e.g. existing user, guest */
    var accountType: String? = null,
    
    /** Any kind of payment method the user selected to proceed. Card, PayPal, Alipay etc. */
    var paymentMethod: String? = null,
    
    /** E.g. invoice or receipt */
    var proofOfPayment: String? = null,
    
    /** If opted in to marketing campaigns to the email address */
    var marketingOptIn: Boolean? = null
) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload["type"] = EcommerceAction.checkout_step
            payload[Parameters.ECOMM_CHECKOUT_STEP] = step
            payload[Parameters.ECOMM_CHECKOUT_SHIPPING_POSTCODE] = shippingPostcode
            payload[Parameters.ECOMM_CHECKOUT_BILLING_POSTCODE] = billingPostcode
            payload[Parameters.ECOMM_CHECKOUT_SHIPPING_ADDRESS] = shippingFullAddress
            payload[Parameters.ECOMM_CHECKOUT_BILLING_ADDRESS] = billingFullAddress
            payload[Parameters.ECOMM_CHECKOUT_DELIVERY_PROVIDER] = deliveryProvider
            payload[Parameters.ECOMM_CHECKOUT_DELIVERY_METHOD] = deliveryMethod
            payload[Parameters.ECOMM_CHECKOUT_COUPON_CODE] = couponCode
            payload[Parameters.ECOMM_CHECKOUT_ACCOUNT_TYPE] = accountType
            payload[Parameters.ECOMM_CHECKOUT_PAYMENT_METHOD] = paymentMethod
            payload[Parameters.ECOMM_CHECKOUT_PROOF_OF_PAYMENT] = proofOfPayment
            payload[Parameters.ECOMM_CHECKOUT_MARKETING_OPT_IN] = marketingOptIn
            return payload
        }
    
}
