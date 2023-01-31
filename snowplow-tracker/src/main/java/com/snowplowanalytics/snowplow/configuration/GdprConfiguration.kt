package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.gdpr.GdprConfigurationInterface
import com.snowplowanalytics.snowplow.util.Basis

/**
 * This class allows the GDPR configuration of the tracker.
 */
open class GdprConfiguration
/**
 * Enables GDPR context to be sent with each event.
 *
 * @param basisForProcessing  GDPR Basis for processing.
 * @param documentId          ID of a GDPR basis document.
 * @param documentVersion     Version of the document.
 * @param documentDescription Description of the document.
 */(
    /**
     * Basis for processing.
     */
    override val basisForProcessing: Basis,
    
    /**
     * ID of a GDPR basis document.
     */
    override val documentId: String?,
    
    /**
     * Version of the document.
     */
    override val documentVersion: String?,
    
    /**
     * Description of the document.
     */
    override val documentDescription: String?
) : Configuration, GdprConfigurationInterface {

    // Copyable
    override fun copy(): GdprConfiguration {
        return GdprConfiguration(
            basisForProcessing,
            documentId,
            documentVersion,
            documentDescription
        )
    }
}
