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

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.utils.Util

/** An error event representing an exception, error or warning message in the app.  */
class TrackerError (
    private val source: String,
    private val message: String,
    private val throwable: Throwable? = null
) : AbstractSelfDescribing() {
    
    // Tracker methods
    
    override val dataPayload: Map<String, Any?>
        get() {
            var msg = truncate(message, MAX_MESSAGE_LENGTH)
            
            if (msg == null || msg.isEmpty()) {
                msg = "Empty message found"
            }
            val payload = HashMap<String, Any?>()
            payload[Parameters.DIAGNOSTIC_ERROR_CLASS_NAME] = source
            payload[Parameters.DIAGNOSTIC_ERROR_MESSAGE] = msg
            
            if (throwable != null) {
                val stack = truncate(
                    Util.stackTraceToString(
                        throwable
                    ), MAX_STACK_LENGTH
                )
                val throwableName = truncate(throwable.javaClass.name, MAX_EXCEPTION_NAME_LENGTH)
                payload[Parameters.DIAGNOSTIC_ERROR_STACK] = stack
                payload[Parameters.DIAGNOSTIC_ERROR_EXCEPTION_NAME] = throwableName
            }
            return payload
        }
    
    override val schema: String
        get() = TrackerConstants.SCHEMA_DIAGNOSTIC_ERROR

    // Private methods
    
    private fun truncate(s: String?, maxLength: Int): String? {
        return s?.substring(0, s.length.coerceAtMost(maxLength))
    }

    companion object {
        private const val MAX_MESSAGE_LENGTH = 2048
        private const val MAX_STACK_LENGTH = 8192
        private const val MAX_EXCEPTION_NAME_LENGTH = 1024
    }
}
