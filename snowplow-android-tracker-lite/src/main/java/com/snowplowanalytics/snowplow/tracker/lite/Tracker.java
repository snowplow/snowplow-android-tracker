package com.snowplowanalytics.snowplow.tracker.lite;

import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.List;

public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final static String TAG = Tracker.class.getSimpleName();

    public Tracker(TrackerBuilder builder) {
        super(builder);
    }

    @Override
    public void trackPageView(final String pageUrl, final String pageTitle, final String referrer,
                              final List<SelfDescribingJson> context, final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackPageView(pageUrl, pageTitle, referrer, context, timestamp);
            }
        });
    }

    @Override
    public void trackStructuredEvent(final String category, final String action, final String label,
                                     final String property, final Double value,
                                     final List<SelfDescribingJson> context, final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackStructuredEvent(category, action, label, property, value, context, timestamp);
            }
        });
    }

    @Override
    public void trackUnstructuredEvent(final SelfDescribingJson eventData,
                                       final List<SelfDescribingJson> context, final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackUnstructuredEvent(eventData, context, timestamp);
            }
        });
    }

    @Override
    public void trackEcommerceTransaction(final String order_id, final Double total_value, final String affiliation,
                                          final Double tax_value, final Double shipping, final String city,
                                          final String state, final String country, final String currency,
                                          final List<TransactionItem> items, final List<SelfDescribingJson> context,
                                          final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackEcommerceTransaction(order_id, total_value, affiliation,
                        tax_value, shipping, city, state, country, currency, items, context,
                        timestamp);
            }
        });
    }

    @Override
    public void trackScreenView(final String name, final String id, final List<SelfDescribingJson> context,
                                final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackScreenView(name, id, context, timestamp);
            }
        });
    }

    @Override
    public void trackTimingWithCategory(final String category, final String variable, final int timing,
                                        final String label, final List<SelfDescribingJson> context,
                                        final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.trackTimingWithCategory(category, variable, timing, label, context,
                        timestamp);
            }
        });
    }
}
