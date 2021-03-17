package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.emitter.BufferOption;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

/**
 * This class allows the tracker configuration from the tracking perspective.
 * The TrackerConfiguration can be used to setup the tracker behaviour indicating what should be
 * tracked in term of automatic tracking and contexts/entities to track with the events.
 */
public class TrackerConfiguration implements TrackerConfigurationInterface, Configuration {

    /**
     * @see #appId(String)
     */
    @NonNull
    public String appId;

    /**
     * @see #devicePlatform(DevicePlatform)
     */
    @NonNull
    public DevicePlatform devicePlatform;

    /**
     * @see #base64encoding(boolean)
     */
    public boolean base64encoding;

    /**
     * @see #logLevel(LogLevel)
     */
    @NonNull
    public LogLevel logLevel;

    /**
     * @see #loggerDelegate(LoggerDelegate)
     */
    @Nullable
    public LoggerDelegate loggerDelegate;

    /**
     * @see #applicationContext(boolean)
     */
    public boolean applicationContext;
    /**
     * @see #platformContext(boolean) 
     */
    public boolean platformContext;
    /**
     * @see #geoLocationContext(boolean) 
     */
    public boolean geoLocationContext;
    /**
     * @see #sessionContext(boolean) 
     */
    public boolean sessionContext;
    /**
     * @see #screenContext(boolean) 
     */
    public boolean screenContext;
    /**
     * @see #screenViewAutotracking(boolean) 
     */
    public boolean screenViewAutotracking;
    /**
     * @see #lifecycleAutotracking(boolean) 
     */
    public boolean lifecycleAutotracking;
    /**
     * @see #installAutotracking(boolean) 
     */
    public boolean installAutotracking;
    /**
     * @see #exceptionAutotracking(boolean) 
     */
    public boolean exceptionAutotracking;
    /**
     * @see #diagnosticAutotracking(boolean) 
     */
    public boolean diagnosticAutotracking;

    // Getters and Setters

    /**
     * @return The data set in {@link #appId(String)}
     */
    @Override
    @NonNull
    public String getAppId() {
        return appId;
    }

    /**
     * @see #appId(String)
     */
    @Override
    public void setAppId(@NonNull String appId) {
        this.appId = appId;
    }

    /**
     * @return The data set in {@link #devicePlatform(DevicePlatform)}
     */
    @Override
    @NonNull
    public DevicePlatform getDevicePlatform() {
        return devicePlatform;
    }

    /**
     * @see #devicePlatform(DevicePlatform)
     */
    @Override
    public void setDevicePlatform(@NonNull DevicePlatform devicePlatform) {
        this.devicePlatform = devicePlatform;
    }

    /**
     * @return The data set in {@link #base64encoding(boolean)}
     */
    @Override
    public boolean isBase64encoding() {
        return base64encoding;
    }

    /**
     * @see #base64encoding(boolean)
     */
    @Override
    public void setBase64encoding(boolean base64encoding) {
        this.base64encoding = base64encoding;
    }

