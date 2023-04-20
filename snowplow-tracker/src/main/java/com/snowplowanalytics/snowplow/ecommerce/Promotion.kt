package com.snowplowanalytics.snowplow.ecommerce

data class Promotion(
    /**
     * The ID of the promotion.
     */
    val id: String,
    
    /**
     * The name of the promotion.
     */
    val name: String? = null,
    
    /**
     * List of SKUs or product IDs showcased in the promotion.
     */
    val productIds: List<String>? = null,
    
    /**
     * The position the promotion was presented in a list of promotions E.g. banner, slider.
     */
    val position: Number? = null,
    
    /**
     * Identifier/Name/Url for the creative presented on the promotion.
     */
    val creativeId: String? = null,
    
    /**
     * Type of the promotion delivery mechanism. E.g. popup, banner, intra-content
     */
    val type: String? = null,
    
    /**
     * The website slot in which the promotional content was added to.
     */
    val slot: String? = null
)
