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
        documentId: String?,
        documentVersion: String?,
        documentDescription: String?
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
