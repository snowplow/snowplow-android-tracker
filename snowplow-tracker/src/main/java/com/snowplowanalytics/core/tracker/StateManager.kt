package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import java.util.*

class StateManager {
    
    private val identifierToStateMachine = HashMap<String, StateMachineInterface>()
    private val stateMachineToIdentifier = HashMap<StateMachineInterface, String>()
    private val eventSchemaToStateMachine = HashMap<String?, MutableList<StateMachineInterface>>()
    private val eventSchemaToEntitiesGenerator =
        HashMap<String?, MutableList<StateMachineInterface>>()
    private val eventSchemaToPayloadUpdater = HashMap<String?, MutableList<StateMachineInterface>>()
    
    @JvmField
    val trackerState = TrackerState()
    
    @Synchronized
    fun addOrReplaceStateMachine(stateMachine: StateMachineInterface, identifier: String) {
        val previousStateMachine = identifierToStateMachine[identifier]
        if (previousStateMachine != null) {
            if (stateMachine.javaClass == previousStateMachine.javaClass) {
                return
            }
            removeStateMachine(identifier)
        }
        
        identifierToStateMachine[identifier] = stateMachine
        stateMachineToIdentifier[stateMachine] = identifier
        addToSchemaRegistry(
            eventSchemaToStateMachine,
            stateMachine.subscribedEventSchemasForTransitions(),
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToEntitiesGenerator,
            stateMachine.subscribedEventSchemasForEntitiesGeneration(),
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToPayloadUpdater,
            stateMachine.subscribedEventSchemasForPayloadUpdating(),
            stateMachine
        )
    }

    @Synchronized
    fun removeStateMachine(identifier: String): Boolean {
        val stateMachine = identifierToStateMachine.remove(identifier) ?: return false
        
        stateMachineToIdentifier.remove(stateMachine)
        trackerState.removeState(identifier)
        removeFromSchemaRegistry(
            eventSchemaToStateMachine,
            stateMachine.subscribedEventSchemasForTransitions(),
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToEntitiesGenerator,
            stateMachine.subscribedEventSchemasForEntitiesGeneration(),
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToPayloadUpdater,
            stateMachine.subscribedEventSchemasForPayloadUpdating(),
            stateMachine
        )
        return true
    }

    @Synchronized
    fun trackerStateForProcessedEvent(event: Event): TrackerStateSnapshot {
        if (event is AbstractSelfDescribing) {
            val stateMachines: MutableList<StateMachineInterface> = LinkedList()
            val stateMachinesForSchema: List<StateMachineInterface>? =
                eventSchemaToStateMachine[event.schema]
            if (stateMachinesForSchema != null) {
                stateMachines.addAll(stateMachinesForSchema)
            }
            val stateMachinesGeneral: List<StateMachineInterface>? = eventSchemaToStateMachine["*"]
            if (stateMachinesGeneral != null) {
                stateMachines.addAll(stateMachinesGeneral)
            }
            for (stateMachine in stateMachines) {
                val stateIdentifier = stateMachineToIdentifier[stateMachine]
                val previousStateFuture = trackerState.getStateFuture(stateIdentifier!!)
                val currentStateFuture = StateFuture(event, previousStateFuture, stateMachine)
                trackerState.put(stateIdentifier, currentStateFuture)
                
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
                currentStateFuture.state() // Early state-computation
            }
        }
        return trackerState.snapshot
    }

    @Synchronized
    fun entitiesForProcessedEvent(event: InspectableEvent): List<SelfDescribingJson> {
        val result: MutableList<SelfDescribingJson> = LinkedList()
        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        val stateMachinesForSchema: List<StateMachineInterface>? =
            eventSchemaToEntitiesGenerator[event.schema]
        if (stateMachinesForSchema != null) {
            stateMachines.addAll(stateMachinesForSchema)
        }
        val stateMachinesGeneral: List<StateMachineInterface>? = eventSchemaToEntitiesGenerator["*"]
        if (stateMachinesGeneral != null) {
            stateMachines.addAll(stateMachinesGeneral)
        }
        for (stateMachine in stateMachines) {
            val stateIdentifier = stateMachineToIdentifier[stateMachine]
            val state = event.state.getState(stateIdentifier!!)
            val entities = stateMachine.entities(event, state)
            if (entities != null) {
                result.addAll(entities)
            }
        }
        return result
    }

    @Synchronized
    fun addPayloadValuesToEvent(event: InspectableEvent): Boolean {
        var failures = 0
        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        val stateMachinesForSchema: List<StateMachineInterface>? =
            eventSchemaToPayloadUpdater[event.schema]
        if (stateMachinesForSchema != null) {
            stateMachines.addAll(stateMachinesForSchema)
        }
        val stateMachinesGeneral: List<StateMachineInterface>? = eventSchemaToPayloadUpdater["*"]
        if (stateMachinesGeneral != null) {
            stateMachines.addAll(stateMachinesGeneral)
        }
        for (stateMachine in stateMachines) {
            val stateIdentifier = stateMachineToIdentifier[stateMachine]
            val state = event.state.getState(stateIdentifier!!)
            val payloadValues = stateMachine.payloadValues(event, state)
            if (payloadValues != null && !event.addPayloadValues(payloadValues)) {
                failures++
            }
        }
        return failures == 0
    }

    // Private methods
    private fun addToSchemaRegistry(
        schemaRegistry: MutableMap<String?, MutableList<StateMachineInterface>>,
        schemas: List<String?>,
        stateMachine: StateMachineInterface
    ) {
        for (eventSchema in schemas) {
            var list = schemaRegistry[eventSchema]
            if (list == null) {
                list = LinkedList()
                schemaRegistry[eventSchema] = list
            }
            list.add(stateMachine)
        }
    }

    private fun removeFromSchemaRegistry(
        schemaRegistry: Map<String?, MutableList<StateMachineInterface>>,
        schemas: List<String?>,
        stateMachine: StateMachineInterface
    ) {
        for (eventSchema in schemas) {
            val list = schemaRegistry[eventSchema]
            list?.remove(stateMachine)
        }
    }
}
