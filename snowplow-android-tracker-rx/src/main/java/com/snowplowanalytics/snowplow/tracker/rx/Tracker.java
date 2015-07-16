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
 * Builds a Tracker object which is used to
 * send events to a Snowplow Collector.
 */
public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final String TAG = Tracker.class.getSimpleName();
    private final Scheduler scheduler = SchedulerRx.getScheduler();
    private Subscription sessionSub;

    public Tracker(TrackerBuilder builder) {
        super(builder);
    }

    /**
     * Begins a recurring session checker which
     * will run every 5 seconds.
     */
    protected void startSessionChecker(final long interval) {
        final Session session = this.trackerSession;
        sessionSub = Observable.interval(interval, TimeUnit.MILLISECONDS, Schedulers.newThread())
            .doOnError(err -> Logger.e(TAG, "Error checking session: %s", err))
            .retry()
            .doOnSubscribe(() -> Logger.d(TAG, "Session checker has been started."))
            .doOnUnsubscribe(() -> Logger.d(TAG, "Session checker has been shutdown."))
            .subscribe(tick -> session.checkAndUpdateSession());
    }

    /**
     * Shuts the session checker down.
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

    protected void trackEcommerceItem(EcommerceTransactionItem event, long timestamp) {
        Observable.create(subscriber -> {
            super.track(event, timestamp);
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
