package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.util.Basis;

public interface GdprConfigurationInterface {
    @Nullable
    Basis getBasisForProcessing();

    @Nullable
    String getDocumentId();

    @Nullable
    String getDocumentVersion();

    @Nullable
    String getDocumentDescription();
}
