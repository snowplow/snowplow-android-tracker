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
package com.snowplowanalytics.core.globalcontexts

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.controller.GlobalContextsController
import com.snowplowanalytics.snowplow.globalcontexts.GlobalContext

@RestrictTo(RestrictTo.Scope.LIBRARY)
class GlobalContextsControllerImpl(serviceProvider: ServiceProviderInterface) :
    Controller(serviceProvider), GlobalContextsController {

    override val tags: Set<String?>
        get() {
            return serviceProvider.pluginConfigurations.filter {
                it is GlobalContextPluginConfiguration
            }
                .map { it.identifier }
                .toSet()
        }

    override fun add(tag: String, contextGenerator: GlobalContext): Boolean {
        if (tags.contains(tag)) {
            return false
        }
        val plugin = GlobalContextPluginConfiguration(
            identifier = tag,
            globalContext = contextGenerator
        )
        serviceProvider.pluginsController.addPlugin(plugin)
        return true
    }

    override fun remove(tag: String): GlobalContext? {
        val configuration = serviceProvider.pluginConfigurations.firstOrNull {
            it.identifier == tag && it is GlobalContextPluginConfiguration
        } as GlobalContextPluginConfiguration?
        serviceProvider.pluginsController.removePlugin(tag)
        return configuration?.globalContext
    }
}
