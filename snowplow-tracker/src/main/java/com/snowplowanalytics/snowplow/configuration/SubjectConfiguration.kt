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

import com.snowplowanalytics.core.tracker.SubjectConfigurationInterface
import com.snowplowanalytics.snowplow.util.Size
import org.json.JSONObject

/**
 * The [SubjectConfiguration] can be used to set up the tracker with the basic information about the
 * user and the app. These properties will be added to every event.
 */
open class SubjectConfiguration() : Configuration, SubjectConfigurationInterface {

    override var userId: String? = null
    override var networkUserId: String? = null
    override var domainUserId: String? = null
    override var useragent: String? = null
    override var ipAddress: String? = null
    override var timezone: String? = null
    override var language: String? = null
    override var screenResolution: Size? = null
    override var screenViewPort: Size? = null
    override var colorDepth: Int? = null
    
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
        userId = if (jsonObject.has("userId")) jsonObject.optString("userId") else null
        networkUserId =
            if (jsonObject.has("networkUserId")) jsonObject.optString("networkUserId") else null
        domainUserId =
            if (jsonObject.has("domainUserId")) jsonObject.optString("domainUserId") else null
        useragent = if (jsonObject.has("useragent")) jsonObject.optString("useragent") else null
        ipAddress = if (jsonObject.has("ipAddress")) jsonObject.optString("ipAddress") else null
        timezone = if (jsonObject.has("timezone")) jsonObject.optString("timezone") else null
        language = if (jsonObject.has("language")) jsonObject.optString("language") else null
    }
}
