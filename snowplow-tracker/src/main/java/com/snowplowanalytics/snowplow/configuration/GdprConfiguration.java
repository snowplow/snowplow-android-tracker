package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.gdpr.Gdpr;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;

public class GdprConfiguration implements Configuration, GdprConfigurationInterface {

    @NonNull
    public final Gdpr.Basis basisForProcessing;
    @NonNull
    public final String documentId;
    @NonNull
    public final String documentVersion;
    @NonNull
    public final String documentDescription;

    // Constructors

    public GdprConfiguration(@NonNull Gdpr.Basis basisForProcessing,
                             @NonNull String documentId,
                             @NonNull String documentVersion,
                             @NonNull String documentDescription)
    {
        this.basisForProcessing = basisForProcessing;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
        this.documentDescription = documentDescription;
    }

    // Getters

    @Override
    @NonNull
    public Gdpr.Basis getBasisForProcessing() {
        return basisForProcessing;
    }

    @Override
    @NonNull
    public String getDocumentId() {
        return documentId;
    }

    @Override
    @NonNull
    public String getDocumentVersion() {
        return documentVersion;
    }

    @Override
    @NonNull
    public String getDocumentDescription() {
        return documentDescription;
    }

    // Copyable

    @NonNull
    @Override
    public GdprConfiguration copy() {
        return new GdprConfiguration(basisForProcessing, documentId, documentVersion, documentDescription);
    }
}
