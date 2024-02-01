/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.core.tracker.SubjectConfigurationInterface
import com.snowplowanalytics.snowplow.util.Size
import org.json.JSONObject

/**
 * The [SubjectConfiguration] can be used to set up the tracker with the basic information about the
 * user and the app. These properties will be added to every event.
 */
open class SubjectConfiguration() : Configuration, SubjectConfigurationInterface {

    /**
     * Fallback configuration to read from in case requested values are not present in this configuration.
     */
    internal var sourceConfig: SubjectConfiguration? = null

    private var _userId: String? = null
    override var userId: String?
        get() = _userId ?: sourceConfig?.userId
        set(value) { _userId = value }

    private var _networkUserId: String? = null
    override var networkUserId: String?
        get() = _networkUserId ?: sourceConfig?.networkUserId
        set(value) { _networkUserId = value }

    private var _domainUserId: String? = null
    override var domainUserId: String?
        get() = _domainUserId ?: sourceConfig?.domainUserId
        set(value) { _domainUserId = value }

    private var _useragent: String? = null
    override var useragent: String?
        get() = _useragent ?: sourceConfig?.useragent
        set(value) { _useragent = value }

    private var _ipAddress: String? = null
    override var ipAddress: String?
        get() = _ipAddress ?: sourceConfig?.ipAddress
        set(value) { _ipAddress = value }

    private var _timezone: String? = null
    override var timezone: String?
        get() = _timezone ?: sourceConfig?.timezone
        set(value) { _timezone = value }

    private var _language: String? = null
    override var language: String?
        get() = _language ?: sourceConfig?.language
        set(value) { _language = value }

    private var _screenResolution: Size? = null
    override var screenResolution: Size?
        get() = _screenResolution ?: sourceConfig?.screenResolution
        set(value) { _screenResolution = value }

    private var _screenViewPort: Size? = null
    override var screenViewPort: Size?
        get() = _screenViewPort ?: sourceConfig?.screenViewPort
        set(value) { _screenViewPort = value }

    private var _colorDepth: Int? = null
    override var colorDepth: Int?
        get() = _colorDepth ?: sourceConfig?.colorDepth
        set(value) { _colorDepth = value }
    
    // Builder methods
    
    /**
     * The custom UserID.
     */
    fun userId(userId: String?): SubjectConfiguration {
        this.userId = userId
        return this
    }

    /**
     * Provide a custom network UserID. By default, this field is assigned automatically to the event 
     * during the enrichment phase of the pipeline (i.e., not by the tracker). 
     * The value is the UUID ID of the event collector cookie.
     * This can be prevented by configuring [EmitterConfiguration.serverAnonymisation].
     */
    fun networkUserId(networkUserId: String?): SubjectConfiguration {
        this.networkUserId = networkUserId
        return this
    }

    /**
     * The domain UserID. Not automatically set. 
     * 
     * In client-side tracking, this property is automatically assigned 
     * based on the cookies.
     */
    fun domainUserId(domainUserId: String?): SubjectConfiguration {
        this.domainUserId = domainUserId
        return this
    }

    /**
     * The useragent. By default, this field is assigned automatically to the event
     * during the enrichment phase of the pipeline (i.e., not by the tracker).
     */
    fun useragent(useragent: String?): SubjectConfiguration {
        this.useragent = useragent
        return this
    }

    /**
     * The IP address. By default, this field is assigned automatically to the event
     * during the enrichment phase of the pipeline (i.e., not by the tracker). 
     * This can be prevented by configuring [EmitterConfiguration.serverAnonymisation].
     */
    fun ipAddress(ipAddress: String?): SubjectConfiguration {
        this.ipAddress = ipAddress
        return this
    }

    /**
     * The timezone. By default, this field is assigned automatically to the device timezone.
     */
    fun timezone(timezone: String?): SubjectConfiguration {
        this.timezone = timezone
        return this
    }

    /**
     * The language used for the app. By default, assigned automatically to the device language.
     */
    fun language(language: String?): SubjectConfiguration {
        this.language = language
        return this
    }

    /**
     * The screen resolution. Assigned automatically.
     */
    fun screenResolution(screenResolution: Size?): SubjectConfiguration {
        this.screenResolution = screenResolution
        return this
    }

    /**
     * The screen viewport. Not automatically assigned.
     */
    fun screenViewPort(screenViewPort: Size?): SubjectConfiguration {
        this.screenViewPort = screenViewPort
        return this
    }

    /**
     * The color depth. Not automatically assigned.
     */
    fun colorDepth(colorDepth: Int?): SubjectConfiguration {
        this.colorDepth = colorDepth
        return this
    }

    // Copyable
    override fun copy(): SubjectConfiguration {
        return SubjectConfiguration()
            .userId(userId)
            .networkUserId(networkUserId)
            .domainUserId(domainUserId)
            .useragent(useragent)
            .ipAddress(ipAddress)
            .timezone(timezone)
            .language(language)
            .screenResolution(screenResolution)
            .screenViewPort(screenViewPort)
            .colorDepth(colorDepth)
    }

    // JSON Formatter
    /**
     * This constructor is used in remote configuration.
     */
    constructor(jsonObject: JSONObject) : this() {
        if (jsonObject.has("userId")) { _userId = jsonObject.optString("userId") }
        if (jsonObject.has("networkUserId")) { _networkUserId = jsonObject.optString("networkUserId") }
        if (jsonObject.has("domainUserId")) { _domainUserId = jsonObject.optString("domainUserId") }
        if (jsonObject.has("useragent")) { _useragent = jsonObject.optString("useragent") }
        if (jsonObject.has("ipAddress")) { _ipAddress = jsonObject.optString("ipAddress") }
        if (jsonObject.has("timezone")) { _timezone = jsonObject.optString("timezone") }
        if (jsonObject.has("language")) { _language = jsonObject.optString("language") }
    }
}
