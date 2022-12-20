package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.EmitterController;
import com.snowplowanalytics.snowplow.controller.GdprController;
import com.snowplowanalytics.snowplow.controller.GlobalContextsController;
import com.snowplowanalytics.snowplow.controller.NetworkController;
import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.controller.SubjectController;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;
import com.snowplowanalytics.snowplow.tracker.BuildConfig;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import java.util.UUID;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackerControllerImpl extends Controller implements TrackerController {

    private final static String TAG = TrackerControllerImpl.class.getSimpleName();

    // Constructors

    public TrackerControllerImpl(@NonNull ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    // Sub-controllers

    @Nullable
    @Override
    public NetworkController getNetwork() {
        return serviceProvider.getOrMakeNetworkController();
    }

    @Override
    @NonNull
    public EmitterController getEmitter() {
        return serviceProvider.getOrMakeEmitterController();
    }

    @Override
    @NonNull
    public SubjectController getSubject() {
        return serviceProvider.getOrMakeSubjectController();
    }

    @Override
    @NonNull
    public GdprController getGdpr() {
        return serviceProvider.getOrMakeGdprController();
    }

    @NonNull
    @Override
    public GlobalContextsController getGlobalContexts() {
        return serviceProvider.getOrMakeGlobalContextsController();
    }

    @NonNull
    public SessionControllerImpl getSessionController() {
        return serviceProvider.getOrMakeSessionController();
    }

    @Nullable
    public SessionController getSession() {
        SessionControllerImpl sessionController = getSessionController();
        return sessionController.isEnabled() ? sessionController : null;
    }

    // Control methods

    @Override
    public void pause() {
        getDirtyConfig().isPaused = true;
        getTracker().pauseEventTracking();
    }

    @Override
    public void resume() {
        getDirtyConfig().isPaused = false;
        getTracker().resumeEventTracking();
    }

    @Override
    public @Nullable UUID track(@NonNull Event event) {
        return getTracker().track(event);
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.TRACKER_LABEL;
    }

    @Override
    public boolean isTracking() {
        return getTracker().getDataCollection();
    }



    // Getters and Setters

    @NonNull
    public String getNamespace() {
        return getTracker().namespace;
    }

    @NonNull
    @Override
    public String getAppId() {
        return getTracker().appId;
    }

    @Override
    public void setAppId(@NonNull String appId) {
        getDirtyConfig().appId = appId;
        getDirtyConfig().appIdUpdated = true;
        getTracker().appId = appId;
    }

    @NonNull
    @Override
    public DevicePlatform getDevicePlatform() {
        return getTracker().devicePlatform;
    }

    @Override
    public void setDevicePlatform(@NonNull DevicePlatform devicePlatform) {
        getDirtyConfig().devicePlatform = devicePlatform;
        getDirtyConfig().devicePlatformUpdated = true;
        getTracker().devicePlatform = devicePlatform;
    }

    @Override
    public boolean isBase64encoding() {
        return getTracker().base64Encoded;
    }

    @Override
    public void setBase64encoding(boolean base64encoding) {
        getDirtyConfig().base64encoding = base64encoding;
        getDirtyConfig().base64encodingUpdated = true;
        getTracker().base64Encoded = base64encoding;
    }

    @NonNull
    @Override
    public LogLevel getLogLevel() {
        return getTracker().level;
    }

    @Override
    public void setLogLevel(@NonNull LogLevel logLevel) {
        getDirtyConfig().logLevel = logLevel;
        getDirtyConfig().logLevelUpdated = true;
        getTracker().level = logLevel;
    }

    @Nullable
    @Override
    public LoggerDelegate getLoggerDelegate() {
        return Logger.getDelegate();
    }

    @Override
    public void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        getDirtyConfig().loggerDelegate = loggerDelegate;
        getDirtyConfig().loggerDelegateUpdated = true;
        Logger.setDelegate(loggerDelegate);
    }

    @Override
    public boolean isApplicationContext() {
        return getTracker().applicationContext;
    }

    @Override
    public void setApplicationContext(boolean applicationContext) {
        getDirtyConfig().applicationContext = applicationContext;
        getDirtyConfig().applicationContextUpdated = true;
        getTracker().applicationContext = applicationContext;
    }

    @Override
    public boolean isPlatformContext() {
        return getTracker().mobileContext;
    }

    @Override
    public void setPlatformContext(boolean platformContext) {
        getDirtyConfig().platformContext = platformContext;
        getDirtyConfig().platformContextUpdated = true;
        getTracker().mobileContext = platformContext;
    }

    @Override
    public boolean isGeoLocationContext() {
        return getTracker().geoLocationContext;
    }

    @Override
    public void setGeoLocationContext(boolean geoLocationContext) {
        getDirtyConfig().geoLocationContext = geoLocationContext;
        getDirtyConfig().geoLocationContextUpdated = true;
        getTracker().geoLocationContext = geoLocationContext;
    }

    @Override
    public boolean isSessionContext() {
        return getTracker().getSessionContext();
    }

    @Override
    public void setSessionContext(boolean sessionContext) {
        getDirtyConfig().sessionContext = sessionContext;
        getDirtyConfig().sessionContextUpdated = true;
        getTracker().setSessionContext(sessionContext);
    }

    @Override
    public boolean isDeepLinkContext() {
        return getTracker().getDeepLinkContext();
    }

    @Override
    public void setDeepLinkContext(boolean deepLinkContext) {
        getDirtyConfig().deepLinkContext = deepLinkContext;
        getDirtyConfig().deepLinkContextUpdated = true;
        getTracker().setDeepLinkContext(deepLinkContext);
    }

    @Override
    public boolean isScreenContext() {
        return getTracker().getScreenContext();
    }

    @Override
    public void setScreenContext(boolean screenContext) {
        getDirtyConfig().screenContext = screenContext;
        getDirtyConfig().screenContextUpdated = true;
        getTracker().setScreenContext(screenContext);
    }

    @Override
    public boolean isScreenViewAutotracking() {
        return getTracker().activityTracking;
    }

    @Override
    public void setScreenViewAutotracking(boolean screenViewAutotracking) {
        getDirtyConfig().screenViewAutotracking = screenViewAutotracking;
        getDirtyConfig().screenViewAutotrackingUpdated = true;
        getTracker().activityTracking = screenViewAutotracking;
    }

    @Override
    public boolean isLifecycleAutotracking() {
        return getTracker().lifecycleEvents;
    }

    @Override
    public void setLifecycleAutotracking(boolean lifecycleAutotracking) {
        getDirtyConfig().lifecycleAutotracking = lifecycleAutotracking;
        getDirtyConfig().lifecycleAutotrackingUpdated = true;
        getTracker().lifecycleEvents = lifecycleAutotracking;
    }

    @Override
    public boolean isInstallAutotracking() {
        return getTracker().installTracking;
    }

    @Override
    public void setInstallAutotracking(boolean installAutotracking) {
        getDirtyConfig().installAutotracking = installAutotracking;
        getDirtyConfig().installAutotrackingUpdated = true;
        getTracker().installTracking = installAutotracking;
    }

    @Override
    public boolean isExceptionAutotracking() {
        return getTracker().applicationCrash;
    }

    @Override
    public void setExceptionAutotracking(boolean exceptionAutotracking) {
        getDirtyConfig().exceptionAutotracking = exceptionAutotracking;
        getDirtyConfig().exceptionAutotrackingUpdated = true;
        getTracker().applicationCrash = exceptionAutotracking;
    }

    @Override
    public boolean isDiagnosticAutotracking() {
        return getTracker().trackerDiagnostic;
    }

    @Override
    public void setDiagnosticAutotracking(boolean diagnosticAutotracking) {
        getDirtyConfig().diagnosticAutotracking = diagnosticAutotracking;
        getDirtyConfig().diagnosticAutotrackingUpdated = true;
        getTracker().trackerDiagnostic = diagnosticAutotracking;
    }

    @Override
    public boolean isUserAnonymisation() {
        return getTracker().isUserAnonymisation();
    }

    @Override
    public void setUserAnonymisation(boolean userAnonymisation) {
        getDirtyConfig().userAnonymisation = userAnonymisation;
        getDirtyConfig().userAnonymisationUpdated = true;
        getTracker().setUserAnonymisation(userAnonymisation);
    }

    @Nullable
    @Override
    public String getTrackerVersionSuffix() {
        return getTracker().trackerVersionSuffix;
    }

    @Override
    public void setTrackerVersionSuffix(@Nullable String trackerVersionSuffix) {
        // The trackerVersionSuffix shouldn't be updated.
    }

    // Private methods

    @NonNull
    private Tracker getTracker() {
        if (!serviceProvider.isTrackerInitialized()) {
            getLoggerDelegate().error(TAG, "Recreating tracker instance after it was removed. This will not be supported in future versions.");
        }
        return serviceProvider.getOrMakeTracker();
    }

    @NonNull
    private TrackerConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getTrackerConfigurationUpdate();
    }
}
