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

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.snowplow.configuration.PluginConfiguration
import com.snowplowanalytics.snowplow.ecommerce.EcommerceController
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson

@RestrictTo(RestrictTo.Scope.LIBRARY)
class EcommerceControllerImpl(val serviceProvider: ServiceProviderInterface) : EcommerceController {
    override fun setPageType(type: String, language: String?, locale: String?) {
        val plugin = PluginConfiguration("ecommercePageTypePlugin")
        plugin.entities()  {
            listOf(SelfDescribingJson(
                TrackerConstants.SCHEMA_ECOMMERCE_PAGE, 
                hashMapOf("type" to type, "language" to language, "locale" to locale))
            )
        }
        serviceProvider.addPlugin(plugin)
    }

    override fun setEcommerceUser(id: String, is_guest: Boolean?, email: String?) {
        val plugin = PluginConfiguration("ecommerceUserPlugin")
        plugin.entities()  {
            listOf(SelfDescribingJson(
                TrackerConstants.SCHEMA_ECOMMERCE_PAGE,
                hashMapOf("id" to id, "is_guest" to is_guest, "email" to email))
            )
        }
        serviceProvider.addPlugin(plugin)
    }
    
    fun registerEntities() {
        println("❗️ registerEntities")
    }
}
