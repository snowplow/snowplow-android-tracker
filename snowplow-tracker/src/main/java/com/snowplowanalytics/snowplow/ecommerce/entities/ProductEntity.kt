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

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Provided to certain Ecommerce events. The Product properties will be sent with the event as a 
 * Product entity.
 */
data class ProductEntity @JvmOverloads constructor(
    /**
     * The SKU or product ID.
     */
    var id: String,

    /**
     * The category the product belongs to.
     * Use a consistent separator to express multiple levels. E.g. Woman/Shoes/Sneakers.
     */
    var category: String,

    /**
     * The currency in which the product is being priced (ISO 4217).
     */
    var currency: String,

    /**
     * The price of the product at the current time.
     */
    var price: Number,

    /**
     * The recommended or list price of a product.
     */
    var listPrice: Number? = null,

    /**
     * The name or title of the product.
     */
    var name: String? = null,

    /**
     * The quantity of the product taking part in the action. Used for Cart events.
     */
    var quantity: Int? = null,

    /**
     * The size of the product.
     */
    var size: String? = null,

    /**
     * The variant of the product.
     */
    var variant: String? = null,

    /**
     * The brand of the product.
     */
    var brand: String? = null,

    /**
     * The inventory status of the product (e.g. in stock, out of stock, preorder, backorder, etc).
     */
    var inventoryStatus: String? = null,

    /**
     * The position the product was presented in a list of products (search results, product list page, etc).
     */
    var position: Int? = null,

    /**
     * Identifier, name, or url for the creative presented on the associated promotion.
     */
    var creativeId: String? = null
) {
    internal val entity: SelfDescribingJson
        get() = SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
            mapOf<String, Any?>(
                Parameters.ECOMM_PRODUCT_ID to id,
                Parameters.ECOMM_PRODUCT_NAME to name,
                Parameters.ECOMM_PRODUCT_CATEGORY to category,
                Parameters.ECOMM_PRODUCT_PRICE to price,
                Parameters.ECOMM_PRODUCT_LIST_PRICE to listPrice,
                Parameters.ECOMM_PRODUCT_QUANTITY to quantity,
                Parameters.ECOMM_PRODUCT_SIZE to size,
                Parameters.ECOMM_PRODUCT_VARIANT to variant,
                Parameters.ECOMM_PRODUCT_BRAND to brand,
                Parameters.ECOMM_PRODUCT_INVENTORY_STATUS to inventoryStatus,
                Parameters.ECOMM_PRODUCT_POSITION to position,
                Parameters.ECOMM_PRODUCT_CURRENCY to currency,
                Parameters.ECOMM_PRODUCT_CREATIVE_ID to creativeId
            ).filter { it.value != null }
        )
}
