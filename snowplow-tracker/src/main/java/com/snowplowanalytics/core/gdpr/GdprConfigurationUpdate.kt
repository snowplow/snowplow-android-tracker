package com.snowplowanalytics.core.gdpr

import com.snowplowanalytics.snowplow.configuration.GdprConfiguration
import com.snowplowanalytics.snowplow.util.Basis

class GdprConfigurationUpdate : GdprConfiguration(
    Basis.CONTRACT, 
    null, 
    null, 
    null) {
    @JvmField
    var sourceConfig: GdprConfiguration? = null
    var gdpr: Gdpr? = null
    var isEnabled = false

    // gdpr flag
    var gdprUpdated = false
    fun basisForProcessing(): Basis {
        return if (sourceConfig == null || gdprUpdated) super.basisForProcessing else sourceConfig!!.basisForProcessing
    }

    fun documentId(): String {
        return if (sourceConfig == null || gdprUpdated) super.documentId!! else sourceConfig!!.documentId!!
    }

    fun documentVersion(): String {
        return if (sourceConfig == null || gdprUpdated) super.documentVersion!! else sourceConfig!!.documentVersion!!
    }

    fun documentDescription(): String {
        return if (sourceConfig == null || gdprUpdated) super.documentDescription!! else sourceConfig!!.documentDescription!!
    }
}
