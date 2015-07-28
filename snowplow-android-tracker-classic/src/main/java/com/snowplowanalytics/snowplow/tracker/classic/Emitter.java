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

package com.snowplowanalytics.snowplow.tracker.classic;

import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.snowplowanalytics.snowplow.tracker.emitter.ReadyRequest;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.squareup.okhttp.Request;

/**
 * Builds a Emitter object which is used to store events
 * in a SQLite Database and to send events to a Snowplow
 * Collector.
 */
public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private EventStore eventStore;
    private int emptyCount;

    /**
     * Constructs the Emitter Object.
     *
     * @param builder the base emitter builder.
     */
    public Emitter(EmitterBuilder builder) {
        super(builder);

        this.eventStore = new EventStore(this.context, this.sendLimit);
    }

    /**
     * Adds a payload to the EventStore and
     * then attempts to start the emitter
     * if it is not currently running.
     *
     * @param payload the event payload
     *                to be added.
     */
    public void add(final Payload payload) {
        eventStore.add(payload);
        if (isRunning.compareAndSet(false, true)) {
            attemptEmit();
        }
    }

    /**
     * Attempts to start the emitter if it
     * is not currently running.
     */
    public void flush() {
        Executor.execute(new Runnable() {
            public void run() {
                if (isRunning.compareAndSet(false, true)) {
                    attemptEmit();
                }
            }
        });
    }

    /**
     * Attempts to send events in the database to
     * a collector.
     *
     * - If the emitter is not online it will not send
     * - If the emitter is online but there are no events:
     *   + Increment empty counter until emptyLimit reached
     *   + Incurs a backoff period between empty counters
     * - If the emitter is online and we have events:
     *   + Pulls allowed amount of events from database and
     *     attempts to send.
     *   + If there are failures resets running state
     *   + Otherwise will attempt to emit again
     */
    private void attemptEmit() {
        if (Util.isOnline(this.context)) {
            if (eventStore.getSize() > 0) {
                emptyCount = 0;

                EmittableEvents events = eventStore.getEmittableEvents();
                LinkedList<ReadyRequest> requests = buildRequests(events);
                LinkedList<RequestResult> results = performAsyncEmit(requests);

                events = null;
                requests = null;

                Logger.v(TAG, "Processing emitter results.");

                int successCount = 0;
                int failureCount = 0;
                LinkedList<Long> removableEvents = new LinkedList<>();

                for (RequestResult res : results) {
                    if (res.getSuccess()) {
                        for (final Long eventId : res.getEventIds()) {
                            removableEvents.add(eventId);
                        }
                        successCount += res.getEventIds().size();
                    } else {
                        failureCount += res.getEventIds().size();
                        Logger.e(TAG, "Request sending failed but we will retry later.");
                    }
                }
                performAsyncEventRemoval(removableEvents);

                results = null;
                removableEvents = null;

                Logger.d(TAG, "Success Count: %s", successCount);
                Logger.d(TAG, "Failure Count: %s", failureCount);

                if (requestCallback != null) {
                    if (failureCount != 0) {
                        requestCallback.onFailure(successCount, failureCount);
                    } else {
                        requestCallback.onSuccess(successCount);
                    }
                }

                if (failureCount > 0 && successCount == 0) {
                    if (Util.isOnline(this.context)) {
                        Logger.e(TAG, "Ensure collector path is valid: %s", getEmitterUri());
                    }
                    Logger.e(TAG, "Emitter loop stopping: failures.");
                    isRunning.compareAndSet(true, false);
                } else {
                    attemptEmit();
                }
            } else {
                if (emptyCount >= this.emptyLimit) {
                    Logger.e(TAG, "Emitter loop stopping: empty limit reached.");
                    isRunning.compareAndSet(true, false);
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
            }
        } else {
            Logger.e(TAG, "Emitter loop stopping: emitter offline.");
            isRunning.compareAndSet(true, false);
        }
    }

    /**
     * Asynchronously sends all of the
     * ReadyRequests in the List to the
     * defined endpoint.
     *
     * @param requests the requests to be
     *                 sent
     * @return the results of each request
     */
    private LinkedList<RequestResult> performAsyncEmit(LinkedList<ReadyRequest> requests) {
        LinkedList<RequestResult> results = new LinkedList<>();
        LinkedList<Future> futures = new LinkedList<>();

        // Start all requests in the ThreadPool
        for (ReadyRequest request : requests) {
            futures.add(Executor.futureCallable(getRequestCallable(request.getRequest())));
        }

        Logger.d(TAG, "Request Futures: %s", futures.size());

        // Get results of futures
        // - Wait up to 5 seconds for the request
        for (int i = 0; i < futures.size(); i++) {
            int code = -1;

            try {
                code = (int) futures.get(i).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Logger.e(TAG, "Request Future was interrupted: %s", ie.getMessage());
            } catch (ExecutionException ee) {
                Logger.e(TAG, "Request Future failed: %s", ee.getMessage());
            } catch (TimeoutException te) {
                Logger.e(TAG, "Request Future had a timeout: %s", te.getMessage());
            }

            if (requests.get(i).isOversize()) {
                results.add(new RequestResult(true, requests.get(i).getEventIds()));
            } else {
                results.add(new RequestResult(isSuccessfulSend(code), requests.get(i).getEventIds()));
            }
        }

        requests = null;
        futures = null;

        return results;
    }

    /**
     * Asynchronously removes all the marked
     * event ids from successfully sent events.
     *
     * NOTE: If events fail to be removed they
     * will be double sent.  At small levels this
     * should not be an issue as we can de-dupe later.
     *
     * @param eventIds the events ids to remove
     * @return a list of booleans
     */
    private LinkedList<Boolean> performAsyncEventRemoval(LinkedList<Long> eventIds) {
        LinkedList<Boolean> results = new LinkedList<>();
        LinkedList<Future> futures = new LinkedList<>();

        // Start all requests in the ThreadPool
        for (Long id : eventIds) {
            futures.add(Executor.futureCallable(getRemoveCallable(id)));
        }

        Logger.d(TAG, "Removal Futures: %s", futures.size());

        // Get results of futures
        for (int i = 0; i < futures.size(); i++) {
            boolean result = false;
            try {
                result = (boolean) futures.get(i).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Logger.e(TAG, "Removal Future was interrupted: %s", ie.getMessage());
            } catch (ExecutionException ee) {
                Logger.e(TAG, "Removal Future failed: %s", ee.getMessage());
            } catch (TimeoutException te) {
                Logger.e(TAG, "Removal Future had a timeout: %s", te.getMessage());
            }
            results.add(result);
        }

        eventIds = null;
        futures = null;

        return results;
    }

    /**
     * Returns a Callable Request Send
     *
     * @param request the request to be
     *                sent
     * @return the new Callable object
     */
    private Callable<Integer> getRequestCallable(final Request request) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return requestSender(request);
            }
        };
    }

    /**
     * Returns a Callable Event Removal
     *
     * @param eventId the eventId to
     *                remove
     * @return the new Callable object
     */
    private Callable<Boolean> getRemoveCallable(final Long eventId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return eventStore.removeEvent(eventId);
            }
        };
    }

    /**
     * Resets the `isRunning` truth to false.
     */
    public void shutdown() {
        Logger.d(TAG, "Shutting down emitter.");
        isRunning.compareAndSet(true, false);
        Executor.shutdown();
    }

    /**
     * @return the emitter event store object
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter status
     */
    public boolean getEmitterStatus() {
        return isRunning.get();
    }
}