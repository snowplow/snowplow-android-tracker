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

package com.snowplowanalytics.snowplow.tracker;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.EmitterException;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class RxEmitter extends Emitter {

    private final String TAG = RxEmitter.class.getSimpleName();

    private final Scheduler scheduler = Schedulers.io();

    // TODO: Replace isRunning with a blocking state on the emitter process
    private boolean isRunning = false;
    private int emptyCounter = 0;

    private Subscription emitterSub;
    private RxEventStore eventStore;

    protected RxEmitter(EmitterBuilder builder) {
        super(builder);

        // Create the event store with the context and the buffer option
        this.eventStore = new RxEventStore(this.context);

        // If the device is not online do not send anything!
        if (isOnline() && eventStore.getSize() > 0) {
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
        if (emitterSub == null && isOnline()) {
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
        emitterSub = Observable.interval(TrackerConstants.EMITTER_TICK, TimeUnit.SECONDS)
            .map((tick) -> {
                if (!isRunning) {
                    if (eventStore.getSize() == 0) {
                        emptyCounter++;

                        Logger.ifDebug(TAG, "EventStore empty counter: %s", emptyCounter);

                        if (emptyCounter >= TrackerConstants.EMITTER_EMPTY_EVENTS_LIMIT) {
                            shutdown();
                            throw new EmitterException("EventStore empty exception - limit");
                        }
                        throw new EmitterException("EventStore empty exception");
                    }
                    else {
                        emptyCounter = 0;
                        isRunning = true;
                        return eventStore.getEmittableEvents();
                    }
                }
                else {
                    throw new EmitterException("Emitter concurrency exception");
                }
            })
            .doOnError((err) -> Logger.ifDebug(TAG, "Emitter Error: %s", err.toString()))
            .retry()
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .doOnSubscribe(() -> Logger.ifDebug(TAG, "Emitter has been started!"))
            .doOnUnsubscribe(() -> Logger.ifDebug(TAG, "Emitter has been shutdown!"))
            .flatMap(this::emitEvent)
            .subscribe(results -> {

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
                if (failureCount != 0) {
                    if (isOnline()) {
                        Logger.ifDebug(TAG, "Check your collector path: %s",
                                getEmitterUri());
                    }
                    shutdown();
                }

                // Send the callback
                if (requestCallback != null) {
                    if (failureCount != 0) {
                        requestCallback.onFailure(successCount, failureCount);
                    } else {
                        requestCallback.onSuccess(successCount);
                    }
                }

                // Reset isRunning after completion
                isRunning = false;
            });
    }

    /**
     * Shuts the emitter down!
     */
    public void shutdown() {
        if (emitterSub != null) {
            emitterSub.unsubscribe();
            emitterSub = null;
        }
    }

    /**
     * Emits all the events in the EmittableEvents
     * object.
     *
     * @return Observable that will emit once containing
     * the request results.
     */
    private Observable<LinkedList<RequestResult>> emitEvent(final EmittableEvents events) {
        return Observable
            .just(events)
            .map(this::performEmit)
            .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
    }


    // Setters, Getters and Checkers

    /**
     * @return the emitter subscription status
     */
    public boolean getEmitterSubscriptionStatus() {
        return this.emitterSub != null;
    }



}
