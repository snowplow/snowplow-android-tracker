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
    var yOffset: Int? = null,
    /** Horizontal scroll offset in pixels. */
    var xOffset: Int? = null,
    /** The width of the scroll view in pixels. */
    var viewWidth: Int? = null,
    /** The height of the scroll view in pixels. */
    var viewHeight: Int? = null,
    /** The width of the content of the scroll view in pixels. */
    var contentWidth: Int? = null,
    /** The height of the content of the scroll view in pixels. */
    var contentHeight: Int? = null
) : AbstractSelfDescribing() {

    override val schema: String
        get() = TrackerConstants.SCHEMA_SCROLL_CHANGED

    override val dataPayload: Map<String, Any?>
        get() {
            val data = mutableMapOf<String, Any?>()
            yOffset?.let { data["y_offset"] = it }
            xOffset?.let { data["x_offset"] = it }
            viewWidth?.let { data["view_width"] = it }
            viewHeight?.let { data["view_height"] = it }
            contentWidth?.let { data["content_width"] = it }
            contentHeight?.let { data["content_height"] = it }
            return data
        }
}
