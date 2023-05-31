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
