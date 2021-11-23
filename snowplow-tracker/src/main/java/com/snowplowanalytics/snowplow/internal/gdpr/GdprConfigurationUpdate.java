package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.GdprConfiguration;
import com.snowplowanalytics.snowplow.util.Basis;

public class GdprConfigurationUpdate extends GdprConfiguration {

    @Nullable
    public GdprConfiguration sourceConfig;

    @Nullable
    public Gdpr gdpr;

    public boolean isEnabled;

    // gdpr flag

    public boolean gdprUpdated;

    public GdprConfigurationUpdate() {
        super(Basis.CONTRACT, null, null, null);
    }

    @NonNull
    public Basis getBasisForProcessing() {
        return (sourceConfig == null || gdprUpdated) ? super.basisForProcessing : sourceConfig.basisForProcessing;
    }

    @NonNull
    public String getDocumentId() {
        return (sourceConfig == null || gdprUpdated) ? super.documentId : sourceConfig.documentId;
    }

    @NonNull
    public String getDocumentVersion() {
        return (sourceConfig == null || gdprUpdated) ? super.documentVersion : sourceConfig.documentVersion;
    }

    @NonNull
    public String getDocumentDescription() {
        return (sourceConfig == null || gdprUpdated) ? super.documentDescription : sourceConfig.documentDescription;
    }
}
