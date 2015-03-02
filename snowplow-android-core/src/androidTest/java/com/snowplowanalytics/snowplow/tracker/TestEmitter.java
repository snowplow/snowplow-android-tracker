package com.snowplowanalytics.snowplow.tracker;

import android.content.Context;

public class TestEmitter extends Emitter {

    /**
     * Creates an emitter object
     * @param builder The builder that constructs an emitter
     */
    protected TestEmitter(Emitter.EmitterBuilder builder) {
        super(builder);
    }

    public void add(Payload payload) {

    }

    public void shutdown() {

    }
}
