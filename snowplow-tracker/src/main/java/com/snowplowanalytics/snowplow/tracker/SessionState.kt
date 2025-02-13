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
package com.snowplowanalytics.snowplow.tracker

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.statemachine.State
import com.snowplowanalytics.core.utils.Util

/**
 * Stores the current Session information. Used in creating the client_session entity when
 * [sessionContext](com.snowplowanalytics.snowplow.configuration.TrackerConfiguration.sessionContext) is configured.
 * 
 * @see com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
 */
class SessionState(
    var firstEventId: String,
    var firstEventTimestamp: String,
    var sessionId: String,
    var previousSessionId: String?,  //$ On iOS it has to be set nullable on constructor
    var sessionIndex: Int,
    var userId: String,
    var storage: String = "LOCAL_STORAGE",
    var eventIndex: Int? = null,
    var lastUpdate: Long? = null
) : State {

    val sessionValues: Map<String, Any?>
        get() {
            val sessionContext = HashMap<String, Any?>()
            sessionContext[Parameters.SESSION_FIRST_ID] = firstEventId
            sessionContext[Parameters.SESSION_FIRST_TIMESTAMP] =
                firstEventTimestamp
            sessionContext[Parameters.SESSION_ID] = sessionId
            sessionContext[Parameters.SESSION_PREVIOUS_ID] =
                previousSessionId
            sessionContext[Parameters.SESSION_INDEX] = sessionIndex
            sessionContext[Parameters.SESSION_USER_ID] = userId
            sessionContext[Parameters.SESSION_STORAGE] = storage

            eventIndex?.let {
                sessionContext[Parameters.SESSION_EVENT_INDEX] = it
            }
            return sessionContext
        }


    val dataToPersist: Map<String, Any?>
        get() {
            val dictionary = sessionValues.toMutableMap()

            lastUpdate?.let {
                dictionary[Parameters.SESSION_LAST_UPDATE] = it
            }

            return dictionary
        }

    companion object {
        @JvmStatic
        fun build(storedState: Map<String?, Any?>): SessionState? {
            var value: Any? = storedState[Parameters.SESSION_FIRST_ID]
            if (value !is String) return null
            val firstEventId = value
            
            value = storedState[Parameters.SESSION_FIRST_TIMESTAMP]
            if (value !is String) return null
            val firstEventTimestamp = value
            
            value = storedState[Parameters.SESSION_ID]
            if (value !is String) return null
            val sessionId = value
            
            value = storedState[Parameters.SESSION_PREVIOUS_ID]
            if (value !is String) {
                value = null
            }
            val previousSessionId = value as? String?
            
            value = storedState[Parameters.SESSION_INDEX]
            if (value !is Int) return null
            val sessionIndex = value
            
            value = storedState[Parameters.SESSION_USER_ID]
            if (value !is String) return null
            val userId = value
            
            value = storedState[Parameters.SESSION_STORAGE]
            if (value !is String) return null
            val storage = value

            val eventIndex = storedState[Parameters.SESSION_EVENT_INDEX] as? Int
            val lastUpdate = storedState[Parameters.SESSION_LAST_UPDATE] as? Long

            return SessionState(
                firstEventId=firstEventId,
                firstEventTimestamp=firstEventTimestamp,
                sessionId=sessionId,
                previousSessionId=previousSessionId,
                sessionIndex=sessionIndex,
                userId=userId,
                storage=storage,
                eventIndex=eventIndex,
                lastUpdate=lastUpdate
            )
        }
    }

    fun startNewSession(eventId: String, eventTimestamp: Long) {
        this.previousSessionId = this.sessionId
        this.sessionId = Util.uUIDString()
        this.sessionIndex = this.sessionIndex + 1
        this.eventIndex = 0
        this.firstEventId = eventId
        this.firstEventTimestamp = Util.getDateTimeFromTimestamp(eventTimestamp)

        this.lastUpdate = System.currentTimeMillis()
    }

    fun updateForNextEvent(isSessionCheckerEnabled: Boolean) {
        this.eventIndex = (this.eventIndex ?: 0) + 1
        if (isSessionCheckerEnabled) {
            this.lastUpdate = System.currentTimeMillis()
        }
    }
}
