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

import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

object EcommercePluginManager {
    
    fun ecommPlugin() : PluginConfiguration {
        val ecommercePlugin = PluginConfiguration("ecommercePlugin")
        
        ecommercePlugin.entities(
            listOf(TrackerConstants.SCHEMA_ECOMMERCE_ACTION)
        ) {
            println("💥 in closure")
            val payload = it.payload
            println("❗️ ${payload["ecomm_id"]}, ${payload["ecomm_name"]}")
            val product = SelfDescribingJson(
                TrackerConstants.SCHEMA_ECOMMERCE_PRODUCT,
                hashMapOf(
                    "id" to payload["ecomm_id"],
                    "name" to payload["ecomm_name"]
                )
            )

            payload.remove("ecomm_id")
            payload.remove("ecomm_name")

            listOf(product)
        }
        return ecommercePlugin
    }
}
