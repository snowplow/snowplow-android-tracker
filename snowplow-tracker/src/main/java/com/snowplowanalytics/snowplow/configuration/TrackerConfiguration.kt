package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.TrackerConfigurationInterface
import com.snowplowanalytics.core.tracker.TrackerDefaults
import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate
import org.json.JSONObject
import java.util.*

/**
 * This class represents the configuration of the tracker and the core tracker properties.
 * The TrackerConfiguration can be used to setup the tracker behaviour indicating what should be
 * tracked in term of automatic tracking and contexts/entities to track with the events.
 *
 * Default values:
 * devicePlatform = DevicePlatform.Mobile;
 * base64encoding = true;
 * logLevel = LogLevel.OFF;
 * loggerDelegate = null;
 * sessionContext = true;
 * applicationContext = true;
 * platformContext = true;
 * geoLocationContext = false;
 * screenContext = true;
 * deepLinkContext = true;
 * screenViewAutotracking = true;
 * lifecycleAutotracking = false;
 * installAutotracking = true;
 * exceptionAutotracking = true;
 * diagnosticAutotracking = false;
 * userAnonymisation = false;
 * 
 * @param appId Identifier of the app.
*/
open class TrackerConfiguration(
    /**
     * @see .appId
     */
    override var appId: String
) : TrackerConfigurationInterface, Configuration {
    
    /**
     * @see .devicePlatform
     */
    override var devicePlatform: DevicePlatform = TrackerDefaults.devicePlatform

    /**
     * @see .base64encoding
     */
    override var base64encoding: Boolean = TrackerDefaults.base64Encoded

    /**
     * @see .logLevel
     */
    override var logLevel: LogLevel = TrackerDefaults.logLevel

    /**
     * @see .loggerDelegate
     */
    override var loggerDelegate: LoggerDelegate? = null

    /**
     * @see .sessionContext
     */
    override var sessionContext: Boolean = TrackerDefaults.sessionContext

    /**
     * @see .applicationContext
     */
    override var applicationContext: Boolean = TrackerDefaults.applicationContext

    /**
     * @see .platformContext
     */
    override var platformContext: Boolean = TrackerDefaults.platformContext

    /**
     * @see .geoLocationContext
     */
    override var geoLocationContext: Boolean = TrackerDefaults.geoLocationContext

    /**
     * @see .deepLinkContext
     */
    override var deepLinkContext: Boolean = TrackerDefaults.deepLinkContext

    /**
     * @see .screenContext
     */
    override var screenContext: Boolean = TrackerDefaults.screenContext

    /**
     * @see .screenViewAutotracking
     */
    override var screenViewAutotracking: Boolean = TrackerDefaults.screenViewAutotracking

    /**
     * @see .lifecycleAutotracking
     */
    override var lifecycleAutotracking: Boolean = TrackerDefaults.lifecycleAutotracking

    /**
     * @see .installAutotracking
     */
    override var installAutotracking: Boolean = TrackerDefaults.installAutotracking

    /**
     * @see .exceptionAutotracking
     */
    override var exceptionAutotracking: Boolean = TrackerDefaults.exceptionAutotracking

    /**
     * @see .diagnosticAutotracking
     */
    override var diagnosticAutotracking: Boolean = TrackerDefaults.diagnosticAutotracking

    /**
     * @see .userAnonymisation
     */
    override var userAnonymisation: Boolean = TrackerDefaults.userAnonymisation

    /**
     * @see .trackerVersionSuffix
     */
    override var trackerVersionSuffix: String? = null
    

    // Builder methods
    
    /**
     * Identifer of the app.
     */
    fun appId(appId: String): TrackerConfiguration {
        this.appId = appId
        return this
    }

    /**
     * It sets the device platform the tracker is running on.
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
     * It sets the logger delegate that receive logs from the tracker.
     */
    fun loggerDelegate(loggerDelegate: LoggerDelegate?): TrackerConfiguration {
        this.loggerDelegate = loggerDelegate
        return this
    }

    /**
     * Whether application context is sent with all the tracked events.
     */
    fun applicationContext(applicationContext: Boolean): TrackerConfiguration {
        this.applicationContext = applicationContext
        return this
    }

    /**
     * Whether mobile/platform context is sent with all the tracked events.
     */
    fun platformContext(platformContext: Boolean): TrackerConfiguration {
        this.platformContext = platformContext
        return this
    }

    /**
     * Whether geo-location context is sent with all the tracked events.
     *
     * @apiNote Requires Location permissions as per the requirements of the various
     * Android versions. Otherwise the whole context is skipped.
     */
    fun geoLocationContext(geoLocationContext: Boolean): TrackerConfiguration {
        this.geoLocationContext = geoLocationContext
        return this
    }

    /**
     * Whether session context is sent with all the tracked events.
     */
    fun sessionContext(sessionContext: Boolean): TrackerConfiguration {
        this.sessionContext = sessionContext
        return this
    }

    /**
     * Whether deepLink context is sent with all the ScreenView events.
     */
    fun deepLinkContext(deepLinkContext: Boolean): TrackerConfiguration {
        this.deepLinkContext = deepLinkContext
        return this
    }

    /**
     * Whether screen context is sent with all the tracked events.
     */
    fun screenContext(screenContext: Boolean): TrackerConfiguration {
        this.screenContext = screenContext
        return this
    }

    /**
     * Whether enable automatic tracking of ScreenView events.
     */
    fun screenViewAutotracking(screenViewAutotracking: Boolean): TrackerConfiguration {
        this.screenViewAutotracking = screenViewAutotracking
        return this
    }

    /**
     * Whether enable automatic tracking of background and foreground transitions.
     * @apiNote It needs the Foreground library installed.
     */
    fun lifecycleAutotracking(lifecycleAutotracking: Boolean): TrackerConfiguration {
        this.lifecycleAutotracking = lifecycleAutotracking
        return this
    }

    /**
     * Whether enable automatic tracking of install event.
     */
    fun installAutotracking(installAutotracking: Boolean): TrackerConfiguration {
        this.installAutotracking = installAutotracking
        return this
    }

    /**
     * Whether enable crash reporting.
     */
    fun exceptionAutotracking(exceptionAutotracking: Boolean): TrackerConfiguration {
        this.exceptionAutotracking = exceptionAutotracking
        return this
    }

    /**
     * Whether enable diagnostic reporting.
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
     * Decorate the v_tracker field in the tracker protocol.
     * @note Do not use. Internal use only.
     */
    fun trackerVersionSuffix(trackerVersionSuffix: String?): TrackerConfiguration {
        this.trackerVersionSuffix = trackerVersionSuffix
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
