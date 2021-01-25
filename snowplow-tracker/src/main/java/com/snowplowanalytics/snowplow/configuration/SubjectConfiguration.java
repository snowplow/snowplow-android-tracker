package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate;
import com.snowplowanalytics.snowplow.tracker.utils.LogLevel;
import com.snowplowanalytics.snowplow.util.Size;

import java.util.Objects;

public class SubjectConfiguration implements Configuration {

    @Nullable
    public String userId;
    @Nullable
    public String networkUserId;
    @Nullable
    public String domainUserId;
    @Nullable
    public String useragent;
    @Nullable
    public String ipAddress;

    @Nullable
    public String timezone;
    @Nullable
    public String language;

    @Nullable
    public Size screenResolution;
    @Nullable
    public Size screenViewPort;
    @Nullable
    public Integer colorDepth;

    // Builder methods

    @NonNull
    public SubjectConfiguration userId(@Nullable String userId) {
        this.userId = userId;
        return this;
    }

    @NonNull
    public SubjectConfiguration networkUserId(@Nullable String networkUserId) {
        this.networkUserId = networkUserId;
        return this;
    }

    @NonNull
    public SubjectConfiguration domainUserId(@Nullable String domainUserId) {
        this.domainUserId = domainUserId;
        return this;
    }

    @NonNull
    public SubjectConfiguration useragent(@Nullable String useragent) {
        this.useragent = useragent;
        return this;
    }

    @NonNull
    public SubjectConfiguration ipAddress(@Nullable String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    @NonNull
    public SubjectConfiguration timezone(@Nullable String timezone) {
        this.timezone = timezone;
        return this;
    }

    @NonNull
    public SubjectConfiguration language(@Nullable String language) {
        this.language = language;
        return this;
    }

    @NonNull
    public SubjectConfiguration screenResolution(@Nullable Size screenResolution) {
        this.screenResolution = screenResolution;
        return this;
    }

    @NonNull
    public SubjectConfiguration screenViewPort(@Nullable Size screenViewPort) {
        this.screenViewPort = screenViewPort;
        return this;
    }

    @NonNull
    public SubjectConfiguration colorDepth(@Nullable Integer colorDepth) {
        this.colorDepth = colorDepth;
        return this;
    }

    // Copyable

    @Override
    @NonNull
    public SubjectConfiguration copy() {
        SubjectConfiguration copy = new SubjectConfiguration();
        copy.userId = userId;
        copy.networkUserId = networkUserId;
        copy.domainUserId = domainUserId;
        copy.useragent = useragent;
        copy.ipAddress = ipAddress;
        copy.timezone = timezone;
        copy.language = language;
        copy.screenResolution = screenResolution;
        copy.screenViewPort = screenViewPort;
        copy.colorDepth = colorDepth;
        return copy;
    }
}
