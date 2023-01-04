package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import org.json.JSONObject

class TrackerConfigurationUpdate : TrackerConfiguration {
    var sourceConfig: TrackerConfiguration? = null
    var isPaused = false

    constructor(appId: String) : super(appId)
    constructor(appId: String, jsonObject: JSONObject) : super(appId, jsonObject)

    // appId flag
    var appIdUpdated = false
    fun appId(): String {
        return if (sourceConfig == null || appIdUpdated) super.appId else sourceConfig!!.appId
    }

    // devicePlatform flag
    var devicePlatformUpdated = false
    fun devicePlatform(): DevicePlatform {
        return if (sourceConfig == null || devicePlatformUpdated) super.devicePlatform else sourceConfig!!.devicePlatform
    }

    // base64encoding flag
    var base64encodingUpdated = false
    fun base64encoding(): Boolean {
        return if (sourceConfig == null || base64encodingUpdated) super.base64encoding else sourceConfig!!.base64encoding
    }

    // logLevel flag
    var logLevelUpdated = false
    fun logLevel(): LogLevel {
        return if (sourceConfig == null || logLevelUpdated) super.logLevel else sourceConfig!!.logLevel
    }

    // loggerDelegate flag
    var loggerDelegateUpdated = false
    fun loggerDelegate(): LoggerDelegate? {
        return if (sourceConfig == null || loggerDelegateUpdated) super.loggerDelegate else sourceConfig!!.loggerDelegate
    }

    // applicationContext flag
    var applicationContextUpdated = false
    fun applicationContext(): Boolean {
        return if (sourceConfig == null || applicationContextUpdated) super.applicationContext else sourceConfig!!.applicationContext
    }

    // platformContext flag
    var platformContextUpdated = false
    fun platformContext(): Boolean {
        return if (sourceConfig == null || platformContextUpdated) super.platformContext else sourceConfig!!.platformContext
    }

    // geoLocationContext flag
    var geoLocationContextUpdated = false
    fun geoLocationContext(): Boolean {
        return if (sourceConfig == null || geoLocationContextUpdated) super.geoLocationContext else sourceConfig!!.geoLocationContext
    }

    // sessionContext flag
    var sessionContextUpdated = false
    fun sessionContext(): Boolean {
        return if (sourceConfig == null || sessionContextUpdated) super.sessionContext else sourceConfig!!.sessionContext
    }

    // deepLinkContext flag
    var deepLinkContextUpdated = false
    fun deepLinkContext(): Boolean {
        return if (sourceConfig == null || deepLinkContextUpdated) super.deepLinkContext else sourceConfig!!.deepLinkContext
    }

    // screenContext flag
    var screenContextUpdated = false
    fun screenContext(): Boolean {
        return if (sourceConfig == null || screenContextUpdated) super.screenContext else sourceConfig!!.screenContext
    }

    // screenViewAutotracking flag
    var screenViewAutotrackingUpdated = false
    fun screenViewAutotracking(): Boolean {
        return if (sourceConfig == null || screenViewAutotrackingUpdated) super.screenViewAutotracking else sourceConfig!!.screenViewAutotracking
    }

    // lifecycleAutotracking flag
    var lifecycleAutotrackingUpdated = false
    fun lifecycleAutotracking(): Boolean {
        return if (sourceConfig == null || lifecycleAutotrackingUpdated) super.lifecycleAutotracking else sourceConfig!!.lifecycleAutotracking
    }

    // installAutotracking flag
    var installAutotrackingUpdated = false
    fun installAutotracking(): Boolean {
        return if (sourceConfig == null || installAutotrackingUpdated) super.installAutotracking else sourceConfig!!.installAutotracking
    }

    // exceptionAutotracking flag
    var exceptionAutotrackingUpdated = false
    fun exceptionAutotracking(): Boolean {
        return if (sourceConfig == null || exceptionAutotrackingUpdated) super.exceptionAutotracking else sourceConfig!!.exceptionAutotracking
    }

    // diagnosticAutotracking flag
    var diagnosticAutotrackingUpdated = false
    fun diagnosticAutotracking(): Boolean {
        return if (sourceConfig == null || diagnosticAutotrackingUpdated) super.diagnosticAutotracking else sourceConfig!!.diagnosticAutotracking
    }

    // userAnonymisation flag
    var userAnonymisationUpdated = false
    fun userAnonymisation(): Boolean {
        return if (sourceConfig == null || userAnonymisationUpdated) super.userAnonymisation else sourceConfig!!.userAnonymisation
    }

    // trackerVersionSuffix flag
    var trackerVersionSuffixUpdated = false
    fun trackerVersionSuffix(): String? {
        return if (sourceConfig == null || trackerVersionSuffixUpdated) super.trackerVersionSuffix else sourceConfig!!.trackerVersionSuffix
    }
}
