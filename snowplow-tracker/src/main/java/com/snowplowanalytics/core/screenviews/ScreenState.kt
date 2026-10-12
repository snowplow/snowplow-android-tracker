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
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.utils.Util.uUIDString
import com.snowplowanalytics.snowplow.event.ScreenEnd
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload

/** Lifecycle of the current screen. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
enum class ScreenLifecycle {
    /** The screen is visible, its entity is attached to all events. */
    ACTIVE,
    /** The screen was just ended, its entity is attached only to the [ScreenEnd] event that ended it. */
    ENDING,
    /** The screen was ended earlier, its entity is no longer attached to events. */
    ENDED
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ScreenState(
    val name: String = "Unknown",
    val type: String? = null,
    val id: String = uUIDString(),
    val transitionType: String? = null,
    val fragmentClassName: String? = null,
    val fragmentTag: String? = null,
    val activityClassName: String? = null,
    val activityTag: String? = null,
    val previousScreenState: ScreenState? = null,
    /** Whether the screen is still visible or was ended using a [ScreenEnd] event. */
    val lifecycle: ScreenLifecycle = ScreenLifecycle.ACTIVE
) : State {

    val previousName: String?
        get() { return previousScreenState?.name }
    val previousId: String?
        get() { return previousScreenState?.id }
    val previousType: String?
        get() { return previousScreenState?.type }

    /**
     * Returns a copy of the state for a screen that was ended by a [ScreenEnd] event.
     * The first [ScreenEnd] moves the screen to [ScreenLifecycle.ENDING], any later one to [ScreenLifecycle.ENDED].
     */
    fun ended(): ScreenState {
        return ScreenState(
            name = name,
            type = type,
            id = id,
            transitionType = transitionType,
            fragmentClassName = fragmentClassName,
            fragmentTag = fragmentTag,
            activityClassName = activityClassName,
            activityTag = activityTag,
            lifecycle = if (lifecycle == ScreenLifecycle.ACTIVE) ScreenLifecycle.ENDING else ScreenLifecycle.ENDED
        )
    }

    fun getCurrentScreen(debug: Boolean): SelfDescribingJson {
        // this creates a screen context from screen state
        val contextPayload = TrackerPayload()
        contextPayload.add(Parameters.SCREEN_ID, id)
        contextPayload.add(Parameters.SCREEN_NAME, name)
        contextPayload.add(Parameters.SCREEN_TYPE, type)
        if (debug) {
            contextPayload.add(
                Parameters.SCREEN_FRAGMENT,
                getValidName(fragmentClassName, fragmentTag)
            )
            contextPayload.add(
                Parameters.SCREEN_ACTIVITY,
                getValidName(activityClassName, activityTag)
            )
        }
        return SelfDescribingJson(
            TrackerConstants.SCHEMA_SCREEN,
            contextPayload
        )
    }

    // Private methods
    private fun getValidName(s1: String?, s2: String?): String? {
        if (s1 != null && s1.isNotEmpty()) {
            return s1
        }
        return if (s2 != null && s2.isNotEmpty()) {
            s2
        } else null
    }
}
