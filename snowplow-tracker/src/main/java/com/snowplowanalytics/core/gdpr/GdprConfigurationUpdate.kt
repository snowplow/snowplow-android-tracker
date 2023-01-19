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
    var isEnabled = false
    private var gdprUpdated = false
    
    var gdpr: Gdpr? = null
        set(value) {
            field = value
            gdprUpdated = true
        }

    override val basisForProcessing: Basis
        get() = if (sourceConfig == null || gdprUpdated) super.basisForProcessing else sourceConfig!!.basisForProcessing
    
    override val documentId: String?
        get() = if (sourceConfig == null || gdprUpdated) super.documentId else sourceConfig!!.documentId
    
    override val documentVersion: String?
        get() = if (sourceConfig == null || gdprUpdated) super.documentVersion else sourceConfig!!.documentVersion
    
    override val documentDescription: String?
        get() = if (sourceConfig == null || gdprUpdated) super.documentDescription else sourceConfig!!.documentDescription
}
