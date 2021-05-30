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

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TrackerControllerImpl extends Controller implements TrackerController {

    // Constructors

    public TrackerControllerImpl(@NonNull ServiceProvider serviceProvider) {
        super(serviceProvider);
    }

    //

    @NonNull
    public Tracker getTracker() {
        return serviceProvider.getTracker();
    }

    // Sub-controllers

    @Nullable
    @Override
    public NetworkController getNetwork() {
        return serviceProvider.getNetworkController();
    }

    @Override
    @NonNull
    public EmitterController getEmitter() {
        return serviceProvider.getEmitterController();
    }

    @Override
    @NonNull
    public SubjectController getSubject() {
        return serviceProvider.getSubjectController();
    }

    @Override
    @NonNull
    public GdprController getGdpr() {
        return serviceProvider.getGdprController();
    }

    @NonNull
    @Override
    public GlobalContextsController getGlobalContexts() {
        return serviceProvider.getGlobalContextsController();
    }

    @NonNull
    public SessionControllerImpl getSessionController() {
        return serviceProvider.getSessionController();
    }

    @Nullable
    public SessionController getSession() {
        SessionControllerImpl sessionController = getSessionController();
        return sessionController.isEnabled() ? sessionController : null;
    }

    // Control methods

    @Override
    public void pause() {
        getTracker().pauseEventTracking();
    }

    @Override
    public void resume() {
        getTracker().resumeEventTracking();
    }

    @Override
    public void track(@NonNull Event event) {
        getTracker().track(event);
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
        getTracker().appId = appId;
    }

    @NonNull
    @Override
    public DevicePlatform getDevicePlatform() {
        return getTracker().devicePlatform;
    }

    @Override
    public void setDevicePlatform(@NonNull DevicePlatform devicePlatform) {
        getTracker().devicePlatform = devicePlatform;
    }

    @Override
    public boolean isBase64encoding() {
        return getTracker().base64Encoded;
    }

    @Override
    public void setBase64encoding(boolean base64encoding) {
        getTracker().base64Encoded = base64encoding;
    }

    @NonNull
    @Override
    public LogLevel getLogLevel() {
        return getTracker().level;
    }

    @Override
    public void setLogLevel(@NonNull LogLevel logLevel) {
        getTracker().level = logLevel;
    }

    @Nullable
    @Override
    public LoggerDelegate getLoggerDelegate() {
        return Logger.getDelegate();
    }

    @Override
    public void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        Logger.setDelegate(loggerDelegate);
    }

    @Override
    public boolean isApplicationContext() {
        return getTracker().applicationContext;
    }

    @Override
    public void setApplicationContext(boolean applicationContext) {
        getTracker().applicationContext = applicationContext;
    }

    @Override
    public boolean isPlatformContext() {
        return getTracker().mobileContext;
    }

    @Override
    public void setPlatformContext(boolean platformContext) {
        getTracker().mobileContext = platformContext;
    }

    @Override
    public boolean isGeoLocationContext() {
        return getTracker().geoLocationContext;
    }

    @Override
    public void setGeoLocationContext(boolean geoLocationContext) {
        getTracker().geoLocationContext = geoLocationContext;
    }

    @Override
    public boolean isSessionContext() {
        return getTracker().getSessionContext();
    }

    @Override
    public void setSessionContext(boolean sessionContext) {
        getTracker().setSessionContext(sessionContext);
    }

    @Override
    public boolean isScreenContext() {
        return getTracker().screenContext;
    }

    @Override
    public void setScreenContext(boolean screenContext) {
        getTracker().screenContext = screenContext;
    }

    @Override
    public boolean isScreenViewAutotracking() {
        return getTracker().screenviewEvents;
    }

    @Override
    public void setScreenViewAutotracking(boolean screenViewAutotracking) {
        getTracker().screenviewEvents = screenViewAutotracking;
    }

    @Override
    public boolean isLifecycleAutotracking() {
        return getTracker().lifecycleEvents;
    }

    @Override
    public void setLifecycleAutotracking(boolean lifecycleAutotracking) {
        getTracker().lifecycleEvents = lifecycleAutotracking;
    }

    @Override
    public boolean isInstallAutotracking() {
        return getTracker().installTracking;
    }

    @Override
    public void setInstallAutotracking(boolean installAutotracking) {
        getTracker().installTracking = installAutotracking;
    }

    @Override
    public boolean isExceptionAutotracking() {
        return getTracker().applicationCrash;
    }

    @Override
    public void setExceptionAutotracking(boolean exceptionAutotracking) {
        getTracker().applicationCrash = exceptionAutotracking;
    }

    @Override
    public boolean isDiagnosticAutotracking() {
        return getTracker().trackerDiagnostic;
    }

    @Override
    public void setDiagnosticAutotracking(boolean diagnosticAutotracking) {
        getTracker().trackerDiagnostic = diagnosticAutotracking;
    }
}
