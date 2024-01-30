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
package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface

/**
 * Controller for managing how events are sent.
 */
interface EmitterController : EmitterConfigurationInterface {
    /**
     * Number of events recorded in the EventStore.
     */
    val dbCount: Long

    /**
     * Whether the emitter is currently sending events.
     */
    val isSending: Boolean

    /**
     * Pause emitting events.
     * Emitting events will be suspended until resumed again.
     * Suitable for low bandwidth situations.
     */
    fun pause()
    
    /**
     * Resume emitting events if previously paused.
     * The emitter will resume emitting events again.
     */
    fun resume()
}
