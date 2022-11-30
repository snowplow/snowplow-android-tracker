package com.snowplowanalytics.snowplow.globalcontexts

import com.snowplowanalytics.snowplow.tracker.InspectableEvent

abstract class FunctionalFilter {
    abstract fun apply(event: InspectableEvent): Boolean
}
