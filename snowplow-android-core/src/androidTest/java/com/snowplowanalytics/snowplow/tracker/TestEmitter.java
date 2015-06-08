package com.snowplowanalytics.snowplow.tracker;

public class TestEmitter extends Emitter {

    protected TestEmitter(Emitter.EmitterBuilder builder) {
        super(builder);
    }

    public void add(Payload payload) {}

    public void shutdown() {}

    public EventStore getEventStore() {
        return null;
    }

    public boolean getEmitterStatus() {
        return false;
    }
}
