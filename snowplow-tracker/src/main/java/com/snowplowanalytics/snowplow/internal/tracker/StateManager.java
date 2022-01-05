package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing;
import com.snowplowanalytics.snowplow.event.Event;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.InspectableEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StateManager {

    private final HashMap<String, StateMachineInterface> identifierToStateMachine = new HashMap<>();
    private final HashMap<StateMachineInterface, String> stateMachineToIdentifier = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToStateMachine = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToEntitiesGenerator = new HashMap<>();
    private final HashMap<String, List<StateMachineInterface>> eventSchemaToPayloadUpdater = new HashMap<>();
    final TrackerState trackerState = new TrackerState();


    public synchronized void addOrReplaceStateMachine(@NonNull StateMachineInterface stateMachine, @NonNull String identifier) {
        StateMachineInterface previousStateMachine = identifierToStateMachine.get(identifier);
        if (previousStateMachine != null) {
            if (Objects.equals(stateMachine.getClass(), previousStateMachine.getClass())) {
                return;
            }
            removeStateMachine(identifier);
        }
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
        trackerState.removeState(identifier);
        removeFromSchemaRegistry(eventSchemaToStateMachine, stateMachine.subscribedEventSchemasForTransitions(), stateMachine);
        removeFromSchemaRegistry(eventSchemaToEntitiesGenerator, stateMachine.subscribedEventSchemasForEntitiesGeneration(), stateMachine);
        removeFromSchemaRegistry(eventSchemaToPayloadUpdater, stateMachine.subscribedEventSchemasForPayloadUpdating(), stateMachine);
        return true;
    }

    @NonNull
    synchronized TrackerStateSnapshot trackerStateForProcessedEvent(@NonNull Event event) {
        if (event instanceof AbstractSelfDescribing) {
            AbstractSelfDescribing sdEvent = (AbstractSelfDescribing) event;
            List<StateMachineInterface> stateMachines = new LinkedList<>();
            List<StateMachineInterface> stateMachinesForSchema = eventSchemaToStateMachine.get(sdEvent.getSchema());
            if (stateMachinesForSchema != null) {
                stateMachines.addAll(stateMachinesForSchema);
            }
            List<StateMachineInterface> stateMachinesGeneral = eventSchemaToStateMachine.get("*");
            if (stateMachinesGeneral != null) {
                stateMachines.addAll(stateMachinesGeneral);
            }
            for (StateMachineInterface stateMachine : stateMachines) {
                String stateIdentifier = stateMachineToIdentifier.get(stateMachine);
                StateFuture previousStateFuture = trackerState.getStateFuture(stateIdentifier);
                StateFuture currentStateFuture = new StateFuture(sdEvent, previousStateFuture, stateMachine);
                trackerState.put(stateIdentifier, currentStateFuture);
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
                currentStateFuture.getState(); // Early state-computation
            }
        }
        return trackerState.getSnapshot();
    }

    @NonNull
    synchronized List<SelfDescribingJson> entitiesForProcessedEvent(@NonNull InspectableEvent event) {
        List<SelfDescribingJson> result = new LinkedList<>();
        List<StateMachineInterface> stateMachines = new LinkedList<>();
        List<StateMachineInterface> stateMachinesForSchema = eventSchemaToEntitiesGenerator.get(event.getSchema());
        if (stateMachinesForSchema != null) {
            stateMachines.addAll(stateMachinesForSchema);
        }
        List<StateMachineInterface> stateMachinesGeneral = eventSchemaToEntitiesGenerator.get("*");
        if (stateMachinesGeneral != null) {
            stateMachines.addAll(stateMachinesGeneral);
        }
        for (StateMachineInterface stateMachine : stateMachines) {
            String stateIdentifier = stateMachineToIdentifier.get(stateMachine);
            State state = event.getState().getState(stateIdentifier);
            List<SelfDescribingJson> entities = stateMachine.entities(event, state);
            if (entities != null) {
                result.addAll(entities);
            }
        }
        return result;
    }

    public synchronized boolean addPayloadValuesToEvent(@NonNull InspectableEvent event) {
        int failures = 0;
        List<StateMachineInterface> stateMachines = new LinkedList<>();
        List<StateMachineInterface> stateMachinesForSchema = eventSchemaToPayloadUpdater.get(event.getSchema());
        if (stateMachinesForSchema != null) {
            stateMachines.addAll(stateMachinesForSchema);
        }
        List<StateMachineInterface> stateMachinesGeneral = eventSchemaToPayloadUpdater.get("*");
        if (stateMachinesGeneral != null) {
            stateMachines.addAll(stateMachinesGeneral);
        }
        for (StateMachineInterface stateMachine : stateMachines) {
            String stateIdentifier = stateMachineToIdentifier.get(stateMachine);
            State state = event.getState().getState(stateIdentifier);
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
