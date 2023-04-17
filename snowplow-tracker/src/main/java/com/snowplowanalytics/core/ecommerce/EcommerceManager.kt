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
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

object EcommerceManager {
    fun plugin() : PluginConfiguration {
        val ecommercePlugin = PluginConfiguration("ecommercePlugin")
        
        ecommercePlugin.entities(
            listOf(TrackerConstants.SCHEMA_ECOMMERCE_ACTION)
        ) {
            println("💥 in closure")
            val payload = it.payload
            
            println("❗️ ${payload["type"]}")
            
            if (payload["type"] == EcommerceAction.product_view || payload["type"] == EcommerceAction.list_click) {
                val product = SelfDescribingJson(
                    TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
                    hashMapOf(
                        Parameters.ECOMM_PRODUCT_ID to payload[Parameters.ECOMM_PRODUCT_ID],
                        Parameters.ECOMM_PRODUCT_NAME to payload[Parameters.ECOMM_PRODUCT_NAME],
                        Parameters.ECOMM_PRODUCT_CATEGORY to payload[Parameters.ECOMM_PRODUCT_CATEGORY],
                        Parameters.ECOMM_PRODUCT_PRICE to payload[Parameters.ECOMM_PRODUCT_PRICE],
                        Parameters.ECOMM_PRODUCT_LIST_PRICE to payload[Parameters.ECOMM_PRODUCT_LIST_PRICE],
                        Parameters.ECOMM_PRODUCT_QUANTITY to payload[Parameters.ECOMM_PRODUCT_QUANTITY],
                        Parameters.ECOMM_PRODUCT_SIZE to payload[Parameters.ECOMM_PRODUCT_SIZE],
                        Parameters.ECOMM_PRODUCT_VARIANT to payload[Parameters.ECOMM_PRODUCT_VARIANT],
                        Parameters.ECOMM_PRODUCT_BRAND to payload[Parameters.ECOMM_PRODUCT_BRAND],
                        Parameters.ECOMM_PRODUCT_INVENTORY_STATUS to payload[Parameters.ECOMM_PRODUCT_INVENTORY_STATUS],
                        Parameters.ECOMM_PRODUCT_POSITION to payload[Parameters.ECOMM_PRODUCT_POSITION],
                        Parameters.ECOMM_PRODUCT_CURRENCY to payload[Parameters.ECOMM_PRODUCT_CURRENCY],
                        Parameters.ECOMM_PRODUCT_CREATIVE_ID to payload[Parameters.ECOMM_PRODUCT_CREATIVE_ID]
                    )
                )

                payload["type"] = payload["type"].toString()

                payload.remove(Parameters.ECOMM_PRODUCT_ID)
                payload.remove(Parameters.ECOMM_PRODUCT_NAME)
                payload.remove(Parameters.ECOMM_PRODUCT_CATEGORY)
                payload.remove(Parameters.ECOMM_PRODUCT_PRICE)
                payload.remove(Parameters.ECOMM_PRODUCT_LIST_PRICE)
                payload.remove(Parameters.ECOMM_PRODUCT_QUANTITY)
                payload.remove(Parameters.ECOMM_PRODUCT_SIZE)
                payload.remove(Parameters.ECOMM_PRODUCT_VARIANT)
                payload.remove(Parameters.ECOMM_PRODUCT_BRAND)
                payload.remove(Parameters.ECOMM_PRODUCT_INVENTORY_STATUS)
                payload.remove(Parameters.ECOMM_PRODUCT_POSITION)
                payload.remove(Parameters.ECOMM_PRODUCT_CURRENCY)
                payload.remove(Parameters.ECOMM_PRODUCT_CREATIVE_ID)

                return@entities listOf(product)
            }
            return@entities listOf()
            
        }
        return ecommercePlugin
    }
}
