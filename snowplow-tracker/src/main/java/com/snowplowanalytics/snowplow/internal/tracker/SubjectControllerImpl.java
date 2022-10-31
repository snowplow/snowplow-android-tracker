package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.SubjectController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.util.Size;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SubjectControllerImpl extends Controller implements SubjectController {

    // Constructors

    public SubjectControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    // Getters and Setters

    @Nullable
    @Override
    public String getUserId() {
        return getSubject().userId;
    }

    @Override
    public void setUserId(@Nullable String userId) {
        getDirtyConfig().userId = userId;
        getDirtyConfig().userIdUpdated = true;
        getSubject().setUserId(userId);
    }

    @Nullable
    @Override
    public String getNetworkUserId() {
        return getSubject().networkUserId;
    }

    @Override
    public void setNetworkUserId(@Nullable String networkUserId) {
        getDirtyConfig().networkUserId = networkUserId;
        getDirtyConfig().networkUserIdUpdated = true;
        getSubject().setNetworkUserId(networkUserId);
    }

    @Nullable
    @Override
    public String getDomainUserId() {
        return getSubject().domainUserId;
    }

    @Override
    public void setDomainUserId(@Nullable String domainUserId) {
        getDirtyConfig().domainUserId = domainUserId;
        getDirtyConfig().domainUserIdUpdated = true;
        getSubject().setDomainUserId(domainUserId);
    }

    @Nullable
    @Override
    public String getUseragent() {
        return getSubject().useragent;
    }

    @Override
    public void setUseragent(@Nullable String useragent) {
        getDirtyConfig().useragent = useragent;
        getDirtyConfig().useragentUpdated = true;
        getSubject().setUseragent(useragent);
    }

    @Nullable
    @Override
    public String getIpAddress() {
        return getSubject().ipAddress;
    }

    @Override
    public void setIpAddress(@Nullable String ipAddress) {
        getDirtyConfig().ipAddress = ipAddress;
        getDirtyConfig().ipAddressUpdated = true;
        getSubject().setIpAddress(ipAddress);
    }

    @Nullable
    @Override
    public String getTimezone() {
        return getSubject().timezone;
    }

    @Override
    public void setTimezone(@Nullable String timezone) {
        getDirtyConfig().timezone = timezone;
        getDirtyConfig().timezoneUpdated = true;
        getSubject().setTimezone(timezone);
    }

    @Nullable
    @Override
    public String getLanguage() {
        return getSubject().language;
    }

    @Override
    public void setLanguage(@Nullable String language) {
        getDirtyConfig().language = language;
        getDirtyConfig().languageUpdated = true;
        getSubject().setLanguage(language);
    }

    @Nullable
    @Override
    public Size getScreenResolution() {
        return getSubject().screenResolution;
    }

    @Override
    public void setScreenResolution(@Nullable Size screenResolution) {
        getDirtyConfig().screenResolution = screenResolution;
        getDirtyConfig().screenResolutionUpdated = true;
        getSubject().setScreenResolution(screenResolution.getWidth(), screenResolution.getHeight());
    }

    @Nullable
    @Override
    public Size getScreenViewPort() {
        return getSubject().screenViewPort;
    }

    @Override
    public void setScreenViewPort(@Nullable Size screenViewPort) {
        getDirtyConfig().screenViewPort = screenViewPort;
        getDirtyConfig().screenViewPortUpdated = true;
        getSubject().setViewPort(screenViewPort.getWidth(), screenViewPort.getHeight());
    }

    @Nullable
    @Override
    public Integer getColorDepth() {
        return getSubject().colorDepth;
    }

    @Override
    public void setColorDepth(@Nullable Integer colorDepth) {
        getDirtyConfig().colorDepth = colorDepth;
        getDirtyConfig().colorDepthUpdated = true;
        getSubject().setColorDepth(colorDepth);
    }

    // Private methods

    private Subject getSubject() {
        return serviceProvider.getOrMakeSubject();
    }

    private SubjectConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getSubjectConfigurationUpdate();
    }
}
