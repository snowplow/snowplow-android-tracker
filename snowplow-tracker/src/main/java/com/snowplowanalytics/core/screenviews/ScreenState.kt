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
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload

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
    val previousScreenState: ScreenState? = null
) : State {

    val previousName: String?
        get() { return previousScreenState?.name }
    val previousId: String?
        get() { return previousScreenState?.id }
    val previousType: String?
        get() { return previousScreenState?.type }


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
