package com.snowplowanalytics.snowplow.ecommerce

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
