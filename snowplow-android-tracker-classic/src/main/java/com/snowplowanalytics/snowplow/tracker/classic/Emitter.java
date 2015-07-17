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
import java.util.concurrent.atomic.AtomicBoolean;

import com.snowplowanalytics.snowplow.tracker.emitter.ReadyRequest;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;
import com.squareup.okhttp.Request;

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();

    private EventStore eventStore;
    private int emptyCount;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * Creates an emitter object
     *
     * @param builder The builder that constructs an emitter
     */
    public Emitter(EmitterBuilder builder) {
        super(builder);

        this.eventStore = new EventStore(this.context, this.sendLimit);
    }

    /**
     * Adds a payload to the Event Store and
     * begins the emit function.
     *
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(final Payload payload) {
        eventStore.add(payload);
        if (isRunning.compareAndSet(false, true)) {
            attemptEmit();
        }
    }

    /**
     * Starts the emitter if it is not running.
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
        if (Util.isOnline(this.context)) {
            if (eventStore.getSize() > 0) {
                emptyCount = 0;

                EmittableEvents events = eventStore.getEmittableEvents();
                LinkedList<ReadyRequest> requests = buildRequests(events);
                LinkedList<RequestResult> results = performAsyncEmit(requests);

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
     * Performs asynchronous sending of a list of
     * ReadyRequests.
     *
     * @param requests the requests to send
     * @return the request results
     */
    private LinkedList<RequestResult> performAsyncEmit(LinkedList<ReadyRequest> requests) {
        LinkedList<RequestResult> results = new LinkedList<>();
        LinkedList<Future> futures = new LinkedList<>();

        // Start all requests in the ThreadPool
        for (ReadyRequest request : requests) {
            futures.add(Executor.futureCallable(requestFuture(request.getRequest())));
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
        return results;
    }

    /**
     * Asynchronously removes all the marked
     * event ids from successfully sent events.
     * - If events fail to be removed they will be
     *   double sent.  At small levels this should
     *   not be an issue as we can de-dupe later.
     *
     * @param eventIds the events ids to remove
     * @return a list of booleans stating success
     */
    private LinkedList<Boolean> performAsyncEventRemoval(LinkedList<Long> eventIds) {
        LinkedList<Boolean> results = new LinkedList<>();
        LinkedList<Future> futures = new LinkedList<>();

        // Start all requests in the ThreadPool
        for (Long id : eventIds) {
            futures.add(Executor.futureCallable(removeFuture(id)));
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
        return results;
    }

    /**
     * Returns a Callable Request
     *
     * @param request the request to nest into
     *                the callable.
     * @return the new Callable object
     */
    private Callable<Integer> requestFuture(final Request request) {
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
     * @param eventId the eventId to remove
     * @return the new Callable object
     */
    private Callable<Boolean> removeFuture(final Long eventId) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return eventStore.removeEvent(eventId);
            }
        };
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        Logger.d(TAG, "Shutting down emitter.");
        isRunning.compareAndSet(true, false);
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
        return isRunning.get() && Executor.status();
    }
}