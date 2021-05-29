package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.internal.gdpr.GdprConfigurationInterface;
import com.snowplowanalytics.snowplow.util.Basis;

/**
 * This class allows the GDPR configuration of the tracker.
 */
public class GdprConfiguration implements Configuration, GdprConfigurationInterface {

    /**
     * Basis for processing.
     */
    @NonNull
    public final Basis basisForProcessing;
    /**
     * ID of a GDPR basis document.
     */
    @NonNull
    public final String documentId;
    /**
     * Version of the document.
     */
    @NonNull
    public final String documentVersion;
    /**
     * Description of the document.
     */
    @NonNull
    public final String documentDescription;

    // Constructors

    /**
     * Enables GDPR context to be sent with each event.
     *
     * @param basisForProcessing  GDPR Basis for processing.
     * @param documentId          ID of a GDPR basis document.
     * @param documentVersion     Version of the document.
     * @param documentDescription Description of the document.
     */
    public GdprConfiguration(@NonNull Basis basisForProcessing,
                             @NonNull String documentId,
                             @NonNull String documentVersion,
                             @NonNull String documentDescription) {
        this.basisForProcessing = basisForProcessing;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
        this.documentDescription = documentDescription;
    }

    // Getters

    /**
     * Basis for processing.
     */
    @Override
    @NonNull
    public Basis getBasisForProcessing() {
        return basisForProcessing;
    }

    /**
     * ID of a GDPR basis document.
     */
    @Override
    @NonNull
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Version of the document.
     */
    @Override
    @NonNull
    public String getDocumentVersion() {
        return documentVersion;
    }

    /**
     * Description of the document.
     */
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
