package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.internal.network.NetworkControllerImpl;
import com.snowplowanalytics.snowplow.internal.session.SessionControllerImpl;
import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.NetworkConnection;
import com.snowplowanalytics.snowplow.tracker.OkHttpNetworkConnection;
import com.snowplowanalytics.snowplow.tracker.events.Event;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

public class TrackerControllerImpl implements TrackerController {

    @Nullable
    private NetworkControllerImpl network;
    @NonNull
    private SessionControllerImpl session;

    @NonNull
    private final Tracker tracker;

    // Constructors

    public TrackerControllerImpl(@NonNull Tracker tracker) {
        Emitter emitter = tracker.emitter;
        this.tracker = tracker;
        session = new SessionControllerImpl(tracker);
        // TODO: Add other controllers
        NetworkConnection networkConnection = emitter.getNetworkConnection();
        if (networkConnection == null || networkConnection instanceof OkHttpNetworkConnection) {
            network = new NetworkControllerImpl(emitter);
        }
    }

    // Sub-controllers

    @Nullable
    @Override
    public NetworkControllerImpl getNetwork() {
        return network;
    }

    @Override
    @Nullable
    public SessionControllerImpl getSession() {
        return session.isEnabled() ? session : null;
    }

    // Control methods

    @Override
    public void pause() {
        tracker.pauseEventTracking();
    }

    @Override
    public void resume() {
        tracker.resumeEventTracking();
    }

    @Override
    public void track(@NonNull Event event) {
        tracker.track(event);
    }

    @NonNull
    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public boolean isTracking() {
        return tracker.getDataCollection();
    }

    // Getters and Setters

    @NonNull
    @Override
    public String getNamespace() {
        return tracker.namespace;
    }

    @Override
    public void setNamespace(@NonNull String namespace) {
        tracker.namespace = namespace;
    }

    @NonNull
    @Override
    public String getAppId() {
        return tracker.appId;
    }

    @Override
    public void setAppId(@NonNull String appId) {
        tracker.appId = appId;
    }

    @NonNull
    @Override
    public DevicePlatforms getDevicePlatform() {
        return tracker.devicePlatform;
    }

    @Override
    public void setDevicePlatform(@NonNull DevicePlatforms devicePlatform) {
        tracker.devicePlatform = devicePlatform;
    }

    @Override
    public boolean isBase64encoding() {
        return tracker.base64Encoded;
    }

    @Override
    public void setBase64encoding(boolean base64encoding) {
        tracker.base64Encoded = base64encoding;
    }

    @NonNull
    @Override
    public LogLevel getLogLevel() {
        return tracker.level;
    }

    @Override
    public void setLogLevel(@NonNull LogLevel logLevel) {
        tracker.level = logLevel;
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
        return tracker.applicationContext;
    }

    @Override
    public void setApplicationContext(boolean applicationContext) {
        tracker.applicationContext = applicationContext;
    }

    @Override
    public boolean isPlatformContext() {
        return tracker.mobileContext;
    }

    @Override
    public void setPlatformContext(boolean platformContext) {
        tracker.mobileContext = platformContext;
    }

    @Override
    public boolean isGeoLocationContext() {
        return tracker.geoLocationContext;
    }

    @Override
    public void setGeoLocationContext(boolean geoLocationContext) {
        tracker.geoLocationContext = geoLocationContext;
    }

    @Override
    public boolean isSessionContext() {
        return tracker.sessionContext;
    }

    @Override
    public void setSessionContext(boolean sessionContext) {
        tracker.sessionContext = sessionContext;
    }

    @Override
    public boolean isScreenContext() {
        return tracker.screenContext;
    }

    @Override
    public void setScreenContext(boolean screenContext) {
        tracker.screenContext = screenContext;
    }

    @Override
    public boolean isScreenViewAutotracking() {
        return tracker.screenviewEvents;
    }

    @Override
    public void setScreenViewAutotracking(boolean screenViewAutotracking) {
        tracker.screenviewEvents = screenViewAutotracking;
    }

    @Override
    public boolean isLifecycleAutotracking() {
        return tracker.lifecycleEvents;
    }

    @Override
    public void setLifecycleAutotracking(boolean lifecycleAutotracking) {
        tracker.lifecycleEvents = lifecycleAutotracking;
    }

    @Override
    public boolean isInstallAutotracking() {
        return tracker.installTracking;
    }

    @Override
    public void setInstallAutotracking(boolean installAutotracking) {
        tracker.installTracking = installAutotracking;
    }

    @Override
    public boolean isExceptionAutotracking() {
        return tracker.applicationCrash;
    }

    @Override
    public void setExceptionAutotracking(boolean exceptionAutotracking) {
        tracker.applicationCrash = exceptionAutotracking;
    }

    @Override
    public boolean isDiagnosticAutotracking() {
        return tracker.trackerDiagnostic;
    }

    @Override
    public void setDiagnosticAutotracking(boolean diagnosticAutotracking) {
        tracker.trackerDiagnostic = diagnosticAutotracking;
    }
}
