/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.core.statemachine.PluginStateMachine
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import com.snowplowanalytics.snowplow.tracker.InspectableEvent
import java.util.function.Consumer
import java.util.function.Function

/**
 * Provides a block closure to be called after events are tracked.
 * Optionally, you can specify the event schemas for which the block should be called.
 *
 * @property schemas Optional list of event schemas to call the block for. If null, the block is called for all events.
 * @property closure Block to call after events are tracked.
 */
class PluginAfterTrackConfiguration(
    val schemas: List<String>? = null,
    val closure: Consumer<InspectableEvent>
)

/**
 * Provides a block closure that returns a list of context entities and is called when events are tracked.
 *  Optionally, you can specify the event schemas for which the block should be called.
 *
 *  @property schemas Optional list of event schemas to call the block for. If null, the block is called for all events.
 *  @property closure Block that produces entities, called when events are tracked.
 */
class PluginEntitiesConfiguration(
    val schemas: List<String>? = null,
    val closure: Function<InspectableEvent, List<SelfDescribingJson>>
)

/**
 * Interface for tracker plugin definition.
 * Specifies configurations for the closures called when and after events are tracked.
 *
 * @property identifier Unique identifier of the plugin within the tracker.
 * @property entitiesConfiguration Closure configuration that is called when events are tracked to generate context entities to enrich the events.
 * @property afterTrackConfiguration Closure configuration that is called after events are tracked.
 */
interface PluginConfigurationInterface {
    val identifier: String
    val entitiesConfiguration: PluginEntitiesConfiguration?
    val afterTrackConfiguration: PluginAfterTrackConfiguration?
}

internal fun PluginConfigurationInterface.toStateMachine(): PluginStateMachine {
    return PluginStateMachine(
        identifier = identifier,
        entitiesConfiguration = entitiesConfiguration,
        afterTrackConfiguration = afterTrackConfiguration
    )
}

/**
 * Configuration for a custom tracker plugin.
 * Enables you to add closures to be called when and after events are tracked in the tracker.
 */
class PluginConfiguration(
    override val identifier: String
) : Configuration, PluginConfigurationInterface {
    override var entitiesConfiguration: PluginEntitiesConfiguration? = null
    override var afterTrackConfiguration: PluginAfterTrackConfiguration? = null

    /**
     * Add a closure that generates entities for a given tracked event.
     *
     * @param schemas Optional list of event schemas to call the closure for. If null, the closure is called for all events.
     * @param closure Closure that produces entities, called when events are tracked.
     */
    fun entities(
        schemas: List<String>? = null,
        closure: Function<InspectableEvent, List<SelfDescribingJson>>
    ) {
        entitiesConfiguration = PluginEntitiesConfiguration(
            schemas = schemas,
            closure = closure
        )
    }

    /**
     * Add a closure that is called after the events are tracked.
     * The closure is called after the events are added to event queue in Emitter, not necessarily after they are sent to the Collector.
     *
     * @param schemas Optional list of event schemas to call the closure for. If null, the closure is called for all events.
     * @param closure Closure block to call after events are tracked.
     */
    fun afterTrack(
        schemas: List<String>? = null,
        closure: Consumer<InspectableEvent>
    ) {
        afterTrackConfiguration = PluginAfterTrackConfiguration(
            schemas = schemas,
            closure = closure
        )
    }

    override fun copy(): Configuration {
        val copy = PluginConfiguration(
            identifier=identifier,
        )
        entitiesConfiguration?.let { copy.entities(schemas = it.schemas, closure = it.closure) }
        afterTrackConfiguration?.let { copy.afterTrack(schemas = it.schemas, closure = it.closure) }
        return copy
    }
}
