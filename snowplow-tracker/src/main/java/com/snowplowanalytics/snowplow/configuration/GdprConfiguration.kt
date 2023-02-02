/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
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
