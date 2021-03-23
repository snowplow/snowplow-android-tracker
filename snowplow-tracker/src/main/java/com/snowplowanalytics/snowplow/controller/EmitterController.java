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
}
