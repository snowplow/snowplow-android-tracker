/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.core.utils.Preconditions
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

/** A consent granted event.  
 * @param expiry Consent expiration.
 * @param documentId Identifier of the first document.
 * @param documentVersion Version of the first document.
 * */
class ConsentGranted(expiry: String, documentId: String, documentVersion: String) :
    AbstractSelfDescribing() {
    
    /** Expiration of the consent.  */
    @JvmField
    val expiry: String

    /** Identifier of the first document.  */
    @JvmField
    val documentId: String

    /** Version of the first document.  */
    @JvmField
    val documentVersion: String

    /** Name of the first document.  */
    @JvmField
    var documentName: String? = null

    /** Description of the first document.  */
    @JvmField
    var documentDescription: String? = null

    /** Other attached documents.  */
    @JvmField
    val consentDocuments: MutableList<ConsentDocument> = LinkedList()

    /**
     * Creates a consent granted event with a first document.
     */
    init {
        Preconditions.checkArgument(expiry.isNotEmpty(), "Expiry cannot be empty")
        Preconditions.checkArgument(documentId.isNotEmpty(), "Document ID cannot be empty")
        Preconditions.checkArgument(documentVersion.isNotEmpty(), "Document version cannot be empty")
        this.expiry = expiry
        this.documentId = documentId
        this.documentVersion = documentVersion
    }
    
    // Builder methods
    
    /** Name of the first document.  */
    fun documentName(documentName: String?): ConsentGranted {
        this.documentName = documentName
        return this
    }

    /** Description of the first document.  */
    fun documentDescription(documentDescription: String?): ConsentGranted {
        this.documentDescription = documentDescription
        return this
    }

    /** Other attached documents.  */
    fun documents(documents: List<ConsentDocument>): ConsentGranted {
        consentDocuments.clear()
        consentDocuments.addAll(documents)
        return this
    }
    
    // Public methods
    
    /** Returns a list of consent documents associated with the event.  */
    val documents: List<ConsentDocument>
        get() {
            val docs: MutableList<ConsentDocument> = ArrayList()
            val doc = ConsentDocument(documentId, documentVersion)
                .documentDescription(documentDescription)
                .documentName(documentName)
            docs.add(doc)
            docs.addAll(consentDocuments)
            return docs
        }

    // Tracker methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.CG_EXPIRY] = expiry
            return payload
        }
    
    override val schema: String
        get() = TrackerConstants.SCHEMA_CONSENT_GRANTED

    override fun beginProcessing(tracker: Tracker) {
        for (document in documents) {
            val context = SelfDescribingJson(document.schema, document.dataPayload)
            customContexts.add(context) // TODO: Only the user should modify the public customContexts property
        }
    }
}
