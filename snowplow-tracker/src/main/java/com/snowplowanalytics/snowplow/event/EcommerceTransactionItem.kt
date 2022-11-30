/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.core.utils.Preconditions

/** An ecommerce item event.  
 * @param sku Stock Keeping Unit of the item.
 * @param price Price of the item.
 * @param quantity Quantity of the item.
 */
class EcommerceTransactionItem(sku: String, price: Double, quantity: Int) : AbstractPrimitive() {
    /** Stock Keeping Unit of the item.  */
    @JvmField
    val sku: String

    /** Price of the item.  */
    @JvmField
    val price: Double

    /** Quantity of the item.  */
    @JvmField
    val quantity: Int

    /** Name of the item.  */
    @JvmField
    var itemName: String? = null

    /** Category of the item.  */
    @JvmField
    var category: String? = null

    /** Currency used for the price of the item.  */
    @JvmField
    var currency: String? = null

    /** OrderID of the order that contains this item.  */
    @JvmField
    var orderId: String? = null

    /**
     * Creates an ecommerce item event.
     */
    init {
        Preconditions.checkArgument(sku.isNotEmpty(), "sku cannot be empty")
        this.sku = sku
        this.price = price
        this.quantity = quantity
    }
    
    // Builder methods
    
    /** Name of the item.  */
    fun name(name: String?): EcommerceTransactionItem {
        this.itemName = name
        return this
    }

    /** Category of the item.  */
    fun category(category: String?): EcommerceTransactionItem {
        this.category = category
        return this
    }

    /** Currency used for the price of the item.  */
    fun currency(currency: String?): EcommerceTransactionItem {
        this.currency = currency
        return this
    }

    /** OrderID of the order that contains this item.  */
    fun orderId(orderId: String?): EcommerceTransactionItem {
        this.orderId = orderId
        return this
    }

    // Public methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            orderId?.let { payload[Parameters.TI_ITEM_ID] = it }
            payload[Parameters.TI_ITEM_SKU] = sku
            payload[Parameters.TI_ITEM_PRICE] = price.toString()
            payload[Parameters.TI_ITEM_QUANTITY] = quantity.toString()
            itemName?.let { payload[Parameters.TI_ITEM_NAME] = it }
            category?.let { payload[Parameters.TI_ITEM_CATEGORY] = it }
            currency?.let { payload[Parameters.TI_ITEM_CURRENCY] = it }
            return payload
        }

    override val name: String
        get() = TrackerConstants.EVENT_ECOMM_ITEM
}
