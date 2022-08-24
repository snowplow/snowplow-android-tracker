package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LogLevel;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;

import org.json.JSONObject;

public class TrackerConfigurationUpdate extends TrackerConfiguration {

    @Nullable
    public TrackerConfiguration sourceConfig;

    public boolean isPaused;

    public TrackerConfigurationUpdate(@NonNull String appId) {
        super(appId);
    }

    public TrackerConfigurationUpdate(@NonNull String appId, @NonNull JSONObject jsonObject) {
        super(appId, jsonObject);
    }

    // appId flag

    public boolean appIdUpdated;

    @NonNull
    public String getAppId() {
        return (sourceConfig == null || appIdUpdated) ? super.appId : sourceConfig.appId;
    }

    // devicePlatform flag

    public boolean devicePlatformUpdated;

    @NonNull
    public DevicePlatform getDevicePlatform() {
        return (sourceConfig == null || devicePlatformUpdated) ? super.devicePlatform : sourceConfig.devicePlatform;
    }

    // base64encoding flag

    public boolean base64encodingUpdated;

    public boolean isBase64encoding() {
        return (sourceConfig == null || base64encodingUpdated) ? super.base64encoding : sourceConfig.base64encoding;
    }

    // logLevel flag

    public boolean logLevelUpdated;

    @NonNull
    public LogLevel getLogLevel() {
        return (sourceConfig == null || logLevelUpdated) ? super.logLevel : sourceConfig.logLevel;
    }

    // loggerDelegate flag

    public boolean loggerDelegateUpdated;

    @Nullable
    public LoggerDelegate getLoggerDelegate() {
        return (sourceConfig == null || loggerDelegateUpdated) ? super.loggerDelegate : sourceConfig.loggerDelegate;
    }

    // applicationContext flag

    public boolean applicationContextUpdated;

    public boolean isApplicationContext() {
        return (sourceConfig == null || applicationContextUpdated) ? super.applicationContext : sourceConfig.applicationContext;
    }

    // platformContext flag

    public boolean platformContextUpdated;

    public boolean isPlatformContext() {
        return (sourceConfig == null || platformContextUpdated) ? super.platformContext : sourceConfig.platformContext;
    }

    // geoLocationContext flag

    public boolean geoLocationContextUpdated;

    public boolean isGeoLocationContext() {
        return (sourceConfig == null || geoLocationContextUpdated) ? super.geoLocationContext : sourceConfig.geoLocationContext;
    }

    // sessionContext flag

    public boolean sessionContextUpdated;

    public boolean isSessionContext() {
        return (sourceConfig == null || sessionContextUpdated) ? super.sessionContext : sourceConfig.sessionContext;
    }

    // deepLinkContext flag

    public boolean deepLinkContextUpdated;

    public boolean isDeepLinkContext() {
        return (sourceConfig == null || deepLinkContextUpdated) ? super.deepLinkContext : sourceConfig.deepLinkContext;
    }

    // screenContext flag

    public boolean screenContextUpdated;

    public boolean isScreenContext() {
        return (sourceConfig == null || screenContextUpdated) ? super.screenContext : sourceConfig.screenContext;
    }

    // screenViewAutotracking flag

    public boolean screenViewAutotrackingUpdated;

    public boolean isScreenViewAutotracking() {
        return (sourceConfig == null || screenViewAutotrackingUpdated) ? super.screenViewAutotracking : sourceConfig.screenViewAutotracking;
    }

    // lifecycleAutotracking flag

    public boolean lifecycleAutotrackingUpdated;

    public boolean isLifecycleAutotracking() {
        return (sourceConfig == null || lifecycleAutotrackingUpdated) ? super.lifecycleAutotracking : sourceConfig.lifecycleAutotracking;
    }

    // installAutotracking flag

    public boolean installAutotrackingUpdated;

    public boolean isInstallAutotracking() {
        return (sourceConfig == null || installAutotrackingUpdated) ? super.installAutotracking : sourceConfig.installAutotracking;
    }

    // exceptionAutotracking flag

    public boolean exceptionAutotrackingUpdated;

    public boolean isExceptionAutotracking() {
        return (sourceConfig == null || exceptionAutotrackingUpdated) ? super.exceptionAutotracking : sourceConfig.exceptionAutotracking;
    }

    // diagnosticAutotracking flag

    public boolean diagnosticAutotrackingUpdated;

    public boolean isDiagnosticAutotracking() {
        return (sourceConfig == null || diagnosticAutotrackingUpdated) ? super.diagnosticAutotracking : sourceConfig.diagnosticAutotracking;
    }

    // userAnonymisation flag

    public boolean userAnonymisationUpdated;

    public boolean isUserAnonymisation() {
        return (sourceConfig == null || userAnonymisationUpdated) ? super.userAnonymisation : sourceConfig.userAnonymisation;
    }

    // trackerVersionSuffix flag

    public boolean trackerVersionSuffixUpdated;

    @Nullable
    public String getTrackerVersionSuffix() {
        return (sourceConfig == null || trackerVersionSuffixUpdated) ? super.trackerVersionSuffix : sourceConfig.trackerVersionSuffix;
    }
}
