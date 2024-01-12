package com.snowplowanalytics.snowplow.util

import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class EventSink : Configuration, PluginIdentifiable, PluginFilterCallable {

    var trackedEvents = mutableListOf<InspectableEvent>()

    override val identifier: String
        get() = "EventSink"

    override val filterConfiguration: PluginFilterConfiguration?
        get() = PluginFilterConfiguration { event ->
            trackedEvents.add(event)
            false
        }

    override fun copy(): Configuration {
        TODO("Not yet implemented")
    }

}
