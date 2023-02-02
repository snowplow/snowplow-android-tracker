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
package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration
import com.snowplowanalytics.snowplow.util.Size

class SubjectConfigurationUpdate : SubjectConfiguration() {
    var sourceConfig: SubjectConfiguration? = null
    private var userIdUpdated = false
    private var networkUserIdUpdated = false
    private var domainUserIdUpdated = false
    private var useragentUpdated = false
    private var ipAddressUpdated = false
    private var timezoneUpdated = false
    private var languageUpdated = false
    private var screenResolutionUpdated = false
    private var screenViewPortUpdated = false
    private var colorDepthUpdated = false

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
