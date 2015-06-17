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

import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.List;

import rx.Observable;
import rx.Scheduler;

/**
 * Builds a Tracker object which is used to
 * send events to a Snowplow Collector.
 */
public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final Scheduler scheduler = SchedulerRx.getScheduler();

    public Tracker(TrackerBuilder builder) {
        super(builder);
    }

    @Override
    public void trackPageView(String pageUrl, String pageTitle, String referrer,
                              List<SelfDescribingJson> context, long timestamp) {
        Observable.create(subscriber -> {
            super.trackPageView(pageUrl, pageTitle, referrer, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void trackStructuredEvent(String category, String action, String label, String property,
                                 Double value, List<SelfDescribingJson> context, long timestamp) {
        Observable.create(subscriber -> {
            super.trackStructuredEvent(category, action, label, property, value, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void trackUnstructuredEvent(SelfDescribingJson eventData, List<SelfDescribingJson> context,
                                       long timestamp) {
        Observable.create(subscriber -> {
            super.trackUnstructuredEvent(eventData, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void trackEcommerceTransaction(String order_id, Double total_value, String affiliation,
                                          Double tax_value, Double shipping, String city,
                                          String state, String country, String currency,
                                          List<TransactionItem> items, List<SelfDescribingJson> context,
                                          long timestamp) {
        Observable.create(subscriber -> {
            super.trackEcommerceTransaction(order_id, total_value, affiliation, tax_value, shipping,
                    city, state, country, currency, items, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void trackScreenView(String name, String id, List<SelfDescribingJson> context,
                                long timestamp) {
        Observable.create(subscriber -> {
            super.trackScreenView(name, id, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }

    @Override
    public void trackTimingWithCategory(String category, String variable, int timing, String label,
                                        List<SelfDescribingJson> context, long timestamp) {
        Observable.create(subscriber -> {
            super.trackTimingWithCategory(category, variable, timing, label, context, timestamp);
            subscriber.onCompleted();
        }).subscribeOn(scheduler).unsubscribeOn(scheduler).subscribe();
    }
}
