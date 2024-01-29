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

import com.snowplowanalytics.snowplow.util.Size

interface SubjectConfigurationInterface {
    var userId: String?
    var networkUserId: String?
    var domainUserId: String?
    var useragent: String?
    var ipAddress: String?
    var timezone: String?
    var language: String?
    var screenResolution: Size?
    var screenViewPort: Size?
    var colorDepth: Int?
}
