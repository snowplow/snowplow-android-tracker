package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.GdprController;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.util.Basis;

public class GdprControllerImpl implements GdprController {

    @NonNull
    private Tracker tracker;

    @Nullable
    private Gdpr gdpr;

    @Nullable
    private Basis basisForProcessing;
    @Nullable
    private String documentId;
    @Nullable
    private String documentVersion;
    @Nullable
    private String documentDescription;


    public GdprControllerImpl(@NonNull Tracker tracker) {
        this.tracker = tracker;
        gdpr = tracker.getGdprContext();
    }

    @Override
    public void reset(@NonNull Basis basisForProcessing, @NonNull String documentId, @NonNull String documentVersion, @NonNull String documentDescription) {
        tracker.enableGdprContext(basisForProcessing, documentId, documentVersion, documentDescription);
        gdpr = tracker.getGdprContext();
    }

    @Override
    public boolean isEnabled() {
        return tracker.getGdprContext() != null;
    }

    @Override
    public boolean enable() {
        if (gdpr == null) {
            return false;
        }
        tracker.enableGdprContext(gdpr.basisForProcessing, gdpr.documentId, gdpr.documentVersion, gdpr.documentDescription);
        return true;
    }

    @Override
    public void disable() {
        tracker.disableGdprContext();
    }

    @Nullable
    @Override
    public Basis getBasisForProcessing() {
        if (gdpr == null) {
            return null;
        }
        return gdpr.basisForProcessing;
    }

    @Nullable
    @Override
    public String getDocumentId() {
        if (gdpr == null) {
            return null;
        }
        return gdpr.documentId;
    }

    @Nullable
    @Override
    public String getDocumentVersion() {
        if (gdpr == null) {
            return null;
        }
        return gdpr.documentVersion;
    }

    @Nullable
    @Override
    public String getDocumentDescription() {
        if (gdpr == null) {
            return null;
        }
        return gdpr.documentDescription;
    }
}
