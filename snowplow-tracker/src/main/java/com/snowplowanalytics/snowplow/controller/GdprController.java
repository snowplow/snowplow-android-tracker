package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;
import com.snowplowanalytics.snowplow.util.Basis;

public interface GdprController extends GdprConfigurationInterface {

    void reset(@NonNull Basis basisForProcessing, @NonNull String documentId, @NonNull String documentVersion, @NonNull String documentDescription);

    boolean isEnabled();

    boolean enable();
    void disable();
}
