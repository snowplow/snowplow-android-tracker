package com.snowplowanalytics.snowplow.controller;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;
import com.snowplowanalytics.snowplow.util.Basis;

public interface GdprController extends GdprConfigurationInterface {

    /**
     * Reset GDPR context to be sent with each event.
     * @param basisForProcessing GDPR Basis for processing.
     * @param documentId ID of a GDPR basis document.
     * @param documentVersion Version of the document.
     * @param documentDescription Description of the document.
     */
    void reset(@NonNull Basis basisForProcessing, @NonNull String documentId, @NonNull String documentVersion, @NonNull String documentDescription);

    /**
     * Whether the recorded GDPR context is enabled and will be attached as context.
     */
    boolean isEnabled();

    /**
     * Enable the GDPR context recorded.
     */
    boolean enable();

    /**
     * Disable the GDPR context recorded.
     */
    void disable();
}
