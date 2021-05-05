package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.util.Size;

public interface SubjectConfigurationInterface {
    @Nullable
    String getUserId();

    void setUserId(@Nullable String userId);

    @Nullable
    String getNetworkUserId();

    void setNetworkUserId(@Nullable String networkUserId);

    @Nullable
    String getDomainUserId();

    void setDomainUserId(@Nullable String domainUserId);

    @Nullable
    String getUseragent();

    void setUseragent(@Nullable String useragent);

    @Nullable
    String getIpAddress();

    void setIpAddress(@Nullable String ipAddress);

    @Nullable
    String getTimezone();

    void setTimezone(@Nullable String timezone);

    @Nullable
    String getLanguage();

    void setLanguage(@Nullable String language);

    @Nullable
    Size getScreenResolution();

    void setScreenResolution(@Nullable Size screenResolution);

    @Nullable
    Size getScreenViewPort();

    void setScreenViewPort(@Nullable Size screenViewPort);

    @Nullable
    Integer getColorDepth();

    void setColorDepth(@Nullable Integer colorDepth);
}
