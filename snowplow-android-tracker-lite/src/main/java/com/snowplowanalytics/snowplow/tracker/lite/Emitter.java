/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.lite;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private EventStore eventStore;

    /**
     * Creates an emitter object
     *
     * @param builder The builder that constructs an emitter
     */
    public Emitter(EmitterBuilder builder) {
        super(builder);

        this.eventStore = new EventStore(this.context, this.sendLimit);

        if (isOnline() && eventStore.getSize() > 0) {
            Executor.execute(new Runnable() {
                public void run() {
                    attemptEmit(false);
                }
            });
        }
    }

    /**
     * Adds a payload to the Event Store and
     * begins the emit function.
     *
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(final Payload payload) {
        Executor.execute(new Runnable() {
            public void run() {
                eventStore.add(payload);
                attemptEmit(false);
            }
        });
    }

    /**
     * Will send everything in the event store
     * to the collector.
     */
    public void flush() {
        Executor.execute(new Runnable() {
            public void run() {
                attemptEmit(true);
            }
        });
    }

    /**
     * Attempts to send events in the database to
     * a collector.
     *
     * @param flush A boolean override for the buffer
     *              limit of the emit attempt.
     */
    private void attemptEmit(boolean flush) {
        if (isOnline() && (flush || eventStore.getSize() >= this.bufferOption.getCode())) {

            EmittableEvents events = eventStore.getEmittableEvents();
            LinkedList<RequestResult> results = performEmit(events);

            Logger.v(TAG, "Processing emitter results.");

            // Start counting successes and failures
            int successCount = 0;
            int failureCount = 0;

            for (RequestResult res : results) {
                if (res.getSuccess()) {
                    successCount++;
                    for (Long eventId : res.getEventIds()) {
                        eventStore.removeEvent(eventId);
                    }
                } else if (!res.getSuccess()) {
                    failureCount++;
                    Logger.e(TAG, "Request sending failed but we will retry later.");
                }
            }

            Logger.d(TAG, "Success Count: %s", successCount);
            Logger.d(TAG, "Failure Count: %s", failureCount);

            // Send the callback
            if (requestCallback != null) {
                if (failureCount != 0) {
                    requestCallback.onFailure(successCount, failureCount);
                } else {
                    requestCallback.onSuccess(successCount);
                }
            }

            // If we have any failures shut the emitter down
            if (failureCount != 0) {
                if (isOnline()) {
                    Logger.e(TAG, "Ensure collector path is valid: %s", getEmitterUri());
                }
                Logger.e(TAG, "Emitter is shutting down due to failures.");
                shutdown();
            }
        } else {
            Executor.schedule(new Runnable() {
                public void run() {
                    attemptEmit(eventStore.getSize() > 0);
                }
            }, this.emitterTick, TimeUnit.SECONDS);
        }
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        Logger.d(TAG, "Shutting down emitter.");
        Executor.shutdown();
    }

    /**
     * @return the emitter event store
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter status
     */
    public boolean getEmitterStatus() {
        return Executor.status();
    }
}