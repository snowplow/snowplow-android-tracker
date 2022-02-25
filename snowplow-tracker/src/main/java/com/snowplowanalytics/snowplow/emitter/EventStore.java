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

package com.snowplowanalytics.snowplow.emitter;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.payload.Payload;

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
    void add(@NonNull Payload payload);

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
    boolean removeEvents(@NonNull List<Long> ids);

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
    @NonNull List<EmitterEvent> getEmittableEvents(int queryLimit);
}