    /**
     * @return The data set in {@link #logLevel(LogLevel)}
     */
    @Override
    @NonNull
    public LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * @see #logLevel(LogLevel)
     */
    @Override
    public void setLogLevel(@NonNull LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * @return The data set in {@link #loggerDelegate(LoggerDelegate)}
     */
    @Override
    @Nullable
    public LoggerDelegate getLoggerDelegate() {
        return loggerDelegate;
    }

    /**
     * @see #loggerDelegate(LoggerDelegate)
     */
    @Override
    public void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        this.loggerDelegate = loggerDelegate;
    }

    /**
     * @return The data set in {@link #applicationContext(boolean)}
     */
    @Override
    public boolean isApplicationContext() {
        return applicationContext;
    }

    /**
     * @see #applicationContext(boolean)
     */
    @Override
    public void setApplicationContext(boolean applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * @return The data set in {@link #platformContext(boolean)}
     */
    @Override
    public boolean isPlatformContext() {
        return platformContext;
    }

    /**
     * @see #platformContext(boolean)
     */
    @Override
    public void setPlatformContext(boolean platformContext) {
        this.platformContext = platformContext;
    }

    /**
     * @return The data set in {@link #geoLocationContext(boolean)}
     */
    @Override
    public boolean isGeoLocationContext() {
        return geoLocationContext;
    }

    /**
     * @see #geoLocationContext(boolean)
     */
    @Override
    public void setGeoLocationContext(boolean geoLocationContext) {
        this.geoLocationContext = geoLocationContext;
    }

    /**
     * @return The data set in {@link #sessionContext(boolean)}
     */
    @Override
    public boolean isSessionContext() {
        return sessionContext;
    }

    /**
     * @see #sessionContext(boolean)
     */
    @Override
    public void setSessionContext(boolean sessionContext) {
        this.sessionContext = sessionContext;
    }

    /**
     * @return The data set in {@link #screenContext(boolean)}
     */
    @Override
    public boolean isScreenContext() {
        return screenContext;
    }

    /**
     * @see #screenContext(boolean)
     */
    @Override
    public void setScreenContext(boolean screenContext) {
        this.screenContext = screenContext;
    }

    /**
     * @return The data set in {@link #screenViewAutotracking(boolean)}
     */
    @Override
    public boolean isScreenViewAutotracking() {
        return screenViewAutotracking;
    }

    /**
     * @see #screenViewAutotracking(boolean)
     */
    @Override
    public void setScreenViewAutotracking(boolean screenViewAutotracking) {
        this.screenViewAutotracking = screenViewAutotracking;
    }

    /**
     * @return The data set in {@link #lifecycleAutotracking(boolean)}
     */
    @Override
    public boolean isLifecycleAutotracking() {
        return lifecycleAutotracking;
    }

    /**
     * @see #lifecycleAutotracking(boolean)
     */
    @Override
    public void setLifecycleAutotracking(boolean lifecycleAutotracking) {
        this.lifecycleAutotracking = lifecycleAutotracking;
    }

    /**
     * @return The data set in {@link #installAutotracking(boolean)}
     */
    @Override
    public boolean isInstallAutotracking() {
        return installAutotracking;
    }

    /**
     * @see #installAutotracking(boolean)
     */
    @Override
    public void setInstallAutotracking(boolean installAutotracking) {
        this.installAutotracking = installAutotracking;
    }

    /**
     * @return The data set in {@link #exceptionAutotracking(boolean)}
     */
    @Override
    public boolean isExceptionAutotracking() {
        return exceptionAutotracking;
    }

    /**
     * @see #exceptionAutotracking(boolean)
     */
    @Override
    public void setExceptionAutotracking(boolean exceptionAutotracking) {
        this.exceptionAutotracking = exceptionAutotracking;
    }

    /**
     * @return The data set in {@link #diagnosticAutotracking(boolean)}
     */
    @Override
    public boolean isDiagnosticAutotracking() {
        return diagnosticAutotracking;
    }

    /**
     * @see #diagnosticAutotracking(boolean)
     */
    @Override
    public void setDiagnosticAutotracking(boolean diagnosticAutotracking) {
        this.diagnosticAutotracking = diagnosticAutotracking;
    }

    // Constructors

    /**
     * It sets a default TrackerConfiguration.
     * Default values:
     *         devicePlatform = DevicePlatform.Mobile;
     *         base64encoding = true;
     *         logLevel = LogLevel.OFF;
     *         loggerDelegate = null;
     *         sessionContext = true;
     *         applicationContext = true;
     *         platformContext = true;
     *         geoLocationContext = false;
     *         screenContext = true;
     *         screenViewAutotracking = true;
     *         lifecycleAutotracking = true;
     *         installAutotracking = true;
     *         exceptionAutotracking = true;
     *         diagnosticAutotracking = false;
     * @param appId Identifier of the app.
     */
    public TrackerConfiguration(@NonNull String appId) {
        this.appId = appId;

        devicePlatform = DevicePlatform.Mobile;
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

    /**
     * Identifer of the app.
     */
    @NonNull
    public TrackerConfiguration appId(@NonNull String appId) {
        this.appId = appId;
        return this;
    }

    /**
     * It sets the device platform the tracker is running on.
     */
    @NonNull
    public TrackerConfiguration devicePlatform(@NonNull DevicePlatform devicePlatform) {
        this.devicePlatform = devicePlatform;
        return this;
    }

    /**
     * It indicates whether the JSON data in the payload should be base64 encoded.
     */
    @NonNull
    public TrackerConfiguration base64encoding(boolean base64encoding) {
        this.base64encoding = base64encoding;
        return this;
    }

    /**
     * It sets the log level of tracker logs.
     */
    @NonNull
    public TrackerConfiguration logLevel(@NonNull LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * It sets the logger delegate that receive logs from the tracker.
     */
    @NonNull
    public TrackerConfiguration loggerDelegate(@Nullable LoggerDelegate loggerDelegate) {
        this.loggerDelegate = loggerDelegate;
        return this;
    }

    /**
     * Whether application context is sent with all the tracked events.
     */
    @NonNull
    public TrackerConfiguration applicationContext(boolean applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    /**
     * Whether mobile/platform context is sent with all the tracked events.
     */
    @NonNull
    public TrackerConfiguration platformContext(boolean platformContext) {
        this.platformContext = platformContext;
        return this;
    }

    /**
     * Whether geo-location context is sent with all the tracked events.
     *
     * @apiNote Requires Location permissions accordingly to the requirements of the various
     * Android versions. Otherwise the whole context is skipped.
     */
    @NonNull
    public TrackerConfiguration geoLocationContext(boolean geoLocationContext) {
        this.geoLocationContext = geoLocationContext;
        return this;
    }

    /**
     * Whether session context is sent with all the tracked events.
     */
    @NonNull
    public TrackerConfiguration sessionContext(boolean sessionContext) {
        this.sessionContext = sessionContext;
        return this;
    }

    /**
     * Whether screen context is sent with all the tracked events.
     */
    @NonNull
    public TrackerConfiguration screenContext(boolean screenContext) {
        this.screenContext = screenContext;
        return this;
    }

    /**
     * Whether enable automatic tracking of ScreenView events.
     */
    @NonNull
    public TrackerConfiguration screenViewAutotracking(boolean screenViewAutotracking) {
        this.screenViewAutotracking = screenViewAutotracking;
        return this;
    }

    /**
     * Whether enable automatic tracking of background and foreground transitions.
     * @apiNote It needs the Foreground library installed.
     */
    @NonNull
    public TrackerConfiguration lifecycleAutotracking(boolean lifecycleAutotracking) {
        this.lifecycleAutotracking = lifecycleAutotracking;
        return this;
    }

    /**
     * Whether enable automatic tracking of install event.
     */
    @NonNull
    public TrackerConfiguration installAutotracking(boolean installAutotracking) {
        this.installAutotracking = installAutotracking;
        return this;
    }

    /**
     * Whether enable crash reporting.
     */
    @NonNull
    public TrackerConfiguration exceptionAutotracking(boolean exceptionAutotracking) {
        this.exceptionAutotracking = exceptionAutotracking;
        return this;
    }

    /**
     * Whether enable diagnostic reporting.
     */
    @NonNull
    public TrackerConfiguration diagnosticAutotracking(boolean diagnosticAutotracking) {
        this.diagnosticAutotracking = diagnosticAutotracking;
        return this;
    }

    // Copyable

    @NonNull
    @Override
    public Configuration copy() {
        TrackerConfiguration copy = new TrackerConfiguration(appId);
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
