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

import com.snowplowanalytics.snowplow.tracker.DevicePlatform
import com.snowplowanalytics.snowplow.tracker.LogLevel
import com.snowplowanalytics.snowplow.tracker.LoggerDelegate

interface TrackerConfigurationInterface {
    
    /**
     * Identifier of the app.
     */
    var appId: String
    
    /**
     * It sets the device platform the tracker is running on.
     */
    var devicePlatform: DevicePlatform

    /**
     * It indicates whether the JSON data in the payload should be base64 encoded.
     */
    var base64encoding: Boolean

    /**
     * It sets the log level of tracker logs.
     */
    var logLevel: LogLevel

    /**
     * It sets the logger delegate that receive logs from the tracker.
     */
    var loggerDelegate: LoggerDelegate?

    /**
     * Whether application context is sent with all the tracked events.
     */
    var applicationContext: Boolean

    /**
     * Whether mobile/platform context is sent with all the tracked events.
     */
    var platformContext: Boolean

    /**
     * Whether geo-location context is sent with all the tracked events.
     *
     * @apiNote Requires Location permissions as per the requirements of the various
     * Android versions. Otherwise the whole context is skipped.
     */
    var geoLocationContext: Boolean
 
    /**
     * Whether session context is sent with all the tracked events.
     */
    var sessionContext: Boolean

    /**
     * Whether deepLink context is sent with all the ScreenView events.
     */
    var deepLinkContext: Boolean

    /**
     * Whether screen context is sent with all the tracked events.
     */
    var screenContext: Boolean

    /**
     * Whether enable automatic tracking of ScreenView events.
     */
    var screenViewAutotracking: Boolean

    /**
     * Whether enable automatic tracking of background and foreground transitions.
     * @apiNote It needs the Foreground library installed.
     */
    var lifecycleAutotracking: Boolean

    /**
     * Whether to enable automatic tracking of install event.
     * In case com.android.installreferrer:installreferrer library is present,
     * an entity with the referrer details will be attached to the install event.
     */
    var installAutotracking: Boolean

    /**
     * Whether enable crash reporting.
     */
    var exceptionAutotracking: Boolean

    /**
     * Whether enable diagnostic reporting.
     */
    var diagnosticAutotracking: Boolean

    /**
     * Whether to anonymise client-side user identifiers in session (userId, previousSessionId), subject (userId, networkUserId, domainUserId, ipAddress) and platform context entities (IDFA)
     * Setting this property on a running tracker instance starts a new session (if sessions are tracked).
     */
    var userAnonymisation: Boolean

    /**
     * Decorate the v_tracker field in the tracker protocol.
     * @note Do not use. Internal use only.
     */
    var trackerVersionSuffix: String?
}
