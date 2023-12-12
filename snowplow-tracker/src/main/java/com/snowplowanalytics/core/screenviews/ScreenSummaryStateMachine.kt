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

import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.statemachine.StateMachineInterface
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent

class ScreenSummaryStateMachine : StateMachineInterface {

    override val identifier: String
        get() = ID

    override val subscribedEventSchemasForTransitions: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW, TrackerConstants.SCHEMA_SCREEN_END, Foreground.schema, Background.schema, TrackerConstants.SCHEMA_LIST_ITEM_VIEW, TrackerConstants.SCHEMA_SCROLL_CHANGED)

    override val subscribedEventSchemasForEntitiesGeneration: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_END, Foreground.schema, Background.schema)

    override val subscribedEventSchemasForPayloadUpdating: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForAfterTrackCallback: List<String>
        get() = emptyList()

    override val subscribedEventSchemasForFiltering: List<String>
        get() = listOf(TrackerConstants.SCHEMA_LIST_ITEM_VIEW, TrackerConstants.SCHEMA_SCREEN_END, TrackerConstants.SCHEMA_SCROLL_CHANGED)

    override val subscribedEventSchemasForEventsBefore: List<String>
        get() = listOf(TrackerConstants.SCHEMA_SCREEN_VIEW)

    override fun transition(event: Event, state: State?): State? {
        if (event is ScreenView) {
            return ScreenSummaryState()
        }
        val screenSummaryState = state as ScreenSummaryState? ?: return null
        when (event) {
            is Foreground -> {
                screenSummaryState.updateTransitionToForeground()
            }
            is Background -> {
                screenSummaryState.updateTransitionToBackground()
            }
            is ScreenEnd -> {
                screenSummaryState.updateForScreenEnd()
            }
            is ListItemView -> {
                screenSummaryState.updateWithListItemView(event)
            }
            is ScrollChanged -> {
                screenSummaryState.updateWithScrollChanged(event)
            }
        }
        return state
    }

    override fun entities(event: InspectableEvent, state: State?): List<SelfDescribingJson>? {
        val screenSummaryState = state as ScreenSummaryState? ?: return null

        return listOf(
            SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN_SUMMARY,
                screenSummaryState.data
            )
        )
    }

    override fun payloadValues(event: InspectableEvent, state: State?): Map<String, Any>? {
        return null
    }

    override fun afterTrack(event: InspectableEvent) {
    }

    override fun filter(event: InspectableEvent, state: State?): Boolean {
        if (event.schema == TrackerConstants.SCHEMA_SCREEN_END) {
            return state != null
        }
        // do not track list item view and scroll changed events
        return false
    }

    override fun eventsBefore(event: Event): List<Event>? {
        return listOf(ScreenEnd())
    }

    companion object {
        val ID: String
            get() = "ScreenSummaryContext"
    }
}
