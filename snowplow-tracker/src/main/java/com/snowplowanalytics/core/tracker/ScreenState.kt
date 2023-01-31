package com.snowplowanalytics.core.tracker

import androidx.annotation.RestrictTo
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
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
