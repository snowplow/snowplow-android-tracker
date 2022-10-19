package com.snowplowanalytics.snowplow.internal.gdpr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.GdprController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.util.Basis;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GdprControllerImpl extends Controller implements GdprController {

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

    public GdprControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    @Override
    public void reset(@NonNull Basis basisForProcessing, @NonNull String documentId, @NonNull String documentVersion, @NonNull String documentDescription) {
        getTracker().enableGdprContext(basisForProcessing, documentId, documentVersion, documentDescription);
        gdpr = getTracker().getGdprContext();
        getDirtyConfig().gdpr = gdpr;
        getDirtyConfig().gdprUpdated = true;
    }

    @Override
    public boolean isEnabled() {
        return getTracker().getGdprContext() != null;
    }

    @Override
    public boolean enable() {
        if (gdpr == null) {
            return false;
        }
        getTracker().enableGdprContext(gdpr.basisForProcessing, gdpr.documentId, gdpr.documentVersion, gdpr.documentDescription);
        getDirtyConfig().isEnabled = true;
        return true;
    }

    @Override
    public void disable() {
        getDirtyConfig().isEnabled = false;
        getTracker().disableGdprContext();
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

    // Private methods

    @NonNull
    private Tracker getTracker() {
        return serviceProvider.getOrMakeTracker();
    }

    @NonNull
    private GdprConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getGdprConfigurationUpdate();
    }
}
