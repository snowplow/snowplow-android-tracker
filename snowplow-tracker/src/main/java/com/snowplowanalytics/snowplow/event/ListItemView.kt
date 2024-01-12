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
 * Event tracking the view of an item in a list.
 * If screen engagement tracking is enabled, the list item view events will be aggregated into a `screen_summary` entity and won't be sent as separate events to the collector.
 *
 * Schema: `iglu:com.snowplowanalytics.mobile/list_item_view/jsonschema/1-0-0`
 */
class ListItemView (
    /** Index of the item in the list. */
    var index: Int,
    /** Total number of items in the list. */
    var itemsCount: Int?
) : AbstractSelfDescribing() {

    override val schema: String
        get() = TrackerConstants.SCHEMA_LIST_ITEM_VIEW

    override val dataPayload: Map<String, Any?>
        get() {
            return mapOf(
                "index" to index,
                "items_count" to itemsCount
            )
        }
}
