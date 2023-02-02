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

import com.snowplowanalytics.snowplow.configuration.GdprConfiguration
import com.snowplowanalytics.snowplow.util.Basis

class GdprConfigurationUpdate : GdprConfiguration(
    Basis.CONTRACT, 
    null, 
    null, 
    null) {
    
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
