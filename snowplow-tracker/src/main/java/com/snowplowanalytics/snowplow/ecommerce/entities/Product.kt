package com.snowplowanalytics.snowplow.ecommerce.entities

data class Product(
    /**
     * The SKU or product ID
     */
    val id: String,

    /**
     * The name or title of the product
     */
    val name: String? = null,

    /**
     * The category the product belongs to.
     * Use a consistent separator to express multiple levels. E.g. Woman/Shoes/Sneakers
     */
    val category: String? = null,

    /**
     * The price of the product at the current time.
     */
    val price: Number,

    /**
     * The recommended or list price of a product
     */
    val listPrice: Number? = null,

    /**
     * The quantity of the product taking part in the action. Used for Cart events.
     */
    val quantity: Int? = null,

    /**
     * The size of the product
     */
    val size: String? = null,

    /**
     * The variant of the product
     */
    val variant: String? = null,

    /**
     * The brand of the product
     */
    val brand: String? = null,

    /**
     * The inventory status of the product (e.g. in stock, out of stock, preorder, backorder, etc)
     */
    val inventoryStatus: String? = null,

    /**
     * The position the product was presented in a list of products (search results, product list page, etc)
     */
    val position: Int? = null,

    /**
     * The currency in which the product is being priced (ISO 4217)
     */
    val currency: String,

    /**
     * Identifier/Name/Url for the creative presented on a list or product view.
     */
    val creativeId: String? = null
)
