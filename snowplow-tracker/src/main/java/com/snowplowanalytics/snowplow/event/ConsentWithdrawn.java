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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** A consent withdrawn event. */
public class ConsentWithdrawn extends AbstractSelfDescribing {

    /** Whether to withdraw consent for all consent documents. */
    public final boolean all;
    /** Identifier of the first document. */
    @NonNull
    public final String documentId;
    /** Version of the first document. */
    @NonNull
    public final String documentVersion;
    /** Name of the first document. */
    @Nullable
    public String documentName;
    /** Description of the first document. */
    @Nullable
    public String documentDescription;
    /** Other attached documents. */
    @NonNull
    public final List<ConsentDocument> consentDocuments = new LinkedList<>();

    /**
     * Creates a consent withdrawn event.
     * @param all Whether to withdraw consent for all consent documents.
     * @param documentId Identifier of the first document.
     * @param documentVersion Version of the first document.
     */
    public ConsentWithdrawn(boolean all, @NonNull String documentId, @NonNull String documentVersion) {
        Preconditions.checkNotNull(documentId);
        Preconditions.checkArgument(!documentId.isEmpty(), "Document ID cannot be empty");
        Preconditions.checkNotNull(documentVersion);
        Preconditions.checkArgument(!documentVersion.isEmpty(), "Document version cannot be empty");
        this.all = all;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
    }

    // Builder methods

    /** Name of the first document. */
    @NonNull
    public ConsentWithdrawn documentName(@Nullable String documentName) {
        this.documentName = documentName;
        return this;
    }

    /** Description of the first document. */
    @NonNull
    public ConsentWithdrawn documentDescription(@Nullable String documentDescription) {
        this.documentDescription = documentDescription;
        return this;
    }

    /** Other attached documents. */
    @NonNull
    public ConsentWithdrawn documents(@NonNull List<ConsentDocument> documents) {
        consentDocuments.clear();
        consentDocuments.addAll(documents);
        return this;
    }

    // Public methods

    public @NonNull List<ConsentDocument> getDocuments() {
        List<ConsentDocument> docs = new ArrayList<>();
        ConsentDocument doc = new ConsentDocument(documentId, documentVersion)
                .documentDescription(documentDescription)
                .documentName(documentName);
        docs.add(doc);
        docs.addAll(consentDocuments);
        return docs;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        payload.put(Parameters.CW_ALL, all);
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_CONSENT_WITHDRAWN;
    }

    @Override
    public void beginProcessing(@NonNull Tracker tracker) {
        for (ConsentDocument document : getDocuments()) {
            SelfDescribingJson context = new SelfDescribingJson(document.getSchema(), document.getDataPayload());
            customContexts.add(context); // TODO: Only the user should modify the public customContexts property
        }
    }
}
