package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.Gdpr;

public class GdprConfiguration implements Configuration {

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

    // Copyable

    @NonNull
    @Override
    public GdprConfiguration copy() {
        return new GdprConfiguration(basisForProcessing, documentId, documentVersion, documentDescription);
    }
}
