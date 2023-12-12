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
package com.snowplowanalytics.core.screenviews

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.snowplow.event.ListItemView
import com.snowplowanalytics.snowplow.event.ScrollChanged
import java.lang.Integer.max

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ScreenSummaryState : State {
    private var lastUpdateTimestamp = dateGenerator()

    private var foregroundDuration: Long = 0
    private var backgroundDuration: Long = 0
    private var lastItemIndex: Int? = null
    private var itemsCount: Int? = null
    private var maxYOffset: Int? = null
    private var contentHeight: Int? = null

    val data: Map<String, Any?>
        get() {
            val data = mutableMapOf<String, Any?>(
                "foreground_sec" to foregroundDuration / 1000.0,
                "background_sec" to backgroundDuration / 1000.0
            )
            lastItemIndex?.let { data["last_item_index"] = it }
            itemsCount?.let { data["items_count"] = it }
            maxYOffset?.let { data["max_y_offset"] = it }
            contentHeight?.let { data["content_height"] = it }

            return data
        }

    fun updateTransitionToForeground() {
        val currentTimestamp = dateGenerator()

        backgroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    fun updateTransitionToBackground() {
        val currentTimestamp = dateGenerator()

        foregroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    fun updateForScreenEnd() {
        val currentTimestamp = dateGenerator()

        foregroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    fun updateWithListItemView(event: ListItemView) {
        lastItemIndex = max(event.index, lastItemIndex ?: 0)
        event.itemsCount?.let {
            itemsCount = max(it, itemsCount ?: 0)
        }

    }

    fun updateWithScrollChanged(event: ScrollChanged) {
        maxYOffset = max(event.yOffset, maxYOffset ?: 0)
        contentHeight = max(event.contentHeight, contentHeight ?: 0)
    }

    companion object {
        var dateGenerator: () -> Long = { System.currentTimeMillis() }
    }
}
