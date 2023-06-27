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

    override fun setEcommerceScreen(type: String, language: String?, locale: String?) {
        val plugin = PluginConfiguration("ecommercePageTypePluginInternal")
        plugin.entities  {
            val map = mutableMapOf<String, Any>()
            map["type"] = type
            language?.let { map["language"] = it }
            locale?.let { map["locale"] = it }

            listOf(SelfDescribingJson(TrackerConstants.SCHEMA_ECOMMERCE_PAGE, map))
        }
        serviceProvider.addPlugin(plugin)
    }

    override fun setEcommerceUser(id: String, isGuest: Boolean?, email: String?) {
        val plugin = PluginConfiguration("ecommerceUserPluginInternal")
        plugin.entities {
            val map = mutableMapOf<String, Any>()
            map["id"] = id
            isGuest?.let { map["is_guest"] = it }
            email?.let { map["email"] = it }
            
            listOf(SelfDescribingJson(TrackerConstants.SCHEMA_ECOMMERCE_USER, map))
        }
        serviceProvider.addPlugin(plugin)
    }

    override fun removeEcommerceScreen() {
        serviceProvider.removePlugin("ecommercePageTypePluginInternal")
    }

    override fun removeEcommerceUser() {
        serviceProvider.removePlugin("ecommerceUserPluginInternal")
    }
}
