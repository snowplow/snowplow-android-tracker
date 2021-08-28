package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StateManager {

    private HashMap<String, StateMachineInterface> identifierToStateMachine = new HashMap<>();
    private HashMap<StateMachineInterface, String> stateMachineToIdentifier = new HashMap<>();
    private HashMap<String, List<StateMachineInterface>> eventSchemaToStateMachine = new HashMap<>();
    private HashMap<String, List<StateMachineInterface>> eventSchemaToEntitiesGenerator = new HashMap<>();
    HashMap<String, StateFuture> stateIdentifierToCurrentState = new HashMap<>();


    public synchronized void addStateMachine(@NonNull StateMachineInterface stateMachine, @NonNull String identifier) {
        identifierToStateMachine.put(identifier, stateMachine);
        stateMachineToIdentifier.put(stateMachine, identifier);
        for (String eventSchema : stateMachine.subscribedEventSchemasForTransitions()) {
            List<StateMachineInterface> list = eventSchemaToStateMachine.get(eventSchema);
            if (list == null) {
                list = new LinkedList<>();
                eventSchemaToStateMachine.put(eventSchema, list);
            }
            list.add(stateMachine);
        }
        for (String eventSchema : stateMachine.subscribedEventSchemasForEntitiesGeneration()) {
            List<StateMachineInterface> list = eventSchemaToEntitiesGenerator.get(eventSchema);
            if (list == null) {
                list = new LinkedList<>();
                eventSchemaToEntitiesGenerator.put(eventSchema, list);
            }
            list.add(stateMachine);
        }
    }

    public synchronized boolean removeStateMachine(@NonNull String identifier) {
        StateMachineInterface stateMachine = identifierToStateMachine.remove(identifier);
        if (stateMachine == null) {
            return false;
        }
        stateMachineToIdentifier.remove(stateMachine);
        stateIdentifierToCurrentState.remove(identifier);
        for (String eventSchema : stateMachine.subscribedEventSchemasForTransitions()) {
            List<StateMachineInterface> list = eventSchemaToStateMachine.get(eventSchema);
            list.remove(stateMachine);
        }
        for (String eventSchema : stateMachine.subscribedEventSchemasForEntitiesGeneration()) {
            List<StateMachineInterface> list = eventSchemaToEntitiesGenerator.get(eventSchema);
            list.remove(stateMachine);
        }
        return true;
    }

    @NonNull
    synchronized Map<String, StateFuture> trackerStateByProcessedEvent(@NonNull Event event) {
        if (event instanceof AbstractSelfDescribing) {
            AbstractSelfDescribing sdEvent = (AbstractSelfDescribing)event;
            List<StateMachineInterface> stateMachines = eventSchemaToStateMachine.get(sdEvent.getSchema());
            if (stateMachines == null) {
                stateMachines = new LinkedList<>();
            }
            List<StateMachineInterface> stateMachinesGeneral = eventSchemaToStateMachine.get("*");
            if (stateMachinesGeneral != null) {
                stateMachines.addAll(stateMachinesGeneral);
            }
            for (StateMachineInterface stateMachine : stateMachines) {
                String stateIdentifier = stateMachineToIdentifier.get(stateMachine);
                StateFuture previousState = stateIdentifierToCurrentState.get(stateIdentifier);
                StateFuture newState = new StateFuture(sdEvent, previousState, stateMachine);
                stateIdentifierToCurrentState.put(stateIdentifier, newState);
            }
        }
        return new HashMap<>(stateIdentifierToCurrentState);
    }

    @NonNull
    synchronized List<SelfDescribingJson> entitiesByProcessedEvent(@NonNull InspectableEvent event) {
        List<SelfDescribingJson> result = new LinkedList<>();
        List<StateMachineInterface> stateMachines = eventSchemaToEntitiesGenerator.get(event.getSchema());
        if (stateMachines == null) {
            stateMachines = new LinkedList<>();
        }
        List<StateMachineInterface> stateMachinesGeneral = eventSchemaToEntitiesGenerator.get("*");
        if (stateMachinesGeneral != null) {
            stateMachines.addAll(stateMachinesGeneral);
        }
        for (StateMachineInterface stateMachine : stateMachines) {
            String stateIdentifier = stateMachineToIdentifier.get(stateMachine);
            StateFuture stateFuture = event.getState().get(stateIdentifier);
            State state = null;
            if (stateFuture != null) {
                state = stateFuture.getState();
            }
            List<SelfDescribingJson> entities = stateMachine.entities(event, state);
            result.addAll(entities);
        }
        return result;
    }

}
