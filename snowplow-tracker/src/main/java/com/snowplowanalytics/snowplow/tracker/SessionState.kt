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
package com.snowplowanalytics.snowplow.tracker

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.tracker.State

class SessionState(
    val firstEventId: String,
    val firstEventTimestamp: String,
    val sessionId: String,
    val previousSessionId: String?,  //$ On iOS it has to be set nullable on constructor
    val sessionIndex: Int,
    val userId: String,
    val storage: String
) : State {
    private val sessionContext = HashMap<String, Any?>()

    init {
        sessionContext[Parameters.SESSION_FIRST_ID] = firstEventId
        sessionContext[Parameters.SESSION_FIRST_TIMESTAMP] =
            firstEventTimestamp
        sessionContext[Parameters.SESSION_ID] = sessionId
        sessionContext[Parameters.SESSION_PREVIOUS_ID] =
            previousSessionId
        sessionContext[Parameters.SESSION_INDEX] = sessionIndex
        sessionContext[Parameters.SESSION_USER_ID] = userId
        sessionContext[Parameters.SESSION_STORAGE] = storage
    }

    val sessionValues: Map<String, Any?>
        get() = sessionContext

    companion object {
        @JvmStatic
        fun build(storedState: Map<String?, Any?>): SessionState? {
            var value: Any? = storedState[Parameters.SESSION_FIRST_ID] as? String
                ?: return null
            val firstEventId = value as String
            
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
            val previousSessionId = value as String?
            
            value = storedState[Parameters.SESSION_INDEX]
            if (value !is Int) return null
            val sessionIndex = value
            
            value = storedState[Parameters.SESSION_USER_ID]
            if (value !is String) return null
            val userId = value
            
            value = storedState[Parameters.SESSION_STORAGE]
            if (value !is String) return null
            val storage = value
            
            return SessionState(firstEventId, firstEventTimestamp, sessionId, previousSessionId, sessionIndex, userId, storage)
        }
    }
}
