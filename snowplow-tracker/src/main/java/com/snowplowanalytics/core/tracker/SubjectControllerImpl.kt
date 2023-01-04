package com.snowplowanalytics.core.tracker

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.snowplow.controller.SubjectController
import com.snowplowanalytics.snowplow.util.Size

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SubjectControllerImpl  // Constructors
    (serviceProvider: ServiceProviderInterface) : Controller(serviceProvider), SubjectController {
    // Getters and Setters
    override var userId: String?
        get() = subject.userId
        set(userId) {
            dirtyConfig.userId = userId
            dirtyConfig.userIdUpdated = true
            subject.setUserId(userId!!)
        }
    
    override var networkUserId: String?
        get() = subject.networkUserId
        set(networkUserId) {
            dirtyConfig.networkUserId = networkUserId
            dirtyConfig.networkUserIdUpdated = true
            subject.setNetworkUserId(networkUserId!!)
        }
    
    override var domainUserId: String?
        get() = subject.domainUserId
        set(domainUserId) {
            dirtyConfig.domainUserId = domainUserId
            dirtyConfig.domainUserIdUpdated = true
            subject.setDomainUserId(domainUserId!!)
        }
    
    override var useragent: String?
        get() = subject.useragent
        set(useragent) {
            dirtyConfig.useragent = useragent
            dirtyConfig.useragentUpdated = true
            subject.setUseragent(useragent!!)
        }
    
    override var ipAddress: String?
        get() = subject.ipAddress
        set(ipAddress) {
            dirtyConfig.ipAddress = ipAddress
            dirtyConfig.ipAddressUpdated = true
            subject.setIpAddress(ipAddress!!)
        }
    
    override var timezone: String?
        get() = subject.timezone
        set(timezone) {
            dirtyConfig.timezone = timezone
            dirtyConfig.timezoneUpdated = true
            subject.setTimezone(timezone!!)
        }
    
    override var language: String?
        get() = subject.language
        set(language) {
            dirtyConfig.language = language
            dirtyConfig.languageUpdated = true
            subject.setLanguage(language!!)
        }
    
    override var screenResolution: Size?
        get() = subject.screenResolution
        set(screenResolution) {
            dirtyConfig.screenResolution = screenResolution
            dirtyConfig.screenResolutionUpdated = true
            subject.setScreenResolution(screenResolution!!.width, screenResolution.height)
        }
    
    override var screenViewPort: Size?
        get() = subject.screenViewPort
        set(screenViewPort) {
            dirtyConfig.screenViewPort = screenViewPort
            dirtyConfig.screenViewPortUpdated = true
            subject.setViewPort(screenViewPort!!.width, screenViewPort.height)
        }
    
    override var colorDepth: Int?
        get() = subject.colorDepth
        set(colorDepth) {
            dirtyConfig.colorDepth = colorDepth
            dirtyConfig.colorDepthUpdated = true
            subject.setColorDepth(colorDepth!!)
        }

    // Private methods
    private val subject: Subject
        get() = serviceProvider.orMakeSubject()
    private val dirtyConfig: SubjectConfigurationUpdate
        get() = serviceProvider.subjectConfigurationUpdate
}
