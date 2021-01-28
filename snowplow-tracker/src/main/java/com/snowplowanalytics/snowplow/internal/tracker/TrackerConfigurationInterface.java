package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;

public interface TrackerConfigurationInterface {

    @NonNull
    String getNamespace();

    void setNamespace(@NonNull String namespace);

    @NonNull
    String getAppId();

    void setAppId(@NonNull String appId);

    @NonNull
    DevicePlatforms getDevicePlatform();

    void setDevicePlatform(@NonNull DevicePlatforms devicePlatform);

    boolean isBase64encoding();

    void setBase64encoding(boolean base64encoding);

    @NonNull
    LogLevel getLogLevel();

    void setLogLevel(@NonNull LogLevel logLevel);

    @Nullable
    LoggerDelegate getLoggerDelegate();

    void setLoggerDelegate(@Nullable LoggerDelegate loggerDelegate);

    boolean isApplicationContext();

    void setApplicationContext(boolean applicationContext);

    boolean isPlatformContext();

    void setPlatformContext(boolean platformContext);

    boolean isGeoLocationContext();

    void setGeoLocationContext(boolean geoLocationContext);

    boolean isSessionContext();

    void setSessionContext(boolean sessionContext);

    boolean isScreenContext();

    void setScreenContext(boolean screenContext);

    boolean isScreenViewAutotracking();

    void setScreenViewAutotracking(boolean screenViewAutotracking);

    boolean isLifecycleAutotracking();

    void setLifecycleAutotracking(boolean lifecycleAutotracking);

    boolean isInstallAutotracking();

    void setInstallAutotracking(boolean installAutotracking);

    boolean isExceptionAutotracking();

    void setExceptionAutotracking(boolean exceptionAutotracking);

    boolean isDiagnosticAutotracking();

    void setDiagnosticAutotracking(boolean diagnosticAutotracking);
}
