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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.EcommerceProduct


class ProductListClick(val product: EcommerceProduct) : AbstractSelfDescribing() {

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_ECOMMERCE_ACTION
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload["type"] = EcommerceAction.list_click
            
            product.id.let { payload[Parameters.ECOMM_PRODUCT_ID] = it }
            product.name?.let { payload[Parameters.ECOMM_PRODUCT_NAME] = it }
            product.category?.let { payload[Parameters.ECOMM_PRODUCT_CATEGORY] = it }
            product.price.let { payload[Parameters.ECOMM_PRODUCT_PRICE] = it }
            product.listPrice?.let { payload[Parameters.ECOMM_PRODUCT_LIST_PRICE] = it }
            product.quantity?.let { payload[Parameters.ECOMM_PRODUCT_QUANTITY] = it }
            product.size?.let { payload[Parameters.ECOMM_PRODUCT_SIZE] = it }
            product.variant?.let { payload[Parameters.ECOMM_PRODUCT_VARIANT] = it }
            product.brand?.let { payload[Parameters.ECOMM_PRODUCT_BRAND] = it }
            product.inventoryStatus?.let { payload[Parameters.ECOMM_PRODUCT_INVENTORY_STATUS] = it }
            product.position?.let { payload[Parameters.ECOMM_PRODUCT_POSITION] = it }
            product.currency.let { payload[Parameters.ECOMM_PRODUCT_CURRENCY] = it }
            product.creativeId?.let { payload[Parameters.ECOMM_PRODUCT_CREATIVE_ID] = it }
            return payload
        }
    
}
