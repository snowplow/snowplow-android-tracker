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
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.utils.Util.uUIDString
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.payload.TrackerPayload

@RestrictTo(RestrictTo.Scope.LIBRARY)
class ScreenState : State {
    private var name: String
    private var type: String? = null
    private var id: String
    var previousName: String? = null
        private set
    var previousId: String? = null
        private set
    var previousType: String? = null
        private set
    private var transitionType: String? = null
    private var fragmentClassName: String? = null
    private var fragmentTag: String? = null
    private var activityClassName: String? = null
    private var activityTag: String? = null

    init {
        id = uUIDString()
        name = "Unknown"
    }

    @Synchronized
    fun updateScreenState(id: String?, name: String, type: String?, transitionType: String?) {
        populatePreviousFields()
        this.name = name
        this.type = type
        this.transitionType = transitionType
        if (id != null) {
            this.id = id
        } else {
            this.id = uUIDString()
        }
    }

    @Synchronized
    fun updateScreenState(
        id: String,
        name: String,
        type: String?,
        transitionType: String?,
        fragmentClassName: String?,
        fragmentTag: String?,
        activityClassName: String?,
        activityTag: String?
    ) {
        this.updateScreenState(id, name, type, transitionType)
        this.fragmentClassName = fragmentClassName
        this.fragmentTag = fragmentTag
        this.activityClassName = activityClassName
        this.activityTag = activityTag
    }

    private fun populatePreviousFields() {
        previousName = name
        previousType = type
        previousId = id
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
