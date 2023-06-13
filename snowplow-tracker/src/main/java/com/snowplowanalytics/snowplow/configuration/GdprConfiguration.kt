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

import com.snowplowanalytics.core.gdpr.Gdpr
import com.snowplowanalytics.core.gdpr.GdprConfigurationInterface
import com.snowplowanalytics.snowplow.util.Basis

/**
 * Allows the GDPR configuration of the tracker. Provide a [GdprConfiguration] when creating a tracker 
 * to attach a GDPR entity to every event.
 */
open class GdprConfiguration
/**
 * Enables GDPR entity to be sent with each event.
 *
 * @param basisForProcessing  GDPR Basis for processing.
 * @param documentId          ID of a GDPR basis document.
 * @param documentVersion     Version of the document.
 * @param documentDescription Description of the document.
 */(
    basisForProcessing: Basis? = null,
    documentId: String? = null,
    documentVersion: String? = null,
    documentDescription: String? = null
) : Configuration, GdprConfigurationInterface {

    /**
     * Fallback configuration to read from in case requested values are not present in this configuration.
     */
    internal var sourceConfig: GdprConfiguration? = null

    private var _isEnabled: Boolean? = null
    internal var isEnabled: Boolean
        get() = _isEnabled ?: sourceConfig?.isEnabled ?: true
        set(value) { _isEnabled = value }

    private var _gdpr: Gdpr? = null
    internal var gdpr: Gdpr?
        get() = _gdpr ?: sourceConfig?.gdpr
        set(value) { _gdpr = value }

    private var _basisForProcessing: Basis? = basisForProcessing
    /**
     * Basis for processing.
     */
    override var basisForProcessing: Basis
        get() = _basisForProcessing ?: sourceConfig?.basisForProcessing ?: Basis.CONTRACT
        set(value) { _basisForProcessing = value }

    private var _documentId: String? = documentId
    /**
     * ID of a GDPR basis document.
     */
    override var documentId: String?
        get() = _documentId ?: sourceConfig?.documentId
        set(value) { _documentId = value }

    private var _documentVersion: String? = documentVersion
    /**
     * Version of the document.
     */
    override var documentVersion: String?
        get() = _documentVersion ?: sourceConfig?.documentVersion
        set(value) { _documentVersion = value }

    private var _documentDescription: String? = documentDescription
    /**
     * Description of the document.
     */
    override var documentDescription: String?
        get() = _documentDescription ?: sourceConfig?.documentDescription
        set(value) { _documentDescription = value }

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
