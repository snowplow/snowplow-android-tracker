package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.emitter.EmitterConfigurationInterface

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
