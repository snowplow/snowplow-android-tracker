package com.snowplowanalytics.snowplow.tracker.rx;

import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final Executor executor = Executors.newSingleThreadScheduledExecutor();
    private final Scheduler scheduler = Schedulers.from(executor);

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
