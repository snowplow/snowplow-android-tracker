package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.SubjectController;
import com.snowplowanalytics.snowplow.util.Size;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SubjectControllerImpl implements SubjectController {

    @NonNull
    private Subject subject;

    // Constructors

    public SubjectControllerImpl(@NonNull Subject subject) {
        this.subject = subject;
    }

    // Getters and Setters

    @Nullable
    @Override
    public String getUserId() {
        return subject.userId;
    }

    @Override
    public void setUserId(@Nullable String userId) {
        subject.setUserId(userId);
    }

    @Nullable
    @Override
    public String getNetworkUserId() {
        return subject.networkUserId;
    }

    @Override
    public void setNetworkUserId(@Nullable String networkUserId) {
        subject.setNetworkUserId(networkUserId);
    }

    @Nullable
    @Override
    public String getDomainUserId() {
        return subject.domainUserId;
    }

    @Override
    public void setDomainUserId(@Nullable String domainUserId) {
        subject.setDomainUserId(domainUserId);
    }

    @Nullable
    @Override
    public String getUseragent() {
        return subject.useragent;
    }

    @Override
    public void setUseragent(@Nullable String useragent) {
        subject.setUseragent(useragent);
    }

    @Nullable
    @Override
    public String getIpAddress() {
        return subject.ipAddress;
    }

    @Override
    public void setIpAddress(@Nullable String ipAddress) {
        subject.setIpAddress(ipAddress);
    }

    @Nullable
    @Override
    public String getTimezone() {
        return subject.timezone;
    }

    @Override
    public void setTimezone(@Nullable String timezone) {
        subject.setTimezone(timezone);
    }

    @Nullable
    @Override
    public String getLanguage() {
        return subject.language;
    }

    @Override
    public void setLanguage(@Nullable String language) {
        subject.setLanguage(language);
    }

    @Nullable
    @Override
    public Size getScreenResolution() {
        return subject.screenResolution;
    }

    @Override
    public void setScreenResolution(@Nullable Size screenResolution) {
        subject.setScreenResolution(screenResolution.getWidth(), screenResolution.getHeight());
    }

    @Nullable
    @Override
    public Size getScreenViewPort() {
        return subject.screenViewPort;
    }

    @Override
    public void setScreenViewPort(@Nullable Size screenViewPort) {
        subject.setViewPort(screenViewPort.getWidth(), screenViewPort.getHeight());
    }

    @Nullable
    @Override
    public Integer getColorDepth() {
        return subject.colorDepth;
    }

    @Override
    public void setColorDepth(@Nullable Integer colorDepth) {
        subject.setColorDepth(colorDepth);
    }
}
