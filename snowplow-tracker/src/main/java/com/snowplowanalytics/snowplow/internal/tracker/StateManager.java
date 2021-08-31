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

    private final HashMap<String, StateMachineInterface> identifierToStateMachine = new HashMap<>();
    private final HashMap<StateMachineInterface, String> stateMachineToIdentifier = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToStateMachine = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToEntitiesGenerator = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToPayloadUpdater = new HashMap<>();
    HashMap<String, StateFuture> stateIdentifierToCurrentState = new HashMap<>();


    public synchronized void addStateMachine(@NonNull StateMachineInterface stateMachine, @NonNull String identifier) {
        identifierToStateMachine.put(identifier, stateMachine);
        stateMachineToIdentifier.put(stateMachine, identifier);
        addToSchemaRegistry(eventSchemaToStateMachine, stateMachine.subscribedEventSchemasForTransitions(), stateMachine);
        addToSchemaRegistry(eventSchemaToEntitiesGenerator, stateMachine.subscribedEventSchemasForEntitiesGeneration(), stateMachine);
        addToSchemaRegistry(eventSchemaToPayloadUpdater, stateMachine.subscribedEventSchemasForPayloadUpdating(), stateMachine);
    }

    public synchronized boolean removeStateMachine(@NonNull String identifier) {
        StateMachineInterface stateMachine = identifierToStateMachine.remove(identifier);
        if (stateMachine == null) {
            return false;
        }
        stateMachineToIdentifier.remove(stateMachine);
        stateIdentifierToCurrentState.remove(identifier);
        removeFromSchemaRegistry(eventSchemaToStateMachine, stateMachine.subscribedEventSchemasForTransitions(), stateMachine);
        removeFromSchemaRegistry(eventSchemaToEntitiesGenerator, stateMachine.subscribedEventSchemasForEntitiesGeneration(), stateMachine);
        removeFromSchemaRegistry(eventSchemaToPayloadUpdater, stateMachine.subscribedEventSchemasForPayloadUpdating(), stateMachine);
        return true;
    }

    @NonNull
    synchronized Map<String, StateFuture> trackerStateByProcessedEvent(@NonNull Event event) {
        if (event instanceof AbstractSelfDescribing) {
            AbstractSelfDescribing sdEvent = (AbstractSelfDescribing) event;
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
                // TODO: Remove early state computation.
                /*
                The early state-computation causes low performance as it's executed synchronously on
                the track method thread. Ideally, the state computation should be executed only on
                entities generation or payload updating (outputs). In that case there are two problems
                to address:
                 - long chains of StateFuture filling the memory (in case the outputs are not generated)
                 - event object reuse by the user (the event object in the StateFuture could be modified
                   externally)
                 Remove the early state-computation only when these two problems are fixed.
                 */
                newState.getState(); // Early state-computation
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
            if (entities != null) {
                result.addAll(entities);
            }
        }
        return result;
    }

    public synchronized boolean addPayloadValuesForEvent(@NonNull InspectableEvent event) {
        int failures = 0;
        List<StateMachineInterface> stateMachines = eventSchemaToPayloadUpdater.get(event.getSchema());
        if (stateMachines == null) {
            stateMachines = new LinkedList<>();
        }
        List<StateMachineInterface> stateMachinesGeneral = eventSchemaToPayloadUpdater.get("*");
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
            Map<String, Object> payloadValues = stateMachine.payloadValues(event, state);
            if (payloadValues != null && !event.addPayloadValues(payloadValues)) {
                failures++;
            }
        }
        return failures == 0;
    }

    // Private methods

    private void addToSchemaRegistry(Map<String, List<StateMachineInterface>> schemaRegistry, List<String> schemas, StateMachineInterface stateMachine) {
        for (String eventSchema : schemas) {
            List<StateMachineInterface> list = schemaRegistry.get(eventSchema);
            if (list == null) {
                list = new LinkedList<>();
                schemaRegistry.put(eventSchema, list);
            }
            list.add(stateMachine);
        }
    }

    private void removeFromSchemaRegistry(Map<String, List<StateMachineInterface>> schemaRegistry, List<String> schemas, StateMachineInterface stateMachine) {
        for (String eventSchema : schemas) {
            List<StateMachineInterface> list = schemaRegistry.get(eventSchema);
            if (list != null) {
                list.remove(stateMachine);
            }
        }
    }

}
