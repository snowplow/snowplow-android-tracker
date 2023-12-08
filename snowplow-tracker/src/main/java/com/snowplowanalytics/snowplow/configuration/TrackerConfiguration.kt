/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.PlatformContext
import com.snowplowanalytics.core.tracker.TrackerConfigurationInterface
import com.snowplowanalytics.core.tracker.TrackerDefaults
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import org.json.JSONObject
import java.util.*

/**
 * The [TrackerConfiguration] can be used to set up the tracker behaviour, including what should be
 * tracked in term of automatic tracking, and entities to track with the events. 
 * Tracker logging can also be configured here.
 *
 * Default values:
 *  - devicePlatform: [DevicePlatform.Mobile]
 *  - base64encoding: true
 *  - logLevel: [LogLevel.OFF]
 *  - loggerDelegate: null
 *  - sessionContext: true
 *  - applicationContext: true
 *  - platformContext: true
 *  - geoLocationContext: false
 *  - screenContext: true
 *  - deepLinkContext: true
 *  - screenViewAutotracking: true
 *  - screenEngagementAutotracking: true
 *  - lifecycleAutotracking: false
 *  - installAutotracking: true
 *  - exceptionAutotracking: true
 *  - diagnosticAutotracking: false
 *  - userAnonymisation: false
*/
open class TrackerConfiguration : TrackerConfigurationInterface, Configuration {

    /**
     * Identifier of the app.
     */
    private var _appId: String? = null
    override var appId: String
        get() = _appId ?: sourceConfig?.appId ?: ""
        set(value) { if (value.isNotEmpty()) { _appId = value } }

    /**
     *  Fallback configuration to read from in case requested values are not present in this configuration.
     */
    var sourceConfig: TrackerConfiguration? = null

    private var _isPaused: Boolean? = null
    internal var isPaused: Boolean
        get() = _isPaused ?: sourceConfig?.isPaused ?: false
        set(value) { _isPaused = value }

    private var _devicePlatform: DevicePlatform? = null
    override var devicePlatform: DevicePlatform
        get() = _devicePlatform ?: sourceConfig?.devicePlatform ?: TrackerDefaults.devicePlatform
        set(value) { _devicePlatform = value }

    private var _base64encoding: Boolean? = null
    override var base64encoding: Boolean
        get() = _base64encoding ?: sourceConfig?.base64encoding ?: TrackerDefaults.base64Encoded
        set(value) { _base64encoding = value }

    private var _logLevel: LogLevel? = null
    override var logLevel: LogLevel
        get() = _logLevel ?: sourceConfig?.logLevel ?: TrackerDefaults.logLevel
        set(value) { _logLevel = value }

    private var _loggerDelegate: LoggerDelegate? = null
    override var loggerDelegate: LoggerDelegate?
        get() = _loggerDelegate ?: sourceConfig?.loggerDelegate
        set(value) { _loggerDelegate = value }

    private var _sessionContext: Boolean? = null
    override var sessionContext: Boolean
        get() = _sessionContext ?: sourceConfig?.sessionContext ?: TrackerDefaults.sessionContext
        set(value) { _sessionContext = value }

    private var _applicationContext: Boolean? = null
    override var applicationContext: Boolean
        get() = _applicationContext ?: sourceConfig?.applicationContext ?: TrackerDefaults.applicationContext
        set(value) { _applicationContext = value }

    private var _platformContext: Boolean? = null
    override var platformContext: Boolean
        get() = _platformContext ?: sourceConfig?.platformContext ?: TrackerDefaults.platformContext
        set(value) { _platformContext = value }

    private var _geoLocationContext: Boolean? = null
    override var geoLocationContext: Boolean
        get() = _geoLocationContext ?: sourceConfig?.geoLocationContext ?: TrackerDefaults.geoLocationContext
        set(value) { _geoLocationContext = value }

    private var _deepLinkContext: Boolean? = null
    override var deepLinkContext: Boolean
        get() = _deepLinkContext ?: sourceConfig?.deepLinkContext ?: TrackerDefaults.deepLinkContext
        set(value) { _deepLinkContext = value }

