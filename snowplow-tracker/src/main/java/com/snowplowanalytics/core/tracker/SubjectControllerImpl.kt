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
package com.snowplowanalytics.core.tracker

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration
import com.snowplowanalytics.snowplow.controller.SubjectController
import com.snowplowanalytics.snowplow.util.Size

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SubjectControllerImpl  // Constructors
    (serviceProvider: ServiceProviderInterface) : Controller(serviceProvider), SubjectController {
    // Getters and Setters
    override var userId: String?
        get() = subject.userId
        set(userId) {
            dirtyConfig.userId = userId
            subject.userId = userId
        }
    
    override var networkUserId: String?
        get() = subject.networkUserId
        set(networkUserId) {
            dirtyConfig.networkUserId = networkUserId
            subject.networkUserId = networkUserId
        }
    
    override var domainUserId: String?
        get() = subject.domainUserId
        set(domainUserId) {
            dirtyConfig.domainUserId = domainUserId
            subject.domainUserId = domainUserId
        }
    
    override var useragent: String?
        get() = subject.useragent
        set(useragent) {
            dirtyConfig.useragent = useragent
            subject.useragent = useragent
        }
    
    override var ipAddress: String?
        get() = subject.ipAddress
        set(ipAddress) {
            dirtyConfig.ipAddress = ipAddress
            subject.ipAddress = ipAddress
        }
    
    override var timezone: String?
        get() = subject.timezone
        set(timezone) {
            dirtyConfig.timezone = timezone
            subject.timezone = timezone
        }
    
    override var language: String?
        get() = subject.language
        set(language) {
            dirtyConfig.language = language
            subject.language = language
        }
    
    override var screenResolution: Size?
        get() = subject.screenResolution
        set(screenResolution) {
            dirtyConfig.screenResolution = screenResolution
            subject.screenResolution = screenResolution
        }
    
    override var screenViewPort: Size?
        get() = subject.screenViewPort
        set(screenViewPort) {
            dirtyConfig.screenViewPort = screenViewPort
            subject.screenViewPort = screenViewPort
        }
    
    override var colorDepth: Int?
        get() = subject.colorDepth
        set(colorDepth) {
            dirtyConfig.colorDepth = colorDepth
            subject.colorDepth = colorDepth
        }

    // Private methods
    private val subject: Subject
        get() = serviceProvider.getOrMakeSubject()
    private val dirtyConfig: SubjectConfiguration
        get() = serviceProvider.subjectConfiguration
}
