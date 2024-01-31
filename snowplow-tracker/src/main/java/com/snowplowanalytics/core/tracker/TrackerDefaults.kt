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

import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import java.util.concurrent.TimeUnit

object TrackerDefaults {
    var base64Encoded = true
    var devicePlatform = DevicePlatform.Mobile
    var logLevel = LogLevel.OFF
    var foregroundTimeout: Long = 1800 // 30 minutes
    var backgroundTimeout: Long = 1800 // 30 minutes
    var timeUnit = TimeUnit.SECONDS
    var sessionContext = true
    var geoLocationContext = false
    var platformContext = true
    var deepLinkContext = true
    var screenContext = true
    var applicationContext = true
    var exceptionAutotracking = true
    var diagnosticAutotracking = false
    var lifecycleAutotracking = true
    var screenViewAutotracking = true
    var screenEngagementAutotracking = true
    var installAutotracking = true
    var userAnonymisation = false
}
