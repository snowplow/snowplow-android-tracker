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
