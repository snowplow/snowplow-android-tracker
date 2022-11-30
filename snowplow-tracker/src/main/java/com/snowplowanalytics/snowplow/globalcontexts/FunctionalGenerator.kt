package com.snowplowanalytics.snowplow.globalcontexts

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

abstract class FunctionalGenerator {
    abstract fun apply(event: InspectableEvent): List<SelfDescribingJson?>?
}
