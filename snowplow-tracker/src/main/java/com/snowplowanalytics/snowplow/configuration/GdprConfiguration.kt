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
    @JvmField
    val basisForProcessing: Basis,
    /**
     * ID of a GDPR basis document.
     */
    @JvmField
    val documentId: String?,
    /**
     * Version of the document.
     */
    @JvmField
    val documentVersion: String?,
    /**
     * Description of the document.
     */
    @JvmField
    val documentDescription: String?
) : Configuration, GdprConfigurationInterface {

    // Getters
    /**
     * Basis for processing.
     */
    override fun getBasisForProcessing(): Basis {
        return basisForProcessing
    }

    /**
     * ID of a GDPR basis document.
     */
    override fun getDocumentId(): String? {
        return documentId
    }

    /**
     * Version of the document.
     */
    override fun getDocumentVersion(): String? {
        return documentVersion
    }

    /**
     * Description of the document.
     */
    override fun getDocumentDescription(): String? {
        return documentDescription
    }

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
