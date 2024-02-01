/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.events.CheckoutStepEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class CheckoutStepTest {
    @Test
    fun testExpectedForm() {
        val event = CheckoutStepEvent(step = 5,
            "postcode",
            shippingFullAddress = "full address",
            deliveryMethod = "stork",
            marketingOptIn = true
        )
        val map = hashMapOf<String, Any>(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_CHECKOUT_STEP,
            "data" to hashMapOf<String, Any>(
                Parameters.ECOMM_CHECKOUT_STEP to 5,
                Parameters.ECOMM_CHECKOUT_SHIPPING_POSTCODE to "postcode",
                Parameters.ECOMM_CHECKOUT_SHIPPING_ADDRESS to "full address",
                Parameters.ECOMM_CHECKOUT_DELIVERY_METHOD to "stork",
                Parameters.ECOMM_CHECKOUT_MARKETING_OPT_IN to true,
            )
        )
        
        val data: Map<String, Any?> = event.dataPayload
        
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.checkout_step.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_CHECKOUT_STEP))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))

        val entities = event.entitiesForProcessing
        Assert.assertNotNull(entities)
        Assert.assertEquals(1, entities!!.size)
        Assert.assertEquals(map, entities[0].map)
    }
}
