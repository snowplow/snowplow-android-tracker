package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.DevicePlatform;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.LogLevel;

public interface TrackerConfigurationInterface {

    /**
     * Identifer of the app.
     */
    @NonNull
    String getAppId();

    /**
     * Identifer of the app.
     */
    void setAppId(@NonNull String appId);

    /**
     * It sets the device platform the tracker is running on.
     */
    @NonNull
    DevicePlatform getDevicePlatform();

    /**
     * It sets the device platform the tracker is running on.
     */
    void setDevicePlatform(@NonNull DevicePlatform devicePlatform);

    /**
     * It indicates whether the JSON data in the payload should be base64 encoded.
     */
    boolean isBase64encoding();

    /**
     * It indicates whether the JSON data in the payload should be base64 encoded.
     */
    void setBase64encoding(boolean base64encoding);

    /**
     * It sets the log level of tracker logs.
     */
    @NonNull
    LogLevel getLogLevel();

    /**
     * It sets the log level of tracker logs.
     */
    void setLogLevel(@NonNull LogLevel logLevel);

    /**
     * It sets the logger delegate that receive logs from the tracker.
     */
    @Nullable
    LoggerDelegate getLoggerDelegate();

    /**
     * It sets the logger delegate that receive logs from the tracker.
     */
    void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate);

    /**
     * Whether application context is sent with all the tracked events.
     */
    boolean isApplicationContext();

    /**
     * Whether application context is sent with all the tracked events.
     */
    void setApplicationContext(boolean applicationContext);

    /**
     * Whether mobile/platform context is sent with all the tracked events.
     */
    boolean isPlatformContext();

    /**
     * Whether mobile/platform context is sent with all the tracked events.
     */
    void setPlatformContext(boolean platformContext);

    /**
     * Whether geo-location context is sent with all the tracked events.
     *
     * @apiNote Requires Location permissions as per the requirements of the various
     * Android versions. Otherwise the whole context is skipped.
     */
    boolean isGeoLocationContext();

    /**
     * Whether geo-location context is sent with all the tracked events.
     *
     * @apiNote Requires Location permissions as per the requirements of the various
     * Android versions. Otherwise the whole context is skipped.
     */
    void setGeoLocationContext(boolean geoLocationContext);

    /**
     * Whether session context is sent with all the tracked events.
     */
    boolean isSessionContext();

    /**
     * Whether session context is sent with all the tracked events.
     */
    void setSessionContext(boolean sessionContext);

    /**
     * Whether deepLink context is sent with all the ScreenView events.
     */
    boolean isDeepLinkContext();

    /**
     * Whether deepLink context is sent with all the ScreenView events.
     */
    void setDeepLinkContext(boolean deepLinkContext);

    /**
     * Whether screen context is sent with all the tracked events.
     */
    boolean isScreenContext();

    /**
     * Whether screen context is sent with all the tracked events.
     */
    void setScreenContext(boolean screenContext);

    /**
     * Whether enable automatic tracking of ScreenView events.
     */
    boolean isScreenViewAutotracking();

    /**
     * Whether enable automatic tracking of ScreenView events.
     */
    void setScreenViewAutotracking(boolean screenViewAutotracking);

    /**
     * Whether enable automatic tracking of background and foreground transitions.
     * @apiNote It needs the Foreground library installed.
     */
    boolean isLifecycleAutotracking();

    /**
     * Whether enable automatic tracking of background and foreground transitions.
     * @apiNote It needs the Foreground library installed.
     */
    void setLifecycleAutotracking(boolean lifecycleAutotracking);

    /**
     * Whether enable automatic tracking of install event.
     */
    boolean isInstallAutotracking();

    /**
     * Whether enable automatic tracking of install event.
     */
    void setInstallAutotracking(boolean installAutotracking);

    /**
     * Whether enable crash reporting.
     */
    boolean isExceptionAutotracking();

    /**
     * Whether enable crash reporting.
     */
    void setExceptionAutotracking(boolean exceptionAutotracking);

    /**
     * Whether enable diagnostic reporting.
     */
    boolean isDiagnosticAutotracking();

    /**
     * Whether enable diagnostic reporting.
     */
    void setDiagnosticAutotracking(boolean diagnosticAutotracking);

    /**
     * Whether to anonymise client-side user identifiers in session (userId, previousSessionId), subject (userId, networkUserId, domainUserId, ipAddress) and platform context entities (IDFA)
     */
    boolean isUserAnonymisation();

    /**
     * Whether to anonymise client-side user identifiers in session (userId, previousSessionId), subject (userId, networkUserId, domainUserId, ipAddress) and platform context entities (IDFA)
     * Setting this property on a running tracker instance starts a new session (if sessions are tracked).
     */
    void setUserAnonymisation(boolean userAnonymisation);

    /**
     * Decorate the v_tracker field in the tracker protocol.
     *
     * @note Do not use. Internal use only.
     */
    @Nullable
    String getTrackerVersionSuffix();

    /**
     * Decorate the v_tracker field in the tracker protocol.
     * @note Do not use. Internal use only.
     */
    void setTrackerVersionSuffix(@Nullable String trackerVersionSuffix);
}
