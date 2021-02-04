/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConsentGranted extends AbstractSelfDescribing {

    @NonNull
    public final String expiry;
    @NonNull
    public final String documentId;
    @NonNull
    public final String documentVersion;
    @Nullable
    public String documentName;
    @Nullable
    public String documentDescription;
    @NonNull
    public final List<ConsentDocument> consentDocuments = new LinkedList<>();

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String expiry;
        private String documentId;
        private String documentVersion;
        private String documentName;
        private String documentDescription;
        private List<ConsentDocument> consentDocuments = new LinkedList<>();

        /**
         * @param expiry Whether to withdraw consent for all consent documents
         * @return itself
         */
        @NonNull
        public T expiry(@NonNull String expiry) {
            this.expiry = expiry;
            return self();
        }

        /**
         * @param id ID of the consent document
         * @return itself
         */
        @NonNull
        public T documentId(@NonNull String id) {
            this.documentId = id;
            return self();
        }

        /**
         * @param version Version of the consent document
         * @return itself
         */
        @NonNull
        public T documentVersion(@NonNull String version) {
            this.documentVersion = version;
            return self();
        }

        /**
         * @param name Name of the consent document
         * @return itself
         */
        @NonNull
        public T documentName(@NonNull String name) {
            this.documentName = name;
            return self();
        }

        /**
         * @param description Description of the consent document
         * @return itself
         */
        @NonNull
        public T documentDescription(@NonNull String description) {
            this.documentDescription = description;
            return self();
        }

        /**
         * @param documents Consent documents attached to consent granted event
         * @return itself
         */
        @NonNull
        public T consentDocuments(@NonNull List<ConsentDocument> documents) {
            this.consentDocuments.addAll(documents);
            return self();
        }

        @NonNull
        public ConsentGranted build() {
            return new ConsentGranted(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @NonNull
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    @NonNull
    public static Builder<?> builder() {
        return new Builder2();
    }

    protected ConsentGranted(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.expiry);
        Preconditions.checkArgument(!builder.expiry.isEmpty(), "Expiry cannot be empty");

        Preconditions.checkNotNull(builder.documentId);
        Preconditions.checkArgument(!builder.documentId.isEmpty(), "Document ID cannot be empty");

        Preconditions.checkNotNull(builder.documentVersion);
        Preconditions.checkArgument(!builder.documentVersion.isEmpty(), "Document version cannot be empty");

        this.expiry = builder.expiry;
        this.documentId = builder.documentId;
        this.documentVersion = builder.documentVersion;
        this.consentDocuments.addAll(builder.consentDocuments);
    }

    public ConsentGranted(@NonNull String expiry, @NonNull String documentId, @NonNull String documentVersion) {
        Preconditions.checkNotNull(expiry);
        Preconditions.checkArgument(!expiry.isEmpty(), "Expiry cannot be empty");
        Preconditions.checkNotNull(documentId);
        Preconditions.checkArgument(!documentId.isEmpty(), "Document ID cannot be empty");
        Preconditions.checkNotNull(documentVersion);
        Preconditions.checkArgument(!documentVersion.isEmpty(), "Document version cannot be empty");
        this.expiry = expiry;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
    }

    // Builder methods

    @NonNull
    public ConsentGranted documentName(@Nullable String documentName) {
        this.documentName = documentName;
        return this;
    }

    @NonNull
    public ConsentGranted documentDescription(@Nullable String documentDescription) {
        this.documentDescription = documentDescription;
        return this;
    }

    @NonNull
    public ConsentGranted documents(@NonNull List<ConsentDocument> documents) {
        consentDocuments.clear();
        consentDocuments.addAll(documents);
        return this;
    }

    // Public methods

    /**
     * Returns a list of consent documents associated with the event.
     *
     * @return the consent documents
     */
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
        if (expiry != null) payload.put(Parameters.CG_EXPIRY, expiry);
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_CONSENT_GRANTED;
    }

    @Override
    public void beginProcessing(@NonNull Tracker tracker) {
        for (ConsentDocument document : getDocuments()) {
            SelfDescribingJson context = new SelfDescribingJson(document.getSchema(), document.getDataPayload());
            customContexts.add(context);  // TODO: Only the user should modify the public customContexts property
        }
    }
}