    private var _screenContext: Boolean? = null
    override var screenContext: Boolean
        get() = _screenContext ?: sourceConfig?.screenContext ?: TrackerDefaults.screenContext
        set(value) { _screenContext = value }

    private var _screenViewAutotracking: Boolean? = null
    override var screenViewAutotracking: Boolean
        get() = _screenViewAutotracking ?: sourceConfig?.screenViewAutotracking ?: TrackerDefaults.screenViewAutotracking
        set(value) { _screenViewAutotracking = value }

    private var _screenEngagementAutotracking: Boolean? = null
    override var screenEngagementAutotracking: Boolean
        get() = _screenEngagementAutotracking ?: sourceConfig?.screenEngagementAutotracking ?: TrackerDefaults.screenEngagementAutotracking
        set(value) { _screenEngagementAutotracking = value }

    private var _lifecycleAutotracking: Boolean? = null
    override var lifecycleAutotracking: Boolean
        get() = _lifecycleAutotracking ?: sourceConfig?.lifecycleAutotracking ?: TrackerDefaults.lifecycleAutotracking
        set(value) { _lifecycleAutotracking = value }

    private var _installAutotracking: Boolean? = null
    override var installAutotracking: Boolean
        get() = _installAutotracking ?: sourceConfig?.installAutotracking ?: TrackerDefaults.installAutotracking
        set(value) { _installAutotracking = value }

    private var _exceptionAutotracking: Boolean? = null
    override var exceptionAutotracking: Boolean
        get() = _exceptionAutotracking ?: sourceConfig?.exceptionAutotracking ?: TrackerDefaults.exceptionAutotracking
        set(value) { _exceptionAutotracking = value }

    private var _diagnosticAutotracking: Boolean? = null
    override var diagnosticAutotracking: Boolean
        get() = _diagnosticAutotracking ?: sourceConfig?.diagnosticAutotracking ?: TrackerDefaults.diagnosticAutotracking
        set(value) { _diagnosticAutotracking = value }

    private var _userAnonymisation: Boolean? = null
    override var userAnonymisation: Boolean
        get() = _userAnonymisation ?: sourceConfig?.userAnonymisation ?: TrackerDefaults.userAnonymisation
        set(value) { _userAnonymisation = value }

    private var _trackerVersionSuffix: String? = null
    override var trackerVersionSuffix: String?
        get() = _trackerVersionSuffix ?: sourceConfig?.trackerVersionSuffix
        set(value) { _trackerVersionSuffix = value }

    private var _platformContextProperties: List<PlatformContextProperty>? = null
    /**
     * List of properties of the platform context to track.
     * If not passed and `platformContext` is enabled, all available properties will be tracked.
     * The required `osType`, `osVersion`, `deviceManufacturer`, and `deviceModel` properties will be tracked in the entity regardless of this setting.
     */
    open var platformContextProperties: List<PlatformContextProperty>?
        get() = _platformContextProperties ?: sourceConfig?.platformContextProperties
        set(value) { _platformContextProperties = value }

    // Builder methods
    
    /**
     * Identifier of the app. This will be included in every event.
     */
    fun appId(appId: String): TrackerConfiguration {
        this.appId = appId
        return this
    }

    /**
     * Specify the device platform the tracker is running on.
     */
    fun devicePlatform(devicePlatform: DevicePlatform): TrackerConfiguration {
        this.devicePlatform = devicePlatform
        return this
    }

    /**
     * It indicates whether the JSON data in the payload should be base64 encoded.
     */
    fun base64encoding(base64encoding: Boolean): TrackerConfiguration {
        this.base64encoding = base64encoding
        return this
    }

    /**
     * It sets the log level of tracker logs.
     */
    fun logLevel(logLevel: LogLevel): TrackerConfiguration {
        this.logLevel = logLevel
        return this
    }

    /**
     * It sets the logger delegate that receive logs from the tracker. Default is STDOUT.
     */
    fun loggerDelegate(loggerDelegate: LoggerDelegate?): TrackerConfiguration {
        this.loggerDelegate = loggerDelegate
        return this
    }

    /**
     * Whether the application context entity should be sent with all the tracked events.
     */
    fun applicationContext(applicationContext: Boolean): TrackerConfiguration {
        this.applicationContext = applicationContext
        return this
    }

