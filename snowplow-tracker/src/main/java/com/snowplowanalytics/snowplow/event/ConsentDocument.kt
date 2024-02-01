/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants

/** A consent document event.  
 * @param documentId identifier of the document.
 * @param documentVersion version of the document.
 * */
class ConsentDocument(documentId: String, documentVersion: String) : AbstractSelfDescribing() {
    /** Identifier of the document.  */
    val documentId: String

    /** Version of the document.  */
    val documentVersion: String

    /** Name of the document.  */
    var documentName: String? = null

    /** Description of the document.  */
    var documentDescription: String? = null

    /**
     * Create a consent document event.
     */
    init {
        require(documentId.isNotEmpty()) { "Document ID cannot be empty" }
        require(documentVersion.isNotEmpty()) { "Document version cannot be empty" }

        this.documentId = documentId
        this.documentVersion = documentVersion
    }
    
    // Builder methods
    
    /** Name of the document.  */
    fun documentName(documentName: String?): ConsentDocument {
        this.documentName = documentName
        return this
    }

    /** Description of the document.  */
    fun documentDescription(documentDescription: String?): ConsentDocument {
        this.documentDescription = documentDescription
        return this
    }

    // Schema and Payload
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.CD_ID] = documentId
            documentName?.let { payload[Parameters.CD_NAME] = it }
            documentDescription?.let { payload[Parameters.CD_DESCRIPTION] = it }
            payload[Parameters.CD_VERSION] = documentVersion
            return payload
        }

    /** The event schema */
    override val schema: String
        get() = TrackerConstants.SCHEMA_CONSENT_DOCUMENT
}
