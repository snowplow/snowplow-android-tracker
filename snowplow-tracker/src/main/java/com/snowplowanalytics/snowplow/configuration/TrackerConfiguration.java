package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.tracker.TrackerConfigurationInterface;
import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

import org.json.JSONObject;

/**
 * This class represents the configuration of the tracker and the core tracker properties.
 * The TrackerConfiguration can be used to setup the tracker behaviour indicating what should be
 * tracked in term of automatic tracking and contexts/entities to track with the events.
 */
public class TrackerConfiguration implements TrackerConfigurationInterface, Configuration {
    public final static String TAG = TrackerConfiguration.class.getSimpleName();

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
    public DevicePlatform getDevicePlatform() {
        return devicePlatform;
    }

    @Override
    public void setDevicePlatform(@NonNull DevicePlatform devicePlatform) {
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
     *         lifecycleAutotracking = false;
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
        lifecycleAutotracking = false;
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
     * @apiNote Requires Location permissions as per the requirements of the various
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
        copy.exceptionAutotracking = exceptionAutotracking;
        copy.diagnosticAutotracking = diagnosticAutotracking;
        return copy;
    }

    // JSON Formatter

    public TrackerConfiguration(@NonNull String appId, @NonNull JSONObject jsonObject) {
        this(jsonObject.optString("appId", appId));
        String val = jsonObject.optString("devicePlatform", DevicePlatform.Mobile.getValue());
        devicePlatform = DevicePlatform.getByValue(val);
        String log = jsonObject.optString("logLevel", LogLevel.OFF.name());
        try {
            logLevel = LogLevel.valueOf(log.toUpperCase());
        } catch (Exception e) {
            Logger.e(TAG, "Unable to decode `logLevel from remote configuration.");
        }
        sessionContext = jsonObject.optBoolean("sessionContext", sessionContext);
        applicationContext = jsonObject.optBoolean("applicationContext", applicationContext);
        platformContext = jsonObject.optBoolean("platformContext", platformContext);
        geoLocationContext = jsonObject.optBoolean("geoLocationContext", geoLocationContext);
        screenContext = jsonObject.optBoolean("screenContext", screenContext);
        screenViewAutotracking = jsonObject.optBoolean("screenViewAutotracking", screenViewAutotracking);
        lifecycleAutotracking = jsonObject.optBoolean("lifecycleAutotracking", lifecycleAutotracking);
        installAutotracking = jsonObject.optBoolean("installAutotracking", installAutotracking);
        exceptionAutotracking = jsonObject.optBoolean("exceptionAutotracking", exceptionAutotracking);
        diagnosticAutotracking = jsonObject.optBoolean("diagnosticAutotracking", diagnosticAutotracking);
    }
}
