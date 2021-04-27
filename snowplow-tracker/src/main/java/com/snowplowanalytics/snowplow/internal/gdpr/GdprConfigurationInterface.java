package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.util.Basis;

public interface GdprConfigurationInterface {

    /** Basis for processing. */
    @Nullable
    Basis getBasisForProcessing();

    /** ID of a GDPR basis document. */
    @Nullable
    String getDocumentId();

    /** Version of the document. */
    @Nullable
    String getDocumentVersion();

    /** Description of the document. */
    @Nullable
    String getDocumentDescription();
}
