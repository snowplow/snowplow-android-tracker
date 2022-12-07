package com.snowplowanalytics.core.gdpr

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.util.Basis

import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
class Gdpr(
    val basisForProcessing: Basis,
    val documentId: String?,
    val documentVersion: String?,
    val documentDescription: String?
) {
    val context: SelfDescribingJson
        get() {
            val map: MutableMap<String, Any?> = HashMap()
            map["basisForProcessing"] =
                basisForProcessing.toString().lowercase(Locale.getDefault())
            map["documentId"] = documentId
            map["documentVersion"] = documentVersion
            map["documentDescription"] = documentDescription
            return SelfDescribingJson(TrackerConstants.SCHEMA_GDPR, map)
        }
}
