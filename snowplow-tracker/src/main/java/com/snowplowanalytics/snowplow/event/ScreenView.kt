/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

import android.app.Activity
import androidx.fragment.app.Fragment
import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.utils.Preconditions
import com.snowplowanalytics.core.utils.Util
import java.util.*

/** A ScreenView event.
 * @param name Name of the screen.
 * @param screenId Identifier of the screen.
 * */
class ScreenView @JvmOverloads constructor(name: String, screenId: UUID? = null) :
    AbstractSelfDescribing() {
    
    /** Name of the screen.  */
    @JvmField
    val name: String

    /** Identifier of the screen.  */
    @JvmField
    val id: String

    /** Type of screen.  */
    @JvmField
    var type: String? = null

    /** Name of the previous screen.  */
    @JvmField
    var previousName: String? = null

    /** Identifier of the previous screen.  */
    @JvmField
    var previousId: String? = null

    /** Type of the previous screen.  */
    @JvmField
    var previousType: String? = null

    /** Type of transition between previous and current screen.  */
    @JvmField
    var transitionType: String? = null

    /** Name of the Fragment subclass.  */
    @JvmField
    var fragmentClassName: String? = null

    /** Tag of the Fragment subclass.  */
    @JvmField
    var fragmentTag: String? = null

    /** Name of the Activity subclass.  */
    @JvmField
    var activityClassName: String? = null

    /** Tag of the Activity subclass.  */
    @JvmField
    var activityTag: String? = null
    
    init {
        Preconditions.checkArgument(name.isNotEmpty(), "Name cannot be empty.")
        this.name = name
        id = screenId?.toString() ?: Util.getUUIDString()
    }
    
    // Builder methods
    
    /** Type of screen.  */
    fun type(type: String?): ScreenView {
        this.type = type
        return this
    }

    /** Name of the previous screen.  */
    fun previousName(previousName: String?): ScreenView {
        this.previousName = previousName
        return this
    }

    /** Identifier of the previous screen.  */
    fun previousId(previousId: String?): ScreenView {
        this.previousId = previousId
        return this
    }

    /** Type of the previous screen.  */
    fun previousType(previousType: String?): ScreenView {
        this.previousType = previousType
        return this
    }

    /** Type of transition between previous and current screen.  */
    fun transitionType(transitionType: String?): ScreenView {
        this.transitionType = transitionType
        return this
    }

    /** Name of the Fragment subclass.  */
    fun fragmentClassName(fragmentClassName: String?): ScreenView {
        this.fragmentClassName = fragmentClassName
        return this
    }

    /** Tag of the Fragment subclass.  */
    fun fragmentTag(fragmentTag: String?): ScreenView {
        this.fragmentTag = fragmentTag
        return this
    }

    /** Name of the Activity subclass.  */
    fun activityClassName(activityClassName: String?): ScreenView {
        this.activityClassName = activityClassName
        return this
    }

    /** Tag of the Activity subclass.  */
    fun activityTag(activityTag: String?): ScreenView {
        this.activityTag = activityTag
        return this
    }

    // Tracker methods
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.SV_ID] = id
            payload[Parameters.SV_NAME] = name
            type?.let { payload[Parameters.SV_TYPE] = it }
            previousId?.let { payload[Parameters.SV_PREVIOUS_ID] = it }
            previousName?.let { payload[Parameters.SV_PREVIOUS_NAME] = it }
            previousType?.let { payload[Parameters.SV_PREVIOUS_TYPE] = it }
            transitionType?.let { payload[Parameters.SV_TRANSITION_TYPE] = it }
            return payload
        }
    
    override val schema: String
        get() = TrackerConstants.SCHEMA_SCREEN_VIEW

    companion object {
        private val TAG = ScreenView::class.java.simpleName

        /** Creates a ScreenView event using the data of an Activity class.  */
        @JvmStatic
        fun buildWithActivity(activity: Activity): ScreenView {
            val activityClassName = activity.localClassName
            val activityTag = getSnowplowScreenId(activity)
            val name = getValidName(activityClassName, activityTag)
            return ScreenView(name)
                .activityClassName(activityClassName)
                .activityTag(activityTag)
                .fragmentClassName(null)
                .fragmentTag(null)
                .type(activityClassName)
                .transitionType(null)
        }

        /** Creates a ScreenView event using the data of an Fragment class.  */
        @JvmStatic
        fun buildWithFragment(fragment: Fragment): ScreenView {
            val fragmentClassName = fragment.javaClass.simpleName
            val fragmentTag = fragment.tag
            val name = getValidName(fragmentClassName, fragmentTag)
            return ScreenView(name)
                .activityClassName(null)
                .activityTag(null)
                .fragmentClassName(fragment.javaClass.simpleName)
                .fragmentTag(fragment.tag)
                .type(fragmentClassName)
                .transitionType(null)
        }

        // Private methods
        private fun getSnowplowScreenId(activity: Activity): String? {
            val activityClass: Class<out Activity> = activity.javaClass
            try {
                val field = activityClass.getField("snowplowScreenId")
                val reflectedValue = field[activity]
                if (reflectedValue is String) {
                    return reflectedValue
                } else {
                    Logger.e(
                        TAG,
                        String.format(
                            "The value of field `snowplowScreenId` on Activity `%s` has to be a String.",
                            activityClass.simpleName
                        )
                    )
                }
            } catch (e: NoSuchFieldException) {
                Logger.d(
                    TAG,
                    String.format(
                        "Field `snowplowScreenId` not found on Activity `%s`.",
                        activityClass.simpleName
                    ),
                    e
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Error retrieving value of field `snowplowScreenId`: " + e.message, e)
            }
            return null
        }

        private fun getValidName(s1: String?, s2: String?): String {
            if (s1 != null && s1.isNotEmpty()) return s1
            return if (s2 != null && s2.isNotEmpty()) {
                s2
            } else "Unknown"
        }
    }
}
