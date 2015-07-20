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

import com.snowplowanalytics.snowplow.tracker.Session;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransaction;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.TimingWithCategory;
import com.snowplowanalytics.snowplow.tracker.events.Unstructured;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * Builds a Tracker object which is used to build and send events
 * to a Snowplow Collector.
 */
public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final String TAG = Tracker.class.getSimpleName();
    private final Scheduler scheduler;
    private Subscription sessionSub;

    /**
     * Constructs a Tracker object.
     *
     * @param builder the base tracker builder
     */
    public Tracker(TrackerBuilder builder) {
        super(builder);

        // Setup the Scheduler
        SchedulerRx.setThreadCount(this.threadCount);
        scheduler = SchedulerRx.getScheduler();

        // Start Checking Sessions
        startSessionChecker(this.sessionCheckInterval);
    }

    /**
     * Starts a polling session checker subscription to
     * run at a defined interval.
     *
     * @param interval the time between checks
     */
    public void startSessionChecker(final long interval) {
        if (sessionSub == null) {
            final Session session = this.trackerSession;
            sessionSub = Observable.interval(interval, TimeUnit.MILLISECONDS, scheduler)
                    .doOnError(err -> Logger.e(TAG, "Error checking session: %s", err))
                    .retry()
                    .doOnSubscribe(() -> Logger.d(TAG, "Session checker has been started."))
                    .doOnUnsubscribe(() -> Logger.d(TAG, "Session checker has been shutdown."))
                    .subscribe(tick -> session.checkAndUpdateSession());
        }
    }

    /**
     * Ends the polling session checker subscription.
     */
    public void shutdownSessionChecker() {
        if (sessionSub != null) {
            sessionSub.unsubscribe();
            sessionSub = null;
        }
    }

    @Override
    public void track(PageView event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void track(Structured event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void track(Unstructured event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void track(EcommerceTransaction event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void track(ScreenView event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void track(TimingWithCategory event) {
        Observable.create(subscriber -> {
            super.track(event);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }
}
