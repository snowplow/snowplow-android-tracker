package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration
import com.snowplowanalytics.snowplow.util.Size

class SubjectConfigurationUpdate : SubjectConfiguration() {
    var sourceConfig: SubjectConfiguration? = null

    // userId flag
    var userIdUpdated = false
    fun userId(): String? {
        return if (sourceConfig == null || userIdUpdated) super.userId else sourceConfig!!.userId
    }

    // networkUserId flag
    var networkUserIdUpdated = false
    fun networkUserId(): String? {
        return if (sourceConfig == null || networkUserIdUpdated) super.networkUserId else sourceConfig!!.networkUserId
    }

    // domainUserId flag
    var domainUserIdUpdated = false
    fun domainUserId(): String? {
        return if (sourceConfig == null || domainUserIdUpdated) super.domainUserId else sourceConfig!!.domainUserId
    }

    // useragent flag
    var useragentUpdated = false
    fun useragent(): String? {
        return if (sourceConfig == null || useragentUpdated) super.useragent else sourceConfig!!.useragent
    }

    // ipAddress flag
    var ipAddressUpdated = false
    fun ipAddress(): String? {
        return if (sourceConfig == null || ipAddressUpdated) super.ipAddress else sourceConfig!!.ipAddress
    }

    // timezone flag
    var timezoneUpdated = false
    fun timezone(): String? {
        return if (sourceConfig == null || timezoneUpdated) super.timezone else sourceConfig!!.timezone
    }

    // language flag
    var languageUpdated = false
    fun language(): String? {
        return if (sourceConfig == null || languageUpdated) super.language else sourceConfig!!.language
    }

    // screenResolution flag
    var screenResolutionUpdated = false
    fun screenResolution(): Size? {
        return if (sourceConfig == null || screenResolutionUpdated) super.screenResolution else sourceConfig!!.screenResolution
    }

    // screenViewPort flag
    var screenViewPortUpdated = false
    fun screenViewPort(): Size? {
        return if (sourceConfig == null || screenViewPortUpdated) super.screenViewPort else sourceConfig!!.screenViewPort
    }

    // colorDepth flag
    var colorDepthUpdated = false
    fun colorDepth(): Int? {
        return if (sourceConfig == null || colorDepthUpdated) super.colorDepth else sourceConfig!!.colorDepth
    }
}
