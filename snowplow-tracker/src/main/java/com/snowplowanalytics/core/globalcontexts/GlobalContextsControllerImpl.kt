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
    private val tracker: Tracker
        get() = serviceProvider.getOrMakeTracker()

    override val tags: Set<String?>
        get() = tracker.globalContextTags

    override fun add(tag: String, contextGenerator: GlobalContext): Boolean {
        return tracker.addGlobalContext(contextGenerator, tag)
    }

    override fun remove(tag: String): GlobalContext? {
        return tracker.removeGlobalContext(tag)
    }
}
