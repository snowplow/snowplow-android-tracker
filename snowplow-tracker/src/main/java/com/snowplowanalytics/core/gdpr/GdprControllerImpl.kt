package com.snowplowanalytics.core.gdpr

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.controller.GdprController
import com.snowplowanalytics.snowplow.util.Basis

@RestrictTo(RestrictTo.Scope.LIBRARY)
class GdprControllerImpl(serviceProvider: ServiceProviderInterface) : Controller(serviceProvider),
    GdprController {
    private var gdpr: Gdpr? = null
    
    override val basisForProcessing: Basis?
        get() = if (gdpr == null) { null } else gdpr!!.basisForProcessing
            
    override val documentId: String?
        get() = if (gdpr == null) { null } else gdpr!!.documentId
    
    override val documentVersion: String?
        get() = if (gdpr == null) { null } else gdpr!!.documentVersion

    override val documentDescription: String?
        get() = if (gdpr == null) { null } else gdpr!!.documentDescription
    
    override fun reset(
        basisForProcessing: Basis,
        documentId: String?,
        documentVersion: String?,
        documentDescription: String?
    ) {
        tracker.enableGdprContext(
            basisForProcessing,
            documentId,
            documentVersion,
            documentDescription
        )
        gdpr = tracker.gdprContext
        dirtyConfig.gdpr = gdpr
    }

    override val isEnabled: Boolean
        get() = tracker.gdprContext != null

    override fun enable(): Boolean {
        if (gdpr == null) {
            return false
        }
        tracker.enableGdprContext(
            gdpr!!.basisForProcessing,
            gdpr!!.documentId,
            gdpr!!.documentVersion,
            gdpr!!.documentDescription
        )
        dirtyConfig.isEnabled = true
        return true
    }

    override fun disable() {
        dirtyConfig.isEnabled = false
        tracker.disableGdprContext()
    }

    // Private methods
    private val tracker: Tracker
        get() = serviceProvider.orMakeTracker()
    private val dirtyConfig: GdprConfigurationUpdate
        get() = serviceProvider.gdprConfigurationUpdate
}
