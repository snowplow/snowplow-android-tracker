package com.snowplowanalytics.snowplow.controller;

import com.snowplowanalytics.snowplow.internal.emitter.EmitterConfigurationInterface;

public interface EmitterController extends EmitterConfigurationInterface {

    /**
     * Number of events recorded in the EventStore.
     */
    long getDbCount();

    /**
     * Whether the emitter is currently sending events.
     */
    boolean isSending();

    /**
     * Pause emitting events.
     * Emitting events will be suspended until resumed again.
     * Suitable for low bandwidth situations.
     */
    void pause();

    /**
     * Resume emitting events if previously paused.
     * The emitter will resume emitting events again.
     */
    void resume();
}
