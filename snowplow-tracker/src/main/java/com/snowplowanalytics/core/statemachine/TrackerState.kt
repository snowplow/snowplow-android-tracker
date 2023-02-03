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
package com.snowplowanalytics.core.statemachine

class TrackerState : TrackerStateSnapshot {
    private var trackerState = HashMap<String, StateFuture>()
    
    @Synchronized
    fun put(stateIdentifier: String, state: StateFuture) {
        trackerState[stateIdentifier] = state
    }

    @Synchronized
    fun getStateFuture(stateIdentifier: String): StateFuture? {
        return trackerState[stateIdentifier]
    }

    fun removeState(stateIdentifier: String) {
        trackerState.remove(stateIdentifier)
    }

    @get:Synchronized
    val snapshot: TrackerStateSnapshot
        get() {
            val newTrackerState = TrackerState()
            newTrackerState.trackerState = HashMap(trackerState)
            return newTrackerState
        }

    // Implements TrackerStateSnapshot
    override fun getState(stateIdentifier: String): State? {
        val stateFuture = getStateFuture(stateIdentifier) ?: return null
        return stateFuture.state()
    }
}
