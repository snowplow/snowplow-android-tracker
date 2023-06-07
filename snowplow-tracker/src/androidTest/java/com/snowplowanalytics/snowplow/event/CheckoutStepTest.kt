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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.ecommerce.EcommerceAction
import com.snowplowanalytics.snowplow.ecommerce.events.CheckoutStep
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class CheckoutStepTest {
    @Test
    fun testExpectedForm() {
        val event = CheckoutStep(step = 5,
            deliveryMethod = "stork",
            marketingOptIn = true)
        val data: Map<String, Any?> = event.dataPayload
        
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.checkout_step.toString(), data[Parameters.ECOMM_TYPE])
        Assert.assertTrue(data.containsKey(Parameters.ECOMM_CHECKOUT_STEP))
        Assert.assertFalse(data.containsKey(Parameters.ECOMM_NAME))
        Assert.assertEquals(5, data[Parameters.ECOMM_CHECKOUT_STEP])
        Assert.assertEquals("stork", data[Parameters.ECOMM_CHECKOUT_DELIVERY_METHOD])
        Assert.assertEquals(true, data[Parameters.ECOMM_CHECKOUT_MARKETING_OPT_IN])
    }
}
