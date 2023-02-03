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

import com.snowplowanalytics.snowplow.event.Event

/**
 * StateFuture represents the placeholder of a future computation.
 * The proper state value is computed when it's observed. Until that moment the StateFuture keeps the elements
 * (event, previous StateFuture, StateMachine) needed to calculate the real state value.
 * For this reason, the StateFuture can be the head of StateFuture chain which will collapse once the StateFuture
 * head is asked to get the real state value.
 */
class StateFuture(event: Event, previousState: StateFuture?, stateMachine: StateMachineInterface) {
    private var event: Event?
    private var previousState: StateFuture?
    private var stateMachine: StateMachineInterface?
    private var computedState: State? = null

    init {
        this.event = event
        this.previousState = previousState
        this.stateMachine = stateMachine
    }
    
    @Synchronized
    fun state(): State? {
        if (computedState == null && stateMachine != null) {
            var prevState: State? = null
            previousState?.let { prevState = it.state() }
            computedState = event?.let { stateMachine!!.transition(it, prevState) }
            event = null
            previousState = null
            stateMachine = null
        }
        return computedState
    }
}
