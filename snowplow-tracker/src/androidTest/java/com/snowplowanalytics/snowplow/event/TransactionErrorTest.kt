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
import com.snowplowanalytics.snowplow.ecommerce.ErrorType
import com.snowplowanalytics.snowplow.ecommerce.entities.ProductEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.TransactionEntity
import com.snowplowanalytics.snowplow.ecommerce.events.TransactionErrorEvent
import com.snowplowanalytics.snowplow.ecommerce.events.TransactionEvent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class TransactionErrorTest {
    @Test
    fun testExpectedForm() {
        val transaction = TransactionEntity(
        "transactionId",
        9876543.21,
        "GBP",
        "cash",
        1
        )

        val event = TransactionErrorEvent(
            transaction,
            errorCode = "E-001",
            errorShortcode = "shortcode",
            errorDescription = "description",
            errorType = ErrorType.Soft,
            resolution = "resolution"
        )

        val map = hashMapOf<String, Any>(
            "schema" to TrackerConstants.SCHEMA_ECOMMERCE_TRANSACTION_ERROR,
            "data" to hashMapOf(
                Parameters.ECOMM_TRANSACTION_ERROR_CODE to "E-001",
                Parameters.ECOMM_TRANSACTION_ERROR_SHORTCODE to "shortcode",
                Parameters.ECOMM_TRANSACTION_ERROR_DESCRIPTION to "description",
                Parameters.ECOMM_TRANSACTION_ERROR_TYPE to "soft",
                Parameters.ECOMM_TRANSACTION_ERROR_RESOLUTION to "resolution"
            )
        )
        
        val data: Map<String, Any?> = event.dataPayload
        Assert.assertNotNull(data)
        Assert.assertEquals(EcommerceAction.trns_error.toString(), data[Parameters.ECOMM_TYPE])

        val entities = event.entitiesForProcessing
        Assert.assertNotNull(entities)
        Assert.assertEquals(2, entities!!.size)
        Assert.assertEquals(map, entities[0].map)
    }
}
