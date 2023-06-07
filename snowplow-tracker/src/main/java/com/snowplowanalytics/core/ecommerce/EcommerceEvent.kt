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
package com.snowplowanalytics.core.ecommerce

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.ecommerce.entities.Promotion
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

/**
 * Interface for creating Ecommerce entities.
 */
interface EcommerceEvent {
    fun productToSdj(product: Product) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_PRODUCT_ID to product.id,
            Parameters.ECOMM_PRODUCT_NAME to product.name,
            Parameters.ECOMM_PRODUCT_CATEGORY to product.category,
            Parameters.ECOMM_PRODUCT_PRICE to product.price,
            Parameters.ECOMM_PRODUCT_LIST_PRICE to product.listPrice,
            Parameters.ECOMM_PRODUCT_QUANTITY to product.quantity,
            Parameters.ECOMM_PRODUCT_SIZE to product.size,
            Parameters.ECOMM_PRODUCT_VARIANT to product.variant,
            Parameters.ECOMM_PRODUCT_BRAND to product.brand,
            Parameters.ECOMM_PRODUCT_INVENTORY_STATUS to product.inventoryStatus,
            Parameters.ECOMM_PRODUCT_POSITION to product.position,
            Parameters.ECOMM_PRODUCT_CURRENCY to product.currency,
            Parameters.ECOMM_PRODUCT_CREATIVE_ID to product.creativeId
        )
        map.values.removeAll(sequenceOf(null))

        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
            map
        )
    }

    fun cartToSdj(cartId: String?, totalValue: Number, currency: String) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_CART_ID to cartId,
            Parameters.ECOMM_CART_VALUE to totalValue,
            Parameters.ECOMM_CART_CURRENCY to currency,
        )
        map.values.removeAll(sequenceOf(null))

        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_CART,
            map
        )
    }

    fun promotionToSdj(promotion: Promotion) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_PROMO_ID to promotion.id,
            Parameters.ECOMM_PROMO_NAME to promotion.name,
            Parameters.ECOMM_PROMO_PRODUCT_IDS to promotion.productIds,
            Parameters.ECOMM_PROMO_POSITION to promotion.position,
            Parameters.ECOMM_PROMO_CREATIVE_ID to promotion.creativeId,
            Parameters.ECOMM_PROMO_TYPE to promotion.type,
            Parameters.ECOMM_PROMO_SLOT to promotion.slot
        )
        map.values.removeAll(sequenceOf(null))

        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PROMOTION,
            map
        )
    }
}
