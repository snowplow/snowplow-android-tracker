package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.gdpr.GdprConfigurationInterface
import com.snowplowanalytics.snowplow.util.Basis

interface GdprController : GdprConfigurationInterface {
    /**
     * Reset GDPR context to be sent with each event.
     * @param basisForProcessing GDPR Basis for processing.
     * @param documentId ID of a GDPR basis document.
     * @param documentVersion Version of the document.
     * @param documentDescription Description of the document.
     */
    fun reset(
        basisForProcessing: Basis,
        documentId: String,
        documentVersion: String,
        documentDescription: String
    )

    /**
     * Whether the recorded GDPR context is enabled and will be attached as context.
     */
    val isEnabled: Boolean

    /**
     * Enable the GDPR context recorded.
     */
    fun enable(): Boolean

    /**
     * Disable the GDPR context recorded.
     */
    fun disable()
}
