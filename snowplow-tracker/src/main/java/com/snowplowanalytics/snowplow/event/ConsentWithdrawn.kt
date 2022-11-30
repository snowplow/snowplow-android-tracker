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

/** A consent withdrawn event.  
 * @param all Whether to withdraw consent for all consent documents.
 * @param documentId Identifier of the first document.
 * @param documentVersion Version of the first document.
 * */
class ConsentWithdrawn(all: Boolean, documentId: String, documentVersion: String) :
    AbstractSelfDescribing() {
    
    /** Whether to withdraw consent for all consent documents.  */
    @JvmField
    val all: Boolean

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
     * Creates a consent withdrawn event.
     */
    init {
        Preconditions.checkArgument(documentId.isNotEmpty(), "Document ID cannot be empty")
        Preconditions.checkArgument(documentVersion.isNotEmpty(), "Document version cannot be empty")
        this.all = all
        this.documentId = documentId
        this.documentVersion = documentVersion
    }
    
    // Builder methods
    
    /** Name of the first document.  */
    fun documentName(documentName: String?): ConsentWithdrawn {
        this.documentName = documentName
        return this
    }

    /** Description of the first document.  */
    fun documentDescription(documentDescription: String?): ConsentWithdrawn {
        this.documentDescription = documentDescription
        return this
    }

    /** Other attached documents.  */
    fun documents(documents: List<ConsentDocument>): ConsentWithdrawn {
        consentDocuments.clear()
        consentDocuments.addAll(documents)
        return this
    }

    // Public methods
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
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.CW_ALL] = all
            return payload
        }
    override val schema: String
        get() = TrackerConstants.SCHEMA_CONSENT_WITHDRAWN

    override fun beginProcessing(tracker: Tracker) {
        for (document in documents) {
            val context = SelfDescribingJson(document.schema, document.dataPayload)
            customContexts.add(context) // TODO: Only the user should modify the public customContexts property
        }
    }
}
