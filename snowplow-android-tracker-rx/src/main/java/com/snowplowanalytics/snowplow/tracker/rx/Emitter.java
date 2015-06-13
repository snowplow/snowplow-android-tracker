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

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.EmitterException;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class Emitter extends com.snowplowanalytics.snowplow.tracker.Emitter {

    private final String TAG = Emitter.class.getSimpleName();
    private final Scheduler scheduler = SchedulerRx.getScheduler();
    private int emptyCounter = 0;
    private Subscription emitterSub;
    private EventStore eventStore;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public Emitter(EmitterBuilder builder) {
        super(builder);

        // Create the event store with the context and the buffer option
        this.eventStore = new EventStore(this.context, this.sendLimit);

        // If the device is not online do not send anything!
        if (isOnline() && eventStore.getSize() > 0) {
            isRunning.compareAndSet(false, true);
            start();
        }
    }

    /**
     * @param payload the payload to be added to
     *                the EventStore
     */
    public void add(Payload payload) {

        // Adds the payload to the EventStore
        eventStore.add(payload);

        // If the emitter is currently shutdown start it..
        if (isRunning.compareAndSet(false, true) && isOnline()) {
            start();
        }
    }

    /**
     * Starts the emitter if it is currently
     * shutdown.
     */
    public void flush() {
        if (isRunning.compareAndSet(false, true) && isOnline()) {
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
        emitterSub = Observable.interval(this.emitterTick, TimeUnit.SECONDS)
            .map((tick) -> doEmitterTick())
            .doOnError(err -> Logger.e(TAG, "Emitter Error: %s", err.toString()))
            .retry()
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .doOnSubscribe(() -> Logger.d(TAG, "Emitter has been started."))
            .doOnUnsubscribe(() -> Logger.d(TAG, "Emitter has been shutdown."))
            .flatMap(this::emitEvent)
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
        if (eventStore.getSize() == 0) {
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
        } else {
            emptyCounter = 0;
            return eventStore.getEmittableEvents();
        }
    }

    /**
     * The logic controlling what to do with the
     * returned Emitter Results.
     *
     * @param results A list of results returned
     *                from the emitter.
     */
    private void processEmitterResults(LinkedList<RequestResult> results) {
        Logger.v(TAG, "Processing emitter results.");

        // Start counting successes and failures
        int successCount = 0;
        int failureCount = 0;

        for (RequestResult res : results) {
            if (res.getSuccess()) {
                for (Long eventId : res.getEventIds()) {
                    eventStore.removeEvent(eventId);
                }
                successCount += res.getEventIds().size();
            } else {
                failureCount += res.getEventIds().size();
                Logger.e(TAG, "Request sending failed; will retry later.");
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
    }

    /**
     * Emits all the events in the Emittable Events object.
     *
     * @return Observable that will emit once containing
     * the request results.
     */
    private Observable<LinkedList<RequestResult>> emitEvent(final EmittableEvents events) {
        return Observable
            .just(events)
            .map(this::performEmitRx)
            .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
    }

    /**
     * Necessary wrapper to avoid Rx issue with 'protected' functions.
     *
     * @param events the events to send to the collector
     * @return a list of RequestResults
     */
    private LinkedList<RequestResult> performEmitRx(EmittableEvents events) {
        return performEmit(events);
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
