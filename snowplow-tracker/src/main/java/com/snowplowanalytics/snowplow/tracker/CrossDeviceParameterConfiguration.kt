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
package com.snowplowanalytics.snowplow.tracker

import com.snowplowanalytics.snowplow.controller.SessionController
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.controller.SubjectController

/**
 * Configuration object for [TrackerController.decorateLink]
 *
 * Enabled properties will be included when decorating a URI using `decorateLink`
 */
data class CrossDeviceParameterConfiguration(
    /** Whether to include the value of [SessionController.sessionId] when decorating a link (enabled by default) */
    val sessionId: Boolean = true,

    /** Whether to include the value of [SubjectController.userId] when decorating a link */
    val subjectUserId: Boolean = false,

    /** Whether to include the value of [TrackerController.appId] when decorating a link (enabled by default) */
    val sourceId: Boolean = true,

    /** Whether to include the value of [TrackerController.devicePlatform] when  decorating a link */
    val sourcePlatform: Boolean = false,

    /** Optional identifier/information for cross-navigation */
    val reason: String? = null
)
