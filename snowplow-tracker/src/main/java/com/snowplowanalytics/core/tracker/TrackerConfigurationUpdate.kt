package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import org.json.JSONObject

class TrackerConfigurationUpdate : TrackerConfiguration {
    var sourceConfig: TrackerConfiguration? = null
    var isPaused = false
    private var appIdUpdated = false
    private var devicePlatformUpdated = false
    private var base64encodingUpdated = false
    private var logLevelUpdated = false
    private var loggerDelegateUpdated = false
    private var applicationContextUpdated = false
    private var platformContextUpdated = false
    private var geoLocationContextUpdated = false
    private var sessionContextUpdated = false
    private var deepLinkContextUpdated = false
    private var screenContextUpdated = false
    private var screenViewAutotrackingUpdated = false
    private var lifecycleAutotrackingUpdated = false
    private var installAutotrackingUpdated = false
    private var exceptionAutotrackingUpdated = false
    private var diagnosticAutotrackingUpdated = false
    private var userAnonymisationUpdated = false
    private var trackerVersionSuffixUpdated = false

    constructor(appId: String) : super(appId)
    constructor(appId: String, jsonObject: JSONObject) : super(appId, jsonObject)

    override var appId: String
        get() = if (sourceConfig == null || appIdUpdated) super.appId else sourceConfig!!.appId
        set(value) {
            super.appId = value
            appIdUpdated = true
        }
    
    override var devicePlatform: DevicePlatform
        get() = if (sourceConfig == null || devicePlatformUpdated) super.devicePlatform else sourceConfig!!.devicePlatform
        set(value) {
            super.devicePlatform = value
            devicePlatformUpdated = true
        }
    
    override var base64encoding: Boolean
        get() = if (sourceConfig == null || base64encodingUpdated) super.base64encoding else sourceConfig!!.base64encoding
        set(value) {
            super.base64encoding = value
            base64encodingUpdated = true
        }
    
    override var logLevel: LogLevel
        get() = if (sourceConfig == null || logLevelUpdated) super.logLevel else sourceConfig!!.logLevel
        set(value) {
            super.logLevel = value
            logLevelUpdated = true
        }
    
    override var loggerDelegate: LoggerDelegate?
        get() = if (sourceConfig == null || loggerDelegateUpdated) super.loggerDelegate else sourceConfig!!.loggerDelegate
        set(value) {
            super.loggerDelegate = value
            loggerDelegateUpdated = true
        }
    
    override var sessionContext: Boolean
        get() = if (sourceConfig == null || sessionContextUpdated) super.sessionContext else sourceConfig!!.sessionContext
        set(value) {
            super.sessionContext = value
            sessionContextUpdated = true
        }
    
    override var applicationContext: Boolean
        get() = if (sourceConfig == null || applicationContextUpdated) super.applicationContext else sourceConfig!!.applicationContext
        set(value) {
            super.applicationContext = value
            applicationContextUpdated = true
        }
    
    override var platformContext: Boolean
        get() = if (sourceConfig == null || platformContextUpdated) super.platformContext else sourceConfig!!.platformContext
        set(value) {
            super.platformContext = value
            platformContextUpdated = true
        }
    
    override var geoLocationContext: Boolean
        get() = if (sourceConfig == null || geoLocationContextUpdated) super.geoLocationContext else sourceConfig!!.geoLocationContext
        set(value) {
            super.geoLocationContext = value
            geoLocationContextUpdated = true
        }
    
    override var deepLinkContext: Boolean
        get() = if (sourceConfig == null || deepLinkContextUpdated) super.deepLinkContext else sourceConfig!!.deepLinkContext
        set(value) {
            super.deepLinkContext = value
            deepLinkContextUpdated = true
        }
    
    override var screenContext: Boolean
        get() = if (sourceConfig == null || screenContextUpdated) super.screenContext else sourceConfig!!.screenContext
        set(value) {
            super.screenContext = value
            screenContextUpdated = true
        }
    
    override var screenViewAutotracking: Boolean
        get() = if (sourceConfig == null || screenViewAutotrackingUpdated) super.screenViewAutotracking else sourceConfig!!.screenViewAutotracking
        set(value) {
            super.screenViewAutotracking = value
            screenViewAutotrackingUpdated = true
        }
    
    override var lifecycleAutotracking: Boolean
        get() = if (sourceConfig == null || lifecycleAutotrackingUpdated) super.lifecycleAutotracking else sourceConfig!!.lifecycleAutotracking
        set(value) {
            super.lifecycleAutotracking = value
            lifecycleAutotrackingUpdated = true
        }
    
    override var installAutotracking: Boolean
        get() = if (sourceConfig == null || installAutotrackingUpdated) super.installAutotracking else sourceConfig!!.installAutotracking
        set(value) {
            super.installAutotracking = value
            installAutotrackingUpdated = true
        }
    
    override var exceptionAutotracking: Boolean
        get() = if (sourceConfig == null || exceptionAutotrackingUpdated) super.exceptionAutotracking else sourceConfig!!.exceptionAutotracking
        set(value) {
            super.exceptionAutotracking = value
            exceptionAutotrackingUpdated = true
        }
    
    override var diagnosticAutotracking: Boolean
        get() = if (sourceConfig == null || diagnosticAutotrackingUpdated) super.diagnosticAutotracking else sourceConfig!!.diagnosticAutotracking
        set(value) {
            super.diagnosticAutotracking = value
            diagnosticAutotrackingUpdated = true
        }
    
    override var userAnonymisation: Boolean
        get() = if (sourceConfig == null || userAnonymisationUpdated) super.userAnonymisation else sourceConfig!!.userAnonymisation
        set(value) {
            super.userAnonymisation = value
            userAnonymisationUpdated = true
        }
    
    override var trackerVersionSuffix: String?
        get() = if (sourceConfig == null || trackerVersionSuffixUpdated) super.trackerVersionSuffix else sourceConfig!!.trackerVersionSuffix
        set(value) {
            super.trackerVersionSuffix = value
            trackerVersionSuffixUpdated = true
        }
}
