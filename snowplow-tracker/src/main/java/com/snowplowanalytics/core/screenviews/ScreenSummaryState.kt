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
import java.lang.Integer.max

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ScreenSummaryState : State {
    private var lastUpdateTimestamp = dateGenerator()

    private var foregroundDuration: Long = 0
    private var backgroundDuration: Long = 0
    private var lastItemIndex: Int? = null
    private var itemsCount: Int? = null

    val data: Map<String, Any?>
        get() {
            val data = mutableMapOf<String, Any?>(
                "foreground_sec" to foregroundDuration / 1000.0,
                "background_sec" to backgroundDuration / 1000.0
            )
            lastItemIndex?.let { data["last_item_index"] = it }
            itemsCount?.let { data["items_count"] = it }

            return data
        }

    @Synchronized
    fun updateTransitionToForeground() {
        val currentTimestamp = dateGenerator()

        backgroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    @Synchronized
    fun updateTransitionToBackground() {
        val currentTimestamp = dateGenerator()

        foregroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    @Synchronized
    fun updateForScreenEnd() {
        val currentTimestamp = dateGenerator()

        foregroundDuration += currentTimestamp - lastUpdateTimestamp
        lastUpdateTimestamp = currentTimestamp
    }

    @Synchronized
    fun updateWithListItemView(event: ListItemView) {
        lastItemIndex = max(event.index, lastItemIndex ?: 0)
        event.itemsCount?.let {
            itemsCount = max(it, itemsCount ?: 0)
        }

    }

    companion object {
        var dateGenerator: () -> Long = { System.currentTimeMillis() }
    }
}
