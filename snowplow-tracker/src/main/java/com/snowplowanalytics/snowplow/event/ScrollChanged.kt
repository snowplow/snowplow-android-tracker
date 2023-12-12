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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.TrackerConstants

/**
 * Event tracked when a scroll view's scroll position changes.
 * If screen engagement tracking is enabled, the scroll changed events will be aggregated into a `screen_summary` entity.
 *
 * Schema: `iglu:com.snowplowanalytics.mobile/scroll_changed/jsonschema/1-0-0`
 */
class ScrollChanged (
    /** Vertical scroll offset in pixels. */
    var yOffset: Int,
    /** The height of the scroll view content in pixels. */
    var contentHeight: Int
) : AbstractSelfDescribing() {

    override val schema: String
        get() = TrackerConstants.SCHEMA_SCROLL_CHANGED

    override val dataPayload: Map<String, Any?>
        get() {
            return mapOf(
                "y_offset" to yOffset,
                "content_height" to contentHeight
            )
        }
}
