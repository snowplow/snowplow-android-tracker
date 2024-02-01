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
package com.snowplowanalytics.core.screenviews

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.snowplow.event.ListItemView
import com.snowplowanalytics.snowplow.event.ScrollChanged
import java.lang.Integer.max
import java.lang.Integer.min

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ScreenSummaryState : State {
    private var lastUpdateTimestamp = dateGenerator()

    private var foregroundDuration: Long = 0
    private var backgroundDuration: Long = 0
    private var lastItemIndex: Int? = null
    private var itemsCount: Int? = null
    private var minYOffset: Int? = null
    private var minXOffset: Int? = null
    private var maxYOffset: Int? = null
    private var maxXOffset: Int? = null
    private var contentHeight: Int? = null
    private var contentWidth: Int? = null

    val data: Map<String, Any?>
        get() {
            val data = mutableMapOf<String, Any?>(
                "foreground_sec" to foregroundDuration / 1000.0,
                "background_sec" to backgroundDuration / 1000.0
            )
            lastItemIndex?.let { data["last_item_index"] = it }
            itemsCount?.let { data["items_count"] = it }
            minYOffset?.let { data["min_y_offset"] = it }
            minXOffset?.let { data["min_x_offset"] = it }
            maxYOffset?.let { data["max_y_offset"] = it }
            maxXOffset?.let { data["max_x_offset"] = it }
            contentHeight?.let { data["content_height"] = it }
            contentWidth?.let { data["content_width"] = it }

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
        event.yOffset?.let { yOffset ->
            var maxYOffset = yOffset
            event.viewHeight?.let { maxYOffset += it }

            minYOffset = min(yOffset, minYOffset ?: yOffset)
            this.maxYOffset = max(maxYOffset, this.maxYOffset ?: maxYOffset)
        }
        event.xOffset?.let { xOffset ->
            var maxXOffset = xOffset
            event.viewWidth?.let { maxXOffset += it }

            minXOffset = min(xOffset, minXOffset ?: xOffset)
            this.maxXOffset = max(maxXOffset, this.maxXOffset ?: maxXOffset)
        }
        event.contentWidth?.let { contentWidth = max(it, contentWidth ?: 0) }
        event.contentHeight?.let { contentHeight = max(it, contentHeight ?: 0) }
    }

    companion object {
        var dateGenerator: () -> Long = { System.currentTimeMillis() }
    }
}
