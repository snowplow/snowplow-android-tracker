package com.snowplowanalytics.snowplow.ecommerce

data class EcommerceProduct(
    /**
     * The SKU or product ID
     */
    val id: String,
    
    /**
     * The name or title of the product
     */
    val name: String?,
    
    /**
     * The category the product belongs to.
     * Use a consistent separator to express multiple levels. E.g. Woman/Shoes/Sneakers
     */
    val category: String?,
    
    /**
     * The price of the product at the current time.
     */
    val price: Number,
    
    /**
     * The recommended or list price of a product
     */
    val list_price: Number?,
    
    /**
     * The quantity of the product taking part in the action. Used for Cart events.
     */
    val quantity: Number?,
    
    /**
     * The size of the product
     */
    val size: String?,
    
    /**
     * The variant of the product
     */
    val variant: String?,
    
    /**
     * The brand of the product
     */
    val brand: String?,
    
    /**
     * The inventory status of the product (e.g. in stock, out of stock, preorder, backorder, etc)
     */
    val inventory_status: String?,
    
    /**
     * The position the product was presented in a list of products (search results, product list page, etc)
     */
    val position: Number?,
    
    /**
     * The currency in which the product is being priced (ISO 4217)
     */
    val currency: String,
    
    /**
     * Identifier/Name/Url for the creative presented on a list or product view.
     */
    val creative_id: String?
)
