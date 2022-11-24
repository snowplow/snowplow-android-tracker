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
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.core.utils.Preconditions

/** An ecommerce event. 
 * @param orderId Identifier of the order.
 * @param totalValue Total amount of the order.
 * @param items Items purchased.
 */
class EcommerceTransaction(
    orderId: String,
    totalValue: Double,
    items: List<EcommerceTransactionItem>
) : AbstractPrimitive() {
    
    /** Identifier of the order.  */
    @JvmField
    val orderId: String

    /** Total amount of the order.  */
    @JvmField
    val totalValue: Double
    
    /** The list of Transaction Items passed with the event.  */
    @JvmField
    val items: List<EcommerceTransactionItem>

    /** Identifies an affiliation.  */
    @JvmField
    var affiliation: String? = null

    /** Taxes applied to the purchase.  */
    @JvmField
    var taxValue: Double? = null

    /** Total amount for shipping.  */
    @JvmField
    var shipping: Double? = null

    /** City for shipping.  */
    @JvmField
    var city: String? = null

    /** State for shipping.  */
    @JvmField
    var state: String? = null

    /** Country for shipping.  */
    @JvmField
    var country: String? = null

    /** Currency used for totalValue and taxValue.  */
    @JvmField
    var currency: String? = null

    /**
     * Creates an ecommerce event.
     */
    init {
        Preconditions.checkArgument(orderId.isNotEmpty(), "orderId cannot be empty")
        this.orderId = orderId
        this.totalValue = totalValue
        this.items = ArrayList(items)
    }
    
    // Builder methods
    
    /** Identifies an affiliation.  */
    fun affiliation(affiliation: String?): EcommerceTransaction {
        this.affiliation = affiliation
        return this
    }

    /** Taxes applied to the purchase.  */
    fun taxValue(taxValue: Double?): EcommerceTransaction {
        this.taxValue = taxValue
        return this
    }

    /** Total amount for shipping.  */
    fun shipping(shipping: Double?): EcommerceTransaction {
        this.shipping = shipping
        return this
    }

    /** City for shipping.  */
    fun city(city: String?): EcommerceTransaction {
        this.city = city
        return this
    }

    /** State for shipping.  */
    fun state(state: String?): EcommerceTransaction {
        this.state = state
        return this
    }

    /** Country for shipping.  */
    fun country(country: String?): EcommerceTransaction {
        this.country = country
        return this
    }

    /** Currency used for totalValue and taxValue.  */
    fun currency(currency: String?): EcommerceTransaction {
        this.currency = currency
        return this
    }

    // Public methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>(9)
            payload[Parameters.TR_ID] = orderId
            payload[Parameters.TR_TOTAL] = totalValue.toString()
            affiliation?.let { payload[Parameters.TR_AFFILIATION] = it }
            taxValue?.let { payload[Parameters.TR_TAX] = it.toString() }
            shipping?.let { payload[Parameters.TR_SHIPPING] = it.toString() }
            city?.let { payload[Parameters.TR_CITY] = it }
            state?.let { payload[Parameters.TR_STATE] = it }
            country?.let { payload[Parameters.TR_COUNTRY] = it }
            currency?.let { payload[Parameters.TR_CURRENCY] = it }
            return payload
        }
    
    override val name: String
        get() = TrackerConstants.EVENT_ECOMM

    override fun endProcessing(tracker: Tracker) {
        for (item in items) {
            item.orderId = orderId
            tracker.track(item)
        }
    }
}
