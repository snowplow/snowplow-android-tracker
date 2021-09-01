package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;

/**
 StateFuture represents the placeholder of a future computation.
 The proper state value is computed when it's observed. Until that moment the StateFuture keeps the elements
 (event, previous StateFuture, StateMachine) needed to calculate the real state value.
 For this reason, the StateFuture can be the head of StateFuture chain which will collapse once the StateFuture
 head is asked to get the real state value.
 */
public class StateFuture {

    private Event event;
    private StateFuture previousState;
    private StateMachineInterface stateMachine;

    private State computedState;

    public StateFuture(@NonNull Event event, @Nullable StateFuture previousState, @NonNull StateMachineInterface stateMachine) {
        this.event = event;
        this.previousState = previousState;
        this.stateMachine = stateMachine;
    }

    @Nullable
    public synchronized State getState() {
        if (computedState == null && stateMachine != null) {
            State prevState = null;
            if (previousState != null) {
                prevState = previousState.getState();
            }
            computedState = stateMachine.transition(event, prevState);
            event = null;
            previousState = null;
            stateMachine = null;
        }
        return computedState;
    }

}
