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
 *  - lifecycleAutotracking: false
 *  - installAutotracking: true
 *  - exceptionAutotracking: true
 *  - diagnosticAutotracking: false
 *  - userAnonymisation: false
 * 
 * @param appId Identifier of the app.
*/
open class TrackerConfiguration(
    override var appId: String
) : TrackerConfigurationInterface, Configuration {
    
    override var devicePlatform: DevicePlatform = TrackerDefaults.devicePlatform
    override var base64encoding: Boolean = TrackerDefaults.base64Encoded
    override var logLevel: LogLevel = TrackerDefaults.logLevel
    override var loggerDelegate: LoggerDelegate? = null
    override var sessionContext: Boolean = TrackerDefaults.sessionContext
    override var applicationContext: Boolean = TrackerDefaults.applicationContext
    override var platformContext: Boolean = TrackerDefaults.platformContext
    override var geoLocationContext: Boolean = TrackerDefaults.geoLocationContext
    override var deepLinkContext: Boolean = TrackerDefaults.deepLinkContext
    override var screenContext: Boolean = TrackerDefaults.screenContext
    override var screenViewAutotracking: Boolean = TrackerDefaults.screenViewAutotracking
    override var lifecycleAutotracking: Boolean = TrackerDefaults.lifecycleAutotracking
    override var installAutotracking: Boolean = TrackerDefaults.installAutotracking
    override var exceptionAutotracking: Boolean = TrackerDefaults.exceptionAutotracking
    override var diagnosticAutotracking: Boolean = TrackerDefaults.diagnosticAutotracking
    override var userAnonymisation: Boolean = TrackerDefaults.userAnonymisation
    override var trackerVersionSuffix: String? = null

    /**
     * List of properties of the platform context to track.
     * If not passed and `platformContext` is enabled, all available properties will be tracked.
     * The required `osType`, `osVersion`, `deviceManufacturer`, and `deviceModel` properties will be tracked in the entity regardless of this setting.
     */
    open var platformContextProperties: List<PlatformContextProperty>? = null

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
     * Whether to enable automatic tracking of background and foreground transitions. 
     * The Foreground library must be installed.
     */
    fun lifecycleAutotracking(lifecycleAutotracking: Boolean): TrackerConfiguration {
        this.lifecycleAutotracking = lifecycleAutotracking
        return this
    }

    /**
     * Whether to enable automatic tracking of install event.
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
            .lifecycleAutotracking(lifecycleAutotracking)
            .installAutotracking(installAutotracking)
            .exceptionAutotracking(exceptionAutotracking)
            .diagnosticAutotracking(diagnosticAutotracking)
            .userAnonymisation(userAnonymisation)
            .trackerVersionSuffix(trackerVersionSuffix)
    }

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
        devicePlatform = DevicePlatform.getByValue(value)
        base64encoding = jsonObject.optBoolean("base64encoding", base64encoding)
        
        val log = jsonObject.optString("logLevel", LogLevel.OFF.name)
        try {
            logLevel = LogLevel.valueOf(log.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            Logger.e(TAG, "Unable to decode logLevel from remote configuration.")
        }
        
        sessionContext = jsonObject.optBoolean("sessionContext", sessionContext)
        applicationContext = jsonObject.optBoolean("applicationContext", applicationContext)
        platformContext = jsonObject.optBoolean("platformContext", platformContext)
        geoLocationContext = jsonObject.optBoolean("geoLocationContext", geoLocationContext)
        screenContext = jsonObject.optBoolean("screenContext", screenContext)
        deepLinkContext = jsonObject.optBoolean("deepLinkContext", deepLinkContext)
        screenViewAutotracking =
            jsonObject.optBoolean("screenViewAutotracking", screenViewAutotracking)
        lifecycleAutotracking =
            jsonObject.optBoolean("lifecycleAutotracking", lifecycleAutotracking)
        installAutotracking = jsonObject.optBoolean("installAutotracking", installAutotracking)
        exceptionAutotracking =
            jsonObject.optBoolean("exceptionAutotracking", exceptionAutotracking)
        diagnosticAutotracking =
            jsonObject.optBoolean("diagnosticAutotracking", diagnosticAutotracking)
        userAnonymisation = jsonObject.optBoolean("userAnonymisation", userAnonymisation)
    }

    companion object {
        val TAG = TrackerConfiguration::class.java.simpleName
    }
}
