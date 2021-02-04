package com.snowplowanalytics.snowplow.event;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import java.util.Map;

public class ConsentDocument extends AbstractSelfDescribing {
    @NonNull
    public final String documentId;
    @NonNull
    public final String documentVersion;
    @Nullable
    public String documentName;
    @Nullable
    public String documentDescription;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String documentId;
        private String documentVersion;
        private String documentName;
        private String documentDescription;

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

        @NonNull
        public ConsentDocument build() {
            return new ConsentDocument(this);
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

    protected ConsentDocument(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks

        Preconditions.checkNotNull(builder.documentId);
        Preconditions.checkArgument(!builder.documentId.isEmpty(), "Document ID cannot be empty");

        Preconditions.checkNotNull(builder.documentVersion);
        Preconditions.checkArgument(!builder.documentVersion.isEmpty(), "Document version cannot be empty");

        this.documentId = builder.documentId;
        this.documentName = builder.documentName;
        this.documentVersion = builder.documentVersion;
        this.documentDescription = builder.documentDescription;
    }

    public ConsentDocument(@NonNull String documentId, @NonNull String documentVersion) {
        Preconditions.checkNotNull(documentId);
        Preconditions.checkArgument(!documentId.isEmpty(), "Document ID cannot be empty");

        Preconditions.checkNotNull(documentVersion);
        Preconditions.checkArgument(!documentVersion.isEmpty(), "Document version cannot be empty");

        this.documentId = documentId;
        this.documentVersion = documentVersion;
    }

    // Builder methods

    @NonNull
    public ConsentDocument documentName(@Nullable String documentName) {
        this.documentName = documentName;
        return this;
    }

    @NonNull
    public ConsentDocument documentDescription(@Nullable String documentDescription) {
        this.documentDescription = documentDescription;
        return this;
    }

    // Schema and Payload

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.CD_ID, this.documentId);
        payload.add(Parameters.CD_NAME, this.documentName);
        payload.add(Parameters.CD_DESCRIPTION, this.documentDescription);
        payload.add(Parameters.CD_VERSION, this.documentVersion);
        return payload.getMap();
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_CONSENT_DOCUMENT;
    }
}
