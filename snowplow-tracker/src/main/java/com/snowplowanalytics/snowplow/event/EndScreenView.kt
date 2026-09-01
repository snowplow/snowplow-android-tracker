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

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import java.util.*

/** An event to manually end the currently active screen view.
 *
 * Use this when a screen's `screen_end` won't be triggered automatically, e.g. when a WebView
 * page view is presented on top of a native screen, or when a screen isn't fully instrumented
 * with automatic screen view tracking.
 *
 * @param screenId Identifier of the screen to end. If provided, the screen is ended only if it
 * matches the identifier of the currently active screen view; otherwise the call is ignored.
 * If not provided, the currently active screen view (if any) is ended.
 * */
class EndScreenView @JvmOverloads constructor(val screenId: UUID? = null) : AbstractSelfDescribing() {

    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            screenId?.let { payload[Parameters.SV_ID] = it.toString() }
            return payload
        }

    override val schema: String
        get() = TrackerConstants.SCHEMA_END_SCREEN_VIEW
}
