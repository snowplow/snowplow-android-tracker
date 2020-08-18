package com.snowplowanalytics.snowplow.tracker.storage;

import com.snowplowanalytics.snowplow.tracker.emitter.EmitterEvent;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;

import java.util.List;

/**
 * Interface for the component that
 * persists events before sending.
 */
public interface EventStore {

    /**
     * Adds an event to the store.
     * @param payload the payload to be added
     */
    void add(Payload payload);

    /**
     * Removes an event from the store.
     * @param id the identifier of the event in the store.
     * @return a boolean of success to remove.
     */
    boolean removeEvent(long id);

    /**
     * Removes a range of events from the store.
     * @param ids the events' identifiers in the store.
     * @return a boolean of success to remove.
     */
    boolean removeEvents(List<Long> ids);

    /**
     * Empties the store of all the events.
     * @return a boolean of success to remove.
     */
    boolean removeAllEvents();

    /**
     * Returns amount of events currently in the store.
     * @return the count of events in the store.
     */
    long getSize();

    /**
     * Returns a list of EmittableEvent objects which
     * contains events and related ids.
     * @return EmittableEvent objects containing
     * eventIds and event payloads.
     */
    List<EmitterEvent> getEmittableEvents();
}
