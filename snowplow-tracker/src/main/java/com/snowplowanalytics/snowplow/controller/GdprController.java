package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.gdpr.Gdpr;
import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;

public interface GdprController extends GdprConfigurationInterface {

    void reset(@NonNull Gdpr.Basis basisForProcessing, @NonNull String documentId, @NonNull String documentVersion, @NonNull String documentDescription);

    boolean isEnabled();

    boolean enable();
    void disable();
}
