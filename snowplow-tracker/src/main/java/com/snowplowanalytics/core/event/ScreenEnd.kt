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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.TrackerConstants
import java.util.*

/**
 * Event tracked when a screen is no longer visible to the user.
 *
 * It is tracked automatically before each [ScreenView] event when screen engagement tracking is
 * enabled, closing out the engagement summary for the previous screen. It can also be tracked
 * manually to end the current screen without starting a new one.
 *
 * After the screen ends, the screen and screen summary entities are no longer attached to
 * subsequent events until the next [ScreenView]. The next [ScreenView] still references the ended
 * screen in its previous screen fields and no further [ScreenEnd] is tracked for it.
 *
 * The event is not tracked if there is no current screen to end.
 */
class ScreenEnd : AbstractSelfDescribing() {

    // Tracker methods
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            return payload
        }

    override val schema: String
        get() = TrackerConstants.SCHEMA_SCREEN_END

}
