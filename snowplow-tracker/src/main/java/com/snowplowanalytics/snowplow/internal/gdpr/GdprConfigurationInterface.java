package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.Nullable;

public interface GdprConfigurationInterface {
    @Nullable
    Gdpr.Basis getBasisForProcessing();

    @Nullable
    String getDocumentId();

    @Nullable
    String getDocumentVersion();

    @Nullable
    String getDocumentDescription();
}
