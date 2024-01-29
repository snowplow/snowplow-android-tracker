/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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

import com.snowplowanalytics.core.emitter.Executor.execute
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.event.AbstractSelfDescribing
import com.snowplowanalytics.snowplow.event.Event
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import java.util.*

class StateManager {
    
    private val identifierToStateMachine = HashMap<String, StateMachineInterface>()
    private val stateMachineToIdentifier = HashMap<StateMachineInterface, String>()
    private val eventSchemaToStateMachine = HashMap<String, MutableList<StateMachineInterface>>()
    private val eventSchemaToEntitiesGenerator =
        HashMap<String, MutableList<StateMachineInterface>>()
    private val eventSchemaToPayloadUpdater = HashMap<String, MutableList<StateMachineInterface>>()
    private val eventSchemaToAfterTrackCallback = HashMap<String, MutableList<StateMachineInterface>>()
    private val eventSchemaToFilter = HashMap<String, MutableList<StateMachineInterface>>()
    private val eventSchemaToEventsBefore = HashMap<String, MutableList<StateMachineInterface>>()

    val trackerState = TrackerState()
    
    @Synchronized
    fun addOrReplaceStateMachine(stateMachine: StateMachineInterface) {
        val previousStateMachine = identifierToStateMachine[stateMachine.identifier]
        if (previousStateMachine != null) {
            if (stateMachine.javaClass == previousStateMachine.javaClass) {
                return
            }
            removeStateMachine(stateMachine.identifier)
        }
        
        identifierToStateMachine[stateMachine.identifier] = stateMachine
        stateMachineToIdentifier[stateMachine] = stateMachine.identifier
        addToSchemaRegistry(
            eventSchemaToStateMachine,
            stateMachine.subscribedEventSchemasForTransitions,
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToEntitiesGenerator,
            stateMachine.subscribedEventSchemasForEntitiesGeneration,
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToPayloadUpdater,
            stateMachine.subscribedEventSchemasForPayloadUpdating,
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToAfterTrackCallback,
            stateMachine.subscribedEventSchemasForAfterTrackCallback,
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToFilter,
            stateMachine.subscribedEventSchemasForFiltering,
            stateMachine
        )
        addToSchemaRegistry(
            eventSchemaToEventsBefore,
            stateMachine.subscribedEventSchemasForEventsBefore,
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
            stateMachine.subscribedEventSchemasForTransitions,
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToEntitiesGenerator,
            stateMachine.subscribedEventSchemasForEntitiesGeneration,
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToPayloadUpdater,
            stateMachine.subscribedEventSchemasForPayloadUpdating,
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToAfterTrackCallback,
            stateMachine.subscribedEventSchemasForAfterTrackCallback,
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToFilter,
            stateMachine.subscribedEventSchemasForFiltering,
            stateMachine
        )
        removeFromSchemaRegistry(
            eventSchemaToEventsBefore,
            stateMachine.subscribedEventSchemasForEventsBefore,
            stateMachine
        )
        return true
    }

    @Synchronized
    fun trackerStateForProcessedEvent(event: Event): TrackerStateSnapshot {
        if (event is AbstractSelfDescribing) {
            val stateMachines: MutableList<StateMachineInterface> = LinkedList()
            eventSchemaToStateMachine[event.schema]?.let { stateMachines.addAll(it) }
            eventSchemaToStateMachine["*"]?.let { stateMachines.addAll(it) }

            for (stateMachine in stateMachines) {
                val stateIdentifier = stateMachineToIdentifier[stateMachine]
                val previousStateFuture = stateIdentifier?.let { trackerState.getStateFuture(it) }
                val currentStateFuture = StateFuture(event, previousStateFuture, stateMachine)
                stateIdentifier?.let { trackerState.put(it, currentStateFuture) }
                
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
    fun eventsBefore(event: Event): List<Event> {
        val result: MutableList<Event> = LinkedList()
        if (event is AbstractSelfDescribing) {
            val stateMachines: MutableList<StateMachineInterface> = LinkedList()
            eventSchemaToEventsBefore[event.schema]?.let { stateMachines.addAll(it) }
            eventSchemaToEventsBefore["*"]?.let { stateMachines.addAll(it) }

            for (stateMachine in stateMachines) {
                stateMachineToIdentifier[stateMachine]?.let { stateIdentifier ->
                    stateMachine.eventsBefore(event)?.let { events ->
                        result.addAll(events)
                    }
                }
            }
        }
        return result
    }

    @Synchronized
    fun entitiesForProcessedEvent(event: StateMachineEvent): List<SelfDescribingJson> {
        val schema = event.schema ?: event.name

        val result: MutableList<SelfDescribingJson> = LinkedList()
        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        eventSchemaToEntitiesGenerator[schema]?.let { stateMachines.addAll(it) }
        eventSchemaToEntitiesGenerator["*"]?.let { stateMachines.addAll(it) }

        for (stateMachine in stateMachines) {
            stateMachineToIdentifier[stateMachine]?.let { stateIdentifier ->
                val state = event.state.getState(stateIdentifier)
                stateMachine.entities(event, state)?.let { entities ->
                    result.addAll(entities)
                }
            }
        }
        return result
    }

    @Synchronized
    fun addPayloadValuesToEvent(event: StateMachineEvent): Boolean {
        var failures = 0
        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        eventSchemaToPayloadUpdater[event.schema]?.let { stateMachines.addAll(it) }
        eventSchemaToPayloadUpdater["*"]?.let { stateMachines.addAll(it) }

        for (stateMachine in stateMachines) {
            stateMachineToIdentifier[stateMachine]?.let { stateIdentifier ->
                val state = event.state.getState(stateIdentifier)
                stateMachine.payloadValues(event, state)?.let { payloadValues ->
                    if (!event.addPayloadValues(payloadValues)) {
                        failures++
                    }
                }
            }
        }
        return failures == 0
    }

    @Synchronized
    fun afterTrack(event: StateMachineEvent) {
        val schema = event.schema ?: event.name

        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        eventSchemaToAfterTrackCallback[schema]?.let { stateMachines.addAll(it) }
        eventSchemaToAfterTrackCallback["*"]?.let { stateMachines.addAll(it) }

        if (stateMachines.isNotEmpty()) {
            execute(TAG) {
                for (stateMachine in stateMachines) {
                    stateMachine.afterTrack(event)
                }
            }
        }
    }

    @Synchronized
    fun filter(event: StateMachineEvent): Boolean {
        val schema = event.schema ?: event.name

        val stateMachines: MutableList<StateMachineInterface> = LinkedList()
        eventSchemaToFilter[schema]?.let { stateMachines.addAll(it) }
        eventSchemaToFilter["*"]?.let { stateMachines.addAll(it) }

        for (stateMachine in stateMachines) {
            stateMachineToIdentifier[stateMachine]?.let { stateIdentifier ->
                val state = event.state.getState(stateIdentifier)
                if (stateMachine.filter(event, state) == false) {
                    return false
                }
            }
        }
        return true
    }

    // Private methods
    private fun addToSchemaRegistry(
        schemaRegistry: MutableMap<String, MutableList<StateMachineInterface>>,
        schemas: List<String>,
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
        schemaRegistry: Map<String, MutableList<StateMachineInterface>>,
        schemas: List<String>,
        stateMachine: StateMachineInterface
    ) {
        for (eventSchema in schemas) {
            val list = schemaRegistry[eventSchema]
            list?.remove(stateMachine)
        }
    }


    companion object {
        private val TAG = StateManager::class.java.simpleName
    }
}
