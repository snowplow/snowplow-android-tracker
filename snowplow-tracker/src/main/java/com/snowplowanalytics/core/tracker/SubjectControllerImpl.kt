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
            subject.userId = userId
        }
    
    override var networkUserId: String?
        get() = subject.networkUserId
        set(networkUserId) {
            dirtyConfig.networkUserId = networkUserId
            subject.networkUserId = networkUserId
        }
    
    override var domainUserId: String?
        get() = subject.domainUserId
        set(domainUserId) {
            dirtyConfig.domainUserId = domainUserId
            subject.domainUserId = domainUserId
        }
    
    override var useragent: String?
        get() = subject.useragent
        set(useragent) {
            dirtyConfig.useragent = useragent
            subject.useragent = useragent
        }
    
    override var ipAddress: String?
        get() = subject.ipAddress
        set(ipAddress) {
            dirtyConfig.ipAddress = ipAddress
            subject.ipAddress = ipAddress
        }
    
    override var timezone: String?
        get() = subject.timezone
        set(timezone) {
            dirtyConfig.timezone = timezone
            subject.timezone = timezone
        }
    
    override var language: String?
        get() = subject.language
        set(language) {
            dirtyConfig.language = language
            subject.language = language
        }
    
    override var screenResolution: Size?
        get() = subject.screenResolution
        set(screenResolution) {
            dirtyConfig.screenResolution = screenResolution
            subject.screenResolution = screenResolution
        }
    
    override var screenViewPort: Size?
        get() = subject.screenViewPort
        set(screenViewPort) {
            dirtyConfig.screenViewPort = screenViewPort
            subject.screenViewPort = screenViewPort
        }
    
    override var colorDepth: Int?
        get() = subject.colorDepth
        set(colorDepth) {
            dirtyConfig.colorDepth = colorDepth
            subject.colorDepth = colorDepth
        }

    // Private methods
    private val subject: Subject
        get() = serviceProvider.orMakeSubject()
    private val dirtyConfig: SubjectConfigurationUpdate
        get() = serviceProvider.subjectConfigurationUpdate
}
