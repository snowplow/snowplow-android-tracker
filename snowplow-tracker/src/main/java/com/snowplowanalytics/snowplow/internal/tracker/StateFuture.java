package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.event.Event;

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
