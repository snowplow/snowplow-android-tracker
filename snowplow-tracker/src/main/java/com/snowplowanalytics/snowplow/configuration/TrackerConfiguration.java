package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

import java.util.Objects;

public class TrackerConfiguration implements TrackerConfigurationInterface, Configuration {

    @NonNull
    public String namespace;
    @NonNull
    public String appId;

    @NonNull
    public DevicePlatforms devicePlatform;  // TODO: Make DevicePlatforms name as singular
    public boolean base64encoding;

    @NonNull
    public LogLevel logLevel;
    @Nullable
    public LoggerDelegate loggerDelegate;

    public boolean applicationContext;
    public boolean platformContext;
    public boolean geoLocationContext;
    public boolean sessionContext;
    public boolean screenContext;
    public boolean screenViewAutotracking;
    public boolean lifecycleAutotracking;
    public boolean installAutotracking;
    public boolean exceptionAutotracking;
    public boolean diagnosticAutotracking;

    // Getters and Setters

    @Override
    @NonNull
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(@NonNull String namespace) {
        this.namespace = namespace;
    }

    @Override
    @NonNull
    public String getAppId() {
        return appId;
    }

    @Override
    public void setAppId(@NonNull String appId) {
        this.appId = appId;
    }

    @Override
    @NonNull
    public DevicePlatforms getDevicePlatform() {
        return devicePlatform;
    }

    @Override
    public void setDevicePlatform(@NonNull DevicePlatforms devicePlatform) {
        this.devicePlatform = devicePlatform;
    }

    @Override
    public boolean isBase64encoding() {
        return base64encoding;
    }

    @Override
    public void setBase64encoding(boolean base64encoding) {
        this.base64encoding = base64encoding;
    }

    @Override
    @NonNull
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public void setLogLevel(@NonNull LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    @Nullable
    public LoggerDelegate getLoggerDelegate() {
        return loggerDelegate;
    }

    @Override
    public void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        this.loggerDelegate = loggerDelegate;
    }

