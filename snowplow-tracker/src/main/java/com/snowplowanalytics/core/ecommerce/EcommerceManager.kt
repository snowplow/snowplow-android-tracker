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
import com.snowplowanalytics.snowplow.ecommerce.EcommerceCart
import com.snowplowanalytics.snowplow.ecommerce.EcommerceProduct
import com.snowplowanalytics.snowplow.event.EcommerceTransaction
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

object EcommerceManager {
    fun plugin() : PluginConfiguration {
        val ecommercePlugin = PluginConfiguration("ecommercePlugin")
        
        ecommercePlugin.entities(
            listOf(TrackerConstants.SCHEMA_ECOMMERCE_ACTION)
        ) {
            println("💥 in closure")
            val payload = it.payload
            val toAttach: MutableList<SelfDescribingJson> = ArrayList()
            
            println("❗️ ${payload["type"]}")
            
            when (payload["type"]) {
                EcommerceAction.product_view, EcommerceAction.list_click -> {
                    val product = payload["product"] as EcommerceProduct
                    toAttach.add(productToSdj(product))
                    payload.remove("product")
                }
                
                EcommerceAction.list_view -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as EcommerceProduct))
                    }
                    payload.remove("products")
                }
                
                EcommerceAction.add_to_cart, EcommerceAction.remove_from_cart -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as EcommerceProduct))
                    }
                    payload.remove("products")
                    
                    val cart = payload["cart"] as EcommerceCart
                    toAttach.add(SelfDescribingJson(
                        TrackerConstants.SCHEMA_ECOMMERCE_CART,
                        hashMapOf(
                            Parameters.ECOMM_CART_ID to cart.cartId,
                            Parameters.ECOMM_CART_VALUE to cart.totalValue,
                            Parameters.ECOMM_CART_CURRENCY to cart.currency,
                        ))
                    )
                    payload.remove("cart")
                }
                
                EcommerceAction.transaction -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as EcommerceProduct))
                    }
                    payload.remove("products")

//                    val transaction = payload["transaction"] as EcommerceTransaction
//                    toAttach.add(SelfDescribingJson(
//                        TrackerConstants.SCHEMA_ECOMMERCE_CART,
//                        hashMapOf(
//                             Parameters.ECOMM_TRANSACTION_ID to transaction."transaction_id",
//                             Parameters.ECOMM_TRANSACTION_REVENUE to transaction."revenue",
//                             Parameters.ECOMM_TRANSACTION_CURRENCY to transaction."currency",
//                             Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD to transaction."payment_method",
//                             Parameters.ECOMM_TRANSACTION_QUANTITY to transaction."total_quantity",
//                             Parameters.ECOMM_TRANSACTION_TAX to transaction."tax",
//                             Parameters.ECOMM_TRANSACTION_SHIPPING to transaction."shipping",
//                             Parameters.ECOMM_TRANSACTION_DISCOUNT_CODE to transaction."discount_code",
//                             Parameters.ECOMM_TRANSACTION_DISCOUNT_AMOUNT to transaction."discount_amount",
//                             Parameters.ECOMM_TRANSACTION_CREDIT_ORDER to transaction."credit_order"
//                        ))
//                    )
//                    payload.remove("cart")
                }
            }

            payload["type"] = payload["type"].toString()
            return@entities toAttach
            
        }
        return ecommercePlugin
    }
    
    private fun productToSdj(product: EcommerceProduct) : SelfDescribingJson {
        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
            hashMapOf(
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
        )
    }
}
