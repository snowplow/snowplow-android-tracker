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
    private int emptyCount;

    private volatile boolean isRunning = false;

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
                    isRunning = true;
                    attemptEmit();
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
                if (!isRunning) {
                    isRunning = true;
                    attemptEmit();
                }
            }
        });
    }

    /**
     * Attempts to send events in the database to
     * a collector.
     * - If the emitter is not online it will not send
     * - If the emitter is online but there are no events:
     *   - Increment empty counter until emptyLimit reached
     *   - Incurs a backoff period between empty counters
     * - If the emitter is online and we have events:
     *   - Pulls allowed amount of events from database and
     *     attempts to send.
     *   - If there are failures resets running state
     *   - Otherwise will attempt to emit again
     */
    private void attemptEmit() {
        boolean online = isOnline();
        if (online && eventStore.getSize() > 0) {
            emptyCount = 0;

            EmittableEvents events = eventStore.getEmittableEvents();
            LinkedList<RequestResult> results = performEmit(events);

            Logger.v(TAG, "Processing emitter results.");

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

            if (requestCallback != null) {
                if (failureCount != 0) {
                    requestCallback.onFailure(successCount, failureCount);
                } else {
                    requestCallback.onSuccess(successCount);
                }
            }

            if (failureCount != 0) {
                if (isOnline()) {
                    Logger.e(TAG, "Ensure collector path is valid: %s", getEmitterUri());
                }
                Logger.e(TAG, "Emitter loop stopping: failures.");
                isRunning = false;
            } else {
                attemptEmit();
            }
        } else if (online) {
            if (emptyCount >= this.emptyLimit) {
                Logger.e(TAG, "Emitter loop stopping: empty limit reached.");
                isRunning = false;
            } else {
                emptyCount++;
                Logger.e(TAG, "Emitter database empty: " + emptyCount);

                try {
                    TimeUnit.SECONDS.sleep(this.emitterTick);
                } catch (InterruptedException e) {
                    Logger.e(TAG, "Emitter thread sleep interrupted: " + e.toString());
                }

                attemptEmit();
            }
        } else {
            Logger.e(TAG, "Emitter loop stopping: emitter offline.");
            isRunning = false;
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