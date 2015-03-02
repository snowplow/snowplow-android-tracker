package com.snowplowanalytics.snowplow.tracker.lite;

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


import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;

import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;

import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private EventStore eventStore;

    /**
     * Creates an emitter object
     * @param builder The builder that constructs an emitter
     */
    protected Emitter(com.snowplowanalytics.snowplow.tracker.Emitter.EmitterBuilder builder) {
        super(builder);
        this.eventStore = new EventStore(this.context);

        // If the device is not online do not send anything!
        if (isOnline() && eventStore.getSize() > 0) {
            Executor.execute(new Runnable() {
                public void run() {
                    attemptEmit();
                }
            });
        }
    }

    /**
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(final Payload payload) {
        eventStore.add(payload);

        // TODO, this actually needs to be started and do the right thing timer wise..

        Executor.execute(new Runnable() {
            public void run() {
                attemptEmit();
            }
        });
    }

    private void attemptEmit() {
        if (isOnline() && eventStore.getSize() > 0) {
            // TODO respect the buffer setting

            EmittableEvents events = eventStore.getEmittableEvents();
            LinkedList<RequestResult> results = performEmit(events);

            Logger.ifDebug(TAG, "Processing emitter results.");
            // Start counting successes and failures
            int successCount = 0;
            int failureCount = 0;

            for (RequestResult res : results) {
                if (res.getSuccess()) {
                    successCount++;
                    Logger.ifDebug(TAG, "Successful send.");

                    // Delete event rows for successfully sent requests
                    for (Long eventId : res.getEventIds()) {
                        eventStore.removeEvent(eventId);
                    }
                } else if (!res.getSuccess()) {
                    failureCount++;
                    Logger.ifDebug(TAG, "Request sending failed but we will retry later.");
                }
            }

            // If we have any failures shut the emitter down
            // TODO on all failures? Or just some?
            if (failureCount != 0) {
                if (isOnline()) {
                    Logger.ifDebug(TAG, "Check your collector path: %s",
                            getEmitterUri());
                }
                //shutdown(); // TODO sort out when we should shut down on failures
            }

            // Send the callback

            if (requestCallback != null) {
                if (failureCount != 0) {
                    requestCallback.onFailure(successCount, failureCount);
                } else {
                    requestCallback.onSuccess(successCount);
                }
            }


        } else {
            // TODO use parameters for deciding attempt schedule
            Executor.schedule(new Runnable() {
                public void run() { attemptEmit(); }
            }, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        Executor.shutdown();
    }

}