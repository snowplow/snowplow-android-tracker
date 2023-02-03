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
        get() = gdpr?.basisForProcessing
            
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
        val gdpr = gdpr ?: return false
        
        tracker.enableGdprContext(
            gdpr.basisForProcessing,
            gdpr.documentId,
            gdpr.documentVersion,
            gdpr.documentDescription
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
        get() = serviceProvider.getOrMakeTracker()
    private val dirtyConfig: GdprConfigurationUpdate
        get() = serviceProvider.gdprConfigurationUpdate
}
