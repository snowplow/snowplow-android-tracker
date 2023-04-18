package com.snowplowanalytics.snowplow.ecommerce

data class EcommerceCart(
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
)
