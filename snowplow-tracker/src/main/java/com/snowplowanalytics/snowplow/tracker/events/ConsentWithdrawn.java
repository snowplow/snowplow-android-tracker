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

package com.snowplowanalytics.snowplow.tracker.events;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ConsentWithdrawn extends AbstractSelfDescribing {
    private final boolean all;
    private final String documentId;
    private final String documentVersion;
    private final String documentName;
    private final String documentDescription;
    private final List<ConsentDocument> consentDocuments;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private boolean all;
        private String documentId;
        private String documentVersion;
        private String documentName;
        private String documentDescription;
        private List<ConsentDocument> consentDocuments = new LinkedList<>();

        /**
         * @param all Whether to withdraw consent for all consent documents
         * @return itself
         */
        @NonNull
        public T all(boolean all) {
            this.all = all;
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
         * @param documents Consent documents attached to consent withdrawn event
         * @return itself
         */
        @NonNull
        public T consentDocuments(@NonNull List<ConsentDocument> documents) {
            this.consentDocuments = documents;
            return self();
        }

        public ConsentWithdrawn build() {
            return new ConsentWithdrawn(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    protected ConsentWithdrawn(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.documentId);
        Preconditions.checkArgument(!builder.documentId.isEmpty(), "Document ID cannot be empty");

        Preconditions.checkNotNull(builder.documentVersion);
        Preconditions.checkArgument(!builder.documentVersion.isEmpty(), "Document version cannot be empty");

        this.all = builder.all;
        this.documentId = builder.documentId;
        this.documentName = builder.documentName;
        this.documentVersion = builder.documentVersion;
        this.documentDescription = builder.documentDescription;
        List<ConsentDocument> documents = new LinkedList<>();
        documents.add(ConsentDocument.builder()
                .documentDescription(this.documentDescription)
                .documentId(this.documentId)
                .documentName(this.documentName)
                .documentVersion(this.documentVersion)
                .build());
        documents.addAll(builder.consentDocuments);
        this.consentDocuments = documents;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @deprecated As of release 1.5.0, it will be removed in version 2.0.0.
     * replaced by {@link #getDataPayload()}.
     *
     * @return the payload to be sent.
     */
    @Deprecated
    @NonNull
    public TrackerPayload getData() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.CW_ALL, this.all);
        return payload;
    }

    /**
     * Returns a list of consent documents associated with the event.
     *
     * @return the consent documents
     */
    public @NonNull List<ConsentDocument> getConsentDocuments() {
        return this.consentDocuments;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return getData().getMap();
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_CONSENT_WITHDRAWN;
    }

    @Override
    public void beginProcessing(Tracker tracker) {
        for (ConsentDocument document : consentDocuments) {
            SelfDescribingJson context = new SelfDescribingJson(document.getSchema(), document.getDataPayload());
            customContexts.add(context);
        }
    }
}
