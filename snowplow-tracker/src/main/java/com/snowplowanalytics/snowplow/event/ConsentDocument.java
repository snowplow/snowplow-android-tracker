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

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import java.util.Map;

/** A consent document event. */
public class ConsentDocument extends AbstractSelfDescribing {

    /** Identifier of the document. */
    @NonNull
    public final String documentId;
    /** Version of the document. */
    @NonNull
    public final String documentVersion;
    /** Name of the document. */
    @Nullable
    public String documentName;
    /** Description of the document. */
    @Nullable
    public String documentDescription;

    /**
     Create a consent document event.
     @param documentId identifier of the document.
     @param documentVersion version of the document.
     */
    public ConsentDocument(@NonNull String documentId, @NonNull String documentVersion) {
        Preconditions.checkNotNull(documentId);
        Preconditions.checkArgument(!documentId.isEmpty(), "Document ID cannot be empty");

        Preconditions.checkNotNull(documentVersion);
        Preconditions.checkArgument(!documentVersion.isEmpty(), "Document version cannot be empty");

        this.documentId = documentId;
        this.documentVersion = documentVersion;
    }

    // Builder methods

    /** Name of the document. */
    @NonNull
    public ConsentDocument documentName(@Nullable String documentName) {
        this.documentName = documentName;
        return this;
    }

    /** Description of the document. */
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
