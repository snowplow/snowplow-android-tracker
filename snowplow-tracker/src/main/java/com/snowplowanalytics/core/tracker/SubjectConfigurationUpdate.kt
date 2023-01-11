package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration
import com.snowplowanalytics.snowplow.util.Size

class SubjectConfigurationUpdate : SubjectConfiguration() {
    var sourceConfig: SubjectConfiguration? = null
    var userIdUpdated = false
    var networkUserIdUpdated = false
    var domainUserIdUpdated = false
    var useragentUpdated = false
    var ipAddressUpdated = false
    var timezoneUpdated = false
    var languageUpdated = false
    var screenResolutionUpdated = false
    var screenViewPortUpdated = false
    var colorDepthUpdated = false

    override var userId: String?
        get() = if (sourceConfig == null || userIdUpdated) super.userId else sourceConfig!!.userId
        set(value) {
            super.userId = value
            userIdUpdated = true
        }
    
    override var networkUserId: String?
        get() = if (sourceConfig == null || networkUserIdUpdated) super.networkUserId else sourceConfig!!.networkUserId
        set(value) {
            super.networkUserId = value
            networkUserIdUpdated = true
        }
    
    override var domainUserId: String?
        get() = if (sourceConfig == null || domainUserIdUpdated) super.domainUserId else sourceConfig!!.domainUserId
        set(value) {
            super.domainUserId = value
            domainUserIdUpdated = true
        }
    
    override var useragent: String?
        get() = if (sourceConfig == null || useragentUpdated) super.useragent else sourceConfig!!.useragent
        set(value) {
            super.useragent = value
            useragentUpdated = true
        }
    
    override var ipAddress: String?
        get() = if (sourceConfig == null || ipAddressUpdated) super.ipAddress else sourceConfig!!.ipAddress
        set(value) {
            super.ipAddress = value
            ipAddressUpdated = true
        }
    
    override var timezone: String?
        get() = if (sourceConfig == null || timezoneUpdated) super.timezone else sourceConfig!!.timezone
        set(value) {
            super.timezone = value
            timezoneUpdated = true
        }
    
    override var language: String?
        get() = if (sourceConfig == null || languageUpdated) super.language else sourceConfig!!.language
        set(value) {
            super.language = value
            languageUpdated = true
        }
    
    override var screenResolution: Size?
        get() = if (sourceConfig == null || screenResolutionUpdated) super.screenResolution else sourceConfig!!.screenResolution
        set(value) {
            super.screenResolution = value
            screenResolutionUpdated = true
        }
    
    override var screenViewPort: Size?
        get() = if (sourceConfig == null || screenViewPortUpdated) super.screenViewPort else sourceConfig!!.screenViewPort
        set(value) {
            super.screenViewPort = value
            screenViewPortUpdated = true
        }
    
    override var colorDepth: Int?
        get() = if (sourceConfig == null || colorDepthUpdated) super.colorDepth else sourceConfig!!.colorDepth
        set(value) {
            super.colorDepth = value
            colorDepthUpdated = true
        }
}