    /**
     * Whether the mobile/platform context entity should be sent with all the tracked events.
     */
    fun platformContext(platformContext: Boolean): TrackerConfiguration {
        this.platformContext = platformContext
        return this
    }

    /**
     * Whether the geo-location context entity should be sent with all the tracked events. 
     * The location is based off the last cached location in the device.
     * Requires Location permissions as per the requirements of the various
     * Android versions. Otherwise no entity is added at all.
     */
    fun geoLocationContext(geoLocationContext: Boolean): TrackerConfiguration {
        this.geoLocationContext = geoLocationContext
        return this
    }

    /**
     * Whether the session context entity should be sent with all the tracked events.
     */
    fun sessionContext(sessionContext: Boolean): TrackerConfiguration {
        this.sessionContext = sessionContext
        return this
    }

    /**
     * Whether the [DeepLink](com.snowplowanalytics.snowplow.entity.DeepLink) context entity 
     * should be sent with the first ScreenView event following a deep link.
     */
    fun deepLinkContext(deepLinkContext: Boolean): TrackerConfiguration {
        this.deepLinkContext = deepLinkContext
        return this
    }

    /**
     * Whether the screen context entity should be sent with all the tracked events.
     */
    fun screenContext(screenContext: Boolean): TrackerConfiguration {
        this.screenContext = screenContext
        return this
    }

    /**
     * Whether to enable automatic tracking of ScreenView events. 
     * Note that this automatic tracking relies on the Android 
     * `Application.ActivityLifecycleCallbacks` interface, and therefore only works with Activity-based
     * apps. Composable screens or views in Jetpack Compose apps are not autotracked.
     */
    fun screenViewAutotracking(screenViewAutotracking: Boolean): TrackerConfiguration {
        this.screenViewAutotracking = screenViewAutotracking
        return this
    }

    /**
     * Whether enable tracking the screen end event and the screen summary context entity.
     * Make sure that you have lifecycle autotracking enabled for screen summary to have complete information.
     */
    fun screenEngagementAutotracking(screenEngagementAutotracking: Boolean): TrackerConfiguration {
        this.screenEngagementAutotracking = screenEngagementAutotracking
        return this
    }

    /**
     * Whether to enable automatic tracking of background and foreground transitions. 
     * The Foreground library must be installed.
     */
    fun lifecycleAutotracking(lifecycleAutotracking: Boolean): TrackerConfiguration {
        this.lifecycleAutotracking = lifecycleAutotracking
        return this
    }

    /**
     * Whether to enable automatic tracking of install event.
     * In case com.android.installreferrer:installreferrer library is present,
     * an entity with the referrer details will be attached to the install event.
     */
    fun installAutotracking(installAutotracking: Boolean): TrackerConfiguration {
        this.installAutotracking = installAutotracking
        return this
    }

    /**
     * Whether to enable crash reporting.
     */
    fun exceptionAutotracking(exceptionAutotracking: Boolean): TrackerConfiguration {
        this.exceptionAutotracking = exceptionAutotracking
        return this
    }

    /**
     * Whether to enable diagnostic reporting.
     */
    fun diagnosticAutotracking(diagnosticAutotracking: Boolean): TrackerConfiguration {
        this.diagnosticAutotracking = diagnosticAutotracking
        return this
    }

    /**
     * Whether to anonymise client-side user identifiers in session (userId, previousSessionId), 
     * subject (userId, networkUserId, domainUserId, ipAddress) and platform context entities (IDFA)
     */
    fun userAnonymisation(userAnonymisation: Boolean): TrackerConfiguration {
        this.userAnonymisation = userAnonymisation
        return this
    }

    /**
     * Do not use. Internal use only. 
     * 
     * Decorate the v_tracker field in the tracker protocol.
     * @suppress
     */
    fun trackerVersionSuffix(trackerVersionSuffix: String?): TrackerConfiguration {
        this.trackerVersionSuffix = trackerVersionSuffix
        return this
    }

