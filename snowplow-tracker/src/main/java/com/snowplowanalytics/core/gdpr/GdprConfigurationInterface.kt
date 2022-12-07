package com.snowplowanalytics.core.gdpr

import com.snowplowanalytics.snowplow.util.Basis

interface GdprConfigurationInterface {
    /** Basis for processing.  */
    val basisForProcessing: Basis?

    /** ID of a GDPR basis document.  */
    val documentId: String?

    /** Version of the document.  */
    val documentVersion: String?

    /** Description of the document.  */
    val documentDescription: String?
}
