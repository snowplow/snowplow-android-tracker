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