    /**
     * List of properties of the platform context to track.
     * If not passed and `platformContext` is enabled, all available properties will be tracked.
     * The required `osType`, `osVersion`, `deviceManufacturer`, and `deviceModel` properties will be tracked in the entity regardless of this setting.
     */
    fun platformContextProperties(platformContextProperties: List<PlatformContextProperty>?): TrackerConfiguration {
        this.platformContextProperties = platformContextProperties
        return this
    }

    // Copyable
    override fun copy(): Configuration {
        return TrackerConfiguration(appId)
            .devicePlatform(devicePlatform)
            .base64encoding(base64encoding)
            .logLevel(logLevel)
            .loggerDelegate(loggerDelegate)
            .sessionContext(sessionContext)
            .applicationContext(applicationContext)
            .platformContext(platformContext)
            .geoLocationContext(geoLocationContext)
            .screenContext(screenContext)
            .deepLinkContext(deepLinkContext)
            .screenViewAutotracking(screenViewAutotracking)
            .screenEngagementAutotracking(screenEngagementAutotracking)
            .lifecycleAutotracking(lifecycleAutotracking)
            .installAutotracking(installAutotracking)
            .exceptionAutotracking(exceptionAutotracking)
            .diagnosticAutotracking(diagnosticAutotracking)
            .userAnonymisation(userAnonymisation)
            .trackerVersionSuffix(trackerVersionSuffix)
            .platformContextProperties(platformContextProperties)
    }

    /**
     * @param appId Identifier of the app.
     */
    constructor(appId: String) {
        this._appId = appId
    }

    /**
     * This constructor is only used internally in the service provider
     */
    internal constructor()

    // JSON Formatter
    /**
     * This constructor is used in remote configuration.
     */
    constructor(appId: String, jsonObject: JSONObject) : this(
        jsonObject.optString(
            "appId",
            appId
        )
    ) {
        val value = jsonObject.optString("devicePlatform", DevicePlatform.Mobile.value)
        _devicePlatform = DevicePlatform.getByValue(value)
        if (jsonObject.has("base64encoding")) { _base64encoding = jsonObject.getBoolean("base64encoding") }

        if (jsonObject.has("logLevel")) {
            val log = jsonObject.optString("logLevel", LogLevel.OFF.name)
            try {
                _logLevel = LogLevel.valueOf(log.uppercase(Locale.getDefault()))
            } catch (e: Exception) {
                Logger.e(TAG, "Unable to decode logLevel from remote configuration.")
            }
        }

        if (jsonObject.has("sessionContext")) { _sessionContext = jsonObject.getBoolean("sessionContext") }
        if (jsonObject.has("applicationContext")) { _applicationContext = jsonObject.getBoolean("applicationContext") }
        if (jsonObject.has("platformContext")) { _platformContext = jsonObject.getBoolean("platformContext") }
        if (jsonObject.has("geoLocationContext")) { _geoLocationContext = jsonObject.getBoolean("geoLocationContext") }
        if (jsonObject.has("screenContext")) { _screenContext = jsonObject.getBoolean("screenContext") }
        if (jsonObject.has("deepLinkContext")) { _deepLinkContext = jsonObject.getBoolean("deepLinkContext") }
        if (jsonObject.has("screenViewAutotracking")) { _screenViewAutotracking = jsonObject.getBoolean("screenViewAutotracking") }
        if (jsonObject.has("screenEngagementAutotracking")) { _screenEngagementAutotracking = jsonObject.getBoolean("screenEngagementAutotracking") }
        if (jsonObject.has("lifecycleAutotracking")) { _lifecycleAutotracking = jsonObject.getBoolean("lifecycleAutotracking") }
        if (jsonObject.has("installAutotracking")) { _installAutotracking = jsonObject.getBoolean("installAutotracking") }
        if (jsonObject.has("exceptionAutotracking")) { _exceptionAutotracking = jsonObject.getBoolean("exceptionAutotracking") }
        if (jsonObject.has("diagnosticAutotracking")) { _diagnosticAutotracking = jsonObject.getBoolean("diagnosticAutotracking") }
        if (jsonObject.has("userAnonymisation")) { _userAnonymisation = jsonObject.getBoolean("userAnonymisation") }
    }

    companion object {
        val TAG = TrackerConfiguration::class.java.simpleName
    }
}
