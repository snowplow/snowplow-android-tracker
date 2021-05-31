package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.util.Size;

public class SubjectConfigurationUpdate extends SubjectConfiguration {

    @Nullable
    public SubjectConfiguration sourceConfig;

    // userId flag

    public boolean userIdUpdated;

    @Nullable
    public String getUserId() {
        return (sourceConfig == null || userIdUpdated) ? super.userId : sourceConfig.userId;
    }

    // networkUserId flag

    public boolean networkUserIdUpdated;

    @Nullable
    public String getNetworkUserId() {
        return (sourceConfig == null || networkUserIdUpdated) ? super.networkUserId : sourceConfig.networkUserId;
    }

    // domainUserId flag

    public boolean domainUserIdUpdated;

    @Nullable
    public String getDomainUserId() {
        return (sourceConfig == null || domainUserIdUpdated) ? super.domainUserId : sourceConfig.domainUserId;
    }

    // useragent flag

    public boolean useragentUpdated;

    @Nullable
    public String getUseragent() {
        return (sourceConfig == null || useragentUpdated) ? super.useragent : sourceConfig.useragent;
    }

    // ipAddress flag

    public boolean ipAddressUpdated;

    @Nullable
    public String getIpAddress() {
        return (sourceConfig == null || ipAddressUpdated) ? super.ipAddress : sourceConfig.ipAddress;
    }

    // timezone flag

    public boolean timezoneUpdated;

    @Nullable
    public String getTimezone() {
        return (sourceConfig == null || timezoneUpdated) ? super.timezone : sourceConfig.timezone;
    }

    // language flag

    public boolean languageUpdated;

    @Nullable
    public String getLanguage() {
        return (sourceConfig == null || languageUpdated) ? super.language : sourceConfig.language;
    }

    // screenResolution flag

    public boolean screenResolutionUpdated;

    @Nullable
    public Size getScreenResolution() {
        return (sourceConfig == null || screenResolutionUpdated) ? super.screenResolution : sourceConfig.screenResolution;
    }

    // screenViewPort flag

    public boolean screenViewPortUpdated;

    @Nullable
    public Size getScreenViewPort() {
        return (sourceConfig == null || screenViewPortUpdated) ? super.screenViewPort : sourceConfig.screenViewPort;
    }

    // colorDepth flag

    public boolean colorDepthUpdated;

    @Nullable
    public Integer getColorDepth() {
        return (sourceConfig == null || colorDepthUpdated) ? super.colorDepth : sourceConfig.colorDepth;
    }
}