    @Override
    public boolean isApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(boolean applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean isPlatformContext() {
        return platformContext;
    }

    @Override
    public void setPlatformContext(boolean platformContext) {
        this.platformContext = platformContext;
    }

    @Override
    public boolean isGeoLocationContext() {
        return geoLocationContext;
    }

    @Override
    public void setGeoLocationContext(boolean geoLocationContext) {
        this.geoLocationContext = geoLocationContext;
    }

    @Override
    public boolean isSessionContext() {
        return sessionContext;
    }

    @Override
    public void setSessionContext(boolean sessionContext) {
        this.sessionContext = sessionContext;
    }

    @Override
    public boolean isScreenContext() {
        return screenContext;
    }

    @Override
    public void setScreenContext(boolean screenContext) {
        this.screenContext = screenContext;
    }

    @Override
    public boolean isScreenViewAutotracking() {
        return screenViewAutotracking;
    }

    @Override
    public void setScreenViewAutotracking(boolean screenViewAutotracking) {
        this.screenViewAutotracking = screenViewAutotracking;
    }

    @Override
    public boolean isLifecycleAutotracking() {
        return lifecycleAutotracking;
    }

    @Override
    public void setLifecycleAutotracking(boolean lifecycleAutotracking) {
        this.lifecycleAutotracking = lifecycleAutotracking;
    }

    @Override
    public boolean isInstallAutotracking() {
        return installAutotracking;
    }

    @Override
    public void setInstallAutotracking(boolean installAutotracking) {
        this.installAutotracking = installAutotracking;
    }

    @Override
    public boolean isExceptionAutotracking() {
        return exceptionAutotracking;
    }

    @Override
    public void setExceptionAutotracking(boolean exceptionAutotracking) {
        this.exceptionAutotracking = exceptionAutotracking;
    }

    @Override
    public boolean isDiagnosticAutotracking() {
        return diagnosticAutotracking;
    }

    @Override
    public void setDiagnosticAutotracking(boolean diagnosticAutotracking) {
        this.diagnosticAutotracking = diagnosticAutotracking;
    }

    // Constructors

    public TrackerConfiguration(@NonNull String namespace, @NonNull String appId) {
        Objects.requireNonNull(namespace);
        Objects.requireNonNull(appId);
        this.namespace = namespace;
        this.appId = appId;

        devicePlatform = DevicePlatforms.Mobile;
        base64encoding = true;

        logLevel = LogLevel.OFF;
        loggerDelegate = null;

        sessionContext = true;
        applicationContext = true;
        platformContext = true;
        geoLocationContext = false;
        screenContext = true;
        screenViewAutotracking = true;
        lifecycleAutotracking = true;
        installAutotracking = true;
        exceptionAutotracking = true;
        diagnosticAutotracking = false;
    }

    // Builder methods

    @NonNull
    public TrackerConfiguration namespace(@NonNull String namespace) {
        this.namespace = namespace;
        return this;
    }

    @NonNull
    public TrackerConfiguration appId(@NonNull String appId) {
        this.appId = appId;
        return this;
    }

    @NonNull
    public TrackerConfiguration devicePlatform(@NonNull DevicePlatforms devicePlatform) {
        this.devicePlatform = devicePlatform;
        return this;
    }

    @NonNull
    public TrackerConfiguration base64encoding(boolean base64encoding) {
        this.base64encoding = base64encoding;
        return this;
    }

    @NonNull
    public TrackerConfiguration logLevel(@NonNull LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    @NonNull
    public TrackerConfiguration loggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        this.loggerDelegate = loggerDelegate;
        return this;
    }

    @NonNull
    public TrackerConfiguration applicationContext(boolean applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    @NonNull
    public TrackerConfiguration platformContext(boolean platformContext) {
        this.platformContext = platformContext;
        return this;
    }

    @NonNull
    public TrackerConfiguration geoLocationContext(boolean geoLocationContext) {
        this.geoLocationContext = geoLocationContext;
        return this;
    }

    @NonNull
    public TrackerConfiguration sessionContext(boolean sessionContext) {
        this.sessionContext = sessionContext;
        return this;
    }

    @NonNull
    public TrackerConfiguration screenContext(boolean screenContext) {
        this.screenContext = screenContext;
        return this;
    }

    @NonNull
    public TrackerConfiguration screenViewAutotracking(boolean screenViewAutotracking) {
        this.screenViewAutotracking = screenViewAutotracking;
        return this;
    }

    @NonNull
    public TrackerConfiguration lifecycleAutotracking(boolean lifecycleAutotracking) {
        this.lifecycleAutotracking = lifecycleAutotracking;
        return this;
    }

    @NonNull
    public TrackerConfiguration installAutotracking(boolean installAutotracking) {
        this.installAutotracking = installAutotracking;
        return this;
    }

    @NonNull
    public TrackerConfiguration exceptionAutotracking(boolean exceptionAutotracking) {
        this.exceptionAutotracking = exceptionAutotracking;
        return this;
    }

    @NonNull
    public TrackerConfiguration diagnosticAutotracking(boolean diagnosticAutotracking) {
        this.diagnosticAutotracking = diagnosticAutotracking;
        return this;
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        TrackerConfiguration copy = new TrackerConfiguration(namespace, appId);
        copy.devicePlatform = devicePlatform;
        copy.base64encoding = base64encoding;

        copy.logLevel = logLevel;
        copy.loggerDelegate = loggerDelegate;

        copy.sessionContext = sessionContext;
        copy.applicationContext = applicationContext;
        copy.platformContext = platformContext;
        copy.geoLocationContext = geoLocationContext;
        copy.screenContext = screenContext;
        copy.screenViewAutotracking = screenViewAutotracking;
        copy.lifecycleAutotracking = lifecycleAutotracking;
        copy.installAutotracking = installAutotracking;
        copy. exceptionAutotracking = exceptionAutotracking;
        copy. diagnosticAutotracking = diagnosticAutotracking;
        return copy;
    }
}
