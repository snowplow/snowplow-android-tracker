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

package com.snowplowanalytics.snowplow.tracker.rx;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Subscription;

import com.snowplowanalytics.snowplow.tracker.emitter.ReadyRequest;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.emitter.EmitterException;
import com.snowplowanalytics.snowplow.tracker.emitter.EmittableEvents;

/**
 * Build an emitter object which controls the
 * sending of events to the Snowplow Collector.
 */
public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();
    private int emptyCounter = 0;
    private Subscription emitterSub;
    private EventStore eventStore;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public Emitter(EmitterBuilder builder) {
        super(builder);

        // Create the event store with the context and the buffer option
        this.eventStore = new EventStore(this.context, this.sendLimit);
    }

    /**
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(Payload payload) {

        // Adds the payload to the EventStore
        eventStore.add(payload);

        // If the emitter is currently shutdown start it..
        if (isRunning.compareAndSet(false, true)) {
            start();
        }
    }

    /**
     * Starts the emitter if it is currently
     * shutdown.
     */
    public void flush() {
        if (isRunning.compareAndSet(false, true)) {
            start();
        }
    }

    /**
     * Starts a polling emitter subscription
     * which will send all events to the collector.
     *
     * 1. If it is currently sending events we
     *    cannot get new batches of events.
     * 2. If it pulls an empty batch of events
     *    a certain amount of times we
     *    shutdown and wait for a new event add.
     * 3. This subscription will only start if
     *    we are online.
     */
    private void start() {
        emitterSub = Observable.interval(this.emitterTick, TimeUnit.SECONDS, SchedulerRx.getScheduler())
            .map(tick -> doEmitterTick())
            .doOnError(err -> Logger.e(TAG, "Emitter Error: %s", err.toString()))
            .retry()
            .subscribeOn(SchedulerRx.getScheduler())
            .unsubscribeOn(SchedulerRx.getScheduler())
            .doOnSubscribe(() -> Logger.d(TAG, "Emitter has been started."))
            .doOnUnsubscribe(() -> Logger.d(TAG, "Emitter has been shutdown."))
            .map(this::buildRequestsRx)
            .map(this::performAsyncEmit)
            .subscribe(this::processEmitterResults);
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        if (emitterSub != null) {
            emitterSub.unsubscribe();
            isRunning.compareAndSet(true, false);
            emitterSub = null;
        }
    }

    /**
     * The logic controlling each emitter tick.
     *
     * @return an EmittableEvents object or
     *         throw an Exception
     */
    private EmittableEvents doEmitterTick() {
        if (Util.isOnline(this.context)) {
            if (eventStore.getSize() > 0) {
                emptyCounter = 0;
                return eventStore.getEmittableEvents();
            } else {
                emptyCounter++;
                Logger.d(TAG, "EventStore empty counter: %s", emptyCounter);
                if (emptyCounter >= this.emptyLimit) {
                    Logger.d(TAG, "Emitter empty count reached.");
                    shutdown();
                    throw new EmitterException("EventStore empty limit reached.");
                }
                else {
                    throw new EmitterException("EventStore empty exception.");
                }
            }
        } else {
            shutdown();
            throw new EmitterException("Emitter is offline.");
        }
    }

    /**
     * The logic controlling what to do with the
     * returned Emitter Results.
     *
     * @param results A list of results returned
     *                from the emitter.
     */
    private void processEmitterResults(List<RequestResult> results) {
        Logger.v(TAG, "Processing emitter results.");

        int successCount = 0;
        int failureCount = 0;
        List<Long> removableEvents = new ArrayList<>();

        for (RequestResult res : results) {
            if (res.getSuccess()) {
                for (Long eventId : res.getEventIds()) {
                    removableEvents.add(eventId);
                }
                successCount += res.getEventIds().size();
            } else {
                failureCount += res.getEventIds().size();
                Logger.e(TAG, "Request sending failed; will retry later.");
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
            Logger.e(TAG, "Emitter is shutting down due to failures.");
            shutdown();
        }
    }

    /**
     * Necessary wrapper to avoid Rx issue with 'protected' functions.
     *
     * @param events the events ready to be built into requests
     * @return the list of request ready for sending
     */
    private LinkedList<ReadyRequest> buildRequestsRx(EmittableEvents events) {
        return buildRequests(events);
    }

    /**
     * Necessary wrapper to avoid Rx issue with 'protected' functions.
     *
     * @param events the events to send to the collector
     * @return a list of RequestResults
     */
    private LinkedList<RequestResult> performSyncEmitRx(LinkedList<ReadyRequest> events) {
        return performSyncEmit(events);
    }

    /**
     * Performs asynchronous sending of a list of
     * ReadyRequests.
     *
     * @param requests the requests to send
     * @return the request results
     */
    private List<RequestResult> performAsyncEmit(LinkedList<ReadyRequest> requests) {
        List<RequestResult> results = new ArrayList<>();
        List<Future<Integer>> futures = new ArrayList<>();

        for (ReadyRequest request : requests) {
            futures.add(requestFuture(request));
        }

        Logger.d(TAG, "Request Futures: %s", futures.size());

        // Get results of futures
        // - Wait up to 5 seconds for the request
        for (int i = 0; i < futures.size(); i++) {
            int code = -1;

            try {
                code = futures.get(i).get(5, TimeUnit.SECONDS);
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
    private List<Boolean> performAsyncEventRemoval(List<Long> eventIds) {
        List<Boolean> results = new ArrayList<>();
        List<Future<Boolean>> futures = new ArrayList<>();

        // Start all requests in the ThreadPool
        for (Long id : eventIds) {
            futures.add(removeFuture(id));
        }

        Logger.d(TAG, "Removal Futures: %s", futures.size());

        // Get results of futures
        for (int i = 0; i < futures.size(); i++) {
            boolean result = false;
            try {
                result = futures.get(i).get(5, TimeUnit.SECONDS);
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
     * Returns a Future to an operation to send and
     * get a RequestResult.
     *
     * @param request the request that needs to be sent
     * @return the observable function
     */
    private Future<Integer> requestFuture(ReadyRequest request) {
        return Observable.<Integer> create(subscriber -> {
            subscriber.onNext(requestSender(request.getRequest()));
            subscriber.onCompleted();
        }).subscribeOn(SchedulerRx.getScheduler()).unsubscribeOn(SchedulerRx.getScheduler())
                .toBlocking().toFuture();
    }

    /**
     * Returns a Future to an operation to remove an
     * event from the database
     *
     * @param eventId the id of the event to be removed
     * @return the observable function
     */
    private Future<Boolean> removeFuture(long eventId) {
        return Observable.<Boolean> create(subscriber -> {
            subscriber.onNext(this.eventStore.removeEvent(eventId));
            subscriber.onCompleted();
        }).subscribeOn(SchedulerRx.getScheduler()).unsubscribeOn(SchedulerRx.getScheduler())
                .toBlocking().toFuture();
    }

    // Setters, Getters and Checkers

    /**
     * @return the emitter event store
     */
    public EventStore getEventStore() {
        return this.eventStore;
    }

    /**
     * @return the emitter subscription status
     */
    public boolean getEmitterStatus() {
        return this.emitterSub != null;
    }
}
