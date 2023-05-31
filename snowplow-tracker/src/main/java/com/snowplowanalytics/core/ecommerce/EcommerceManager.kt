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
import com.snowplowanalytics.snowplow.ecommerce.entities.Checkout
import com.snowplowanalytics.snowplow.ecommerce.entities.Product
import com.snowplowanalytics.snowplow.ecommerce.entities.Promotion
import com.snowplowanalytics.snowplow.ecommerce.entities.TransactionDetails
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

object EcommerceManager {
    fun plugin() : PluginConfiguration {
        val ecommercePlugin = PluginConfiguration("ecommerce_plugin_sp_internal")
        
        ecommercePlugin.entities(
            listOf(TrackerConstants.SCHEMA_ECOMMERCE_ACTION)
        ) {
            val payload = it.payload
            val toAttach: MutableList<SelfDescribingJson> = ArrayList()

            when (payload["type"]) {
                EcommerceAction.product_view, EcommerceAction.list_click -> {
                    val product = payload["product"] as Product
                    toAttach.add(productToSdj(product))
                    payload.remove("product")
                }
                
                EcommerceAction.list_view -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as Product))
                    }
                    payload.remove("products")
                }
                
                EcommerceAction.add_to_cart, EcommerceAction.remove_from_cart -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as Product))
                    }
                    payload.remove("products")
                    
                    toAttach.add(cartInfoToSdj(
                        payload[Parameters.ECOMM_CART_ID] as String?,
                        payload[Parameters.ECOMM_CART_VALUE] as Number,
                        payload[Parameters.ECOMM_CART_CURRENCY] as String
                    ))
                    payload.remove(Parameters.ECOMM_CART_ID)
                    payload.remove(Parameters.ECOMM_CART_VALUE)
                    payload.remove(Parameters.ECOMM_CART_CURRENCY)
                }
                
                EcommerceAction.transaction -> {
                    val products = payload["products"] as List<*>
                    for (product in products) {
                        toAttach.add(productToSdj(product as Product))
                    }
                    payload.remove("products")

                    val transaction = payload["transaction"] as TransactionDetails
                    toAttach.add(transactionToSdj(transaction))
                    payload.remove("transaction")
                }
                
                EcommerceAction.checkout_step -> {
                    val checkout = payload["checkout"] as Checkout
                    toAttach.add(checkoutToSdj(checkout))
                    payload.remove("checkout")
                }
                
                EcommerceAction.promo_view, EcommerceAction.promo_click -> {
                    val promotion = payload["promotion"] as Promotion
                    toAttach.add(promotionToSdj(promotion))
                    payload.remove("promo")
                }
            }
            payload["type"] = payload["type"].toString()
            return@entities toAttach
        }
        return ecommercePlugin
    }
    
    private fun productToSdj(product: Product) : SelfDescribingJson {
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

    private fun cartInfoToSdj(cartId: String?, totalValue: Number, currency: String) : SelfDescribingJson {
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

    private fun transactionToSdj(transaction: TransactionDetails) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_TRANSACTION_ID to transaction.transactionId,
            Parameters.ECOMM_TRANSACTION_REVENUE to transaction.revenue,
            Parameters.ECOMM_TRANSACTION_CURRENCY to transaction.currency,
            Parameters.ECOMM_TRANSACTION_PAYMENT_METHOD to transaction.paymentMethod,
            Parameters.ECOMM_TRANSACTION_QUANTITY to transaction.totalQuantity,
            Parameters.ECOMM_TRANSACTION_TAX to transaction.tax,
            Parameters.ECOMM_TRANSACTION_SHIPPING to transaction.shipping,
            Parameters.ECOMM_TRANSACTION_DISCOUNT_CODE to transaction.discountCode,
            Parameters.ECOMM_TRANSACTION_DISCOUNT_AMOUNT to transaction.discountAmount,
            Parameters.ECOMM_TRANSACTION_CREDIT_ORDER to transaction.creditOrder
        )
        map.values.removeAll(sequenceOf(null))
        
        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION,
            map
        )
    }

    private fun checkoutToSdj(checkout: Checkout) : SelfDescribingJson {
        val map = hashMapOf(
            Parameters.ECOMM_CHECKOUT_STEP to checkout.step,
            Parameters.ECOMM_CHECKOUT_SHIPPING_POSTCODE to checkout.shippingPostcode,
            Parameters.ECOMM_CHECKOUT_BILLING_POSTCODE to checkout.billingPostcode,
            Parameters.ECOMM_CHECKOUT_SHIPPING_ADDRESS to checkout.shippingFullAddress,
            Parameters.ECOMM_CHECKOUT_BILLING_ADDRESS to checkout.billingFullAddress,
            Parameters.ECOMM_CHECKOUT_DELIVERY_PROVIDER to checkout.deliveryProvider,
            Parameters.ECOMM_CHECKOUT_DELIVERY_METHOD to checkout.deliveryMethod,
            Parameters.ECOMM_CHECKOUT_COUPON_CODE to checkout.couponCode,
            Parameters.ECOMM_CHECKOUT_ACCOUNT_TYPE to checkout.accountType,
            Parameters.ECOMM_CHECKOUT_PAYMENT_METHOD to checkout.paymentMethod,
            Parameters.ECOMM_CHECKOUT_PROOF_OF_PAYMENT to checkout.proofOfPayment,
            Parameters.ECOMM_CHECKOUT_MARKETING_OPT_IN to checkout.marketingOptIn,
        )
        map.values.removeAll(sequenceOf(null))
        
        return SelfDescribingJson(
            TrackerConstants.SCHEMA_ECOMMERCE_CHECKOUT_STEP,
            map
        )
    }

    private fun promotionToSdj(promotion: Promotion) : SelfDescribingJson {
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
