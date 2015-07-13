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

import com.snowplowanalytics.snowplow.tracker.Session;
import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Builds a Tracker object which is used to
 * send events to a Snowplow Collector.
 */
public class Tracker extends com.snowplowanalytics.snowplow.tracker.Tracker {

    private final static String TAG = Tracker.class.getSimpleName();
    private static ScheduledExecutorService sessionExecutor;

    public Tracker(TrackerBuilder builder) {
        super(builder);
    }

    /**
     * Begins a recurring session checker which
     * will run every 5 seconds.
     */
    protected void startSessionChecker() {
        final Session session = this.trackerSession;
        if (sessionExecutor == null) {
            sessionExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        sessionExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                session.checkAndUpdateSession();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Shuts the session checker down.
     */
    public void shutdownSessionChecker() {
        if (sessionExecutor != null) {
            sessionExecutor.shutdown();
            sessionExecutor = null;
        }
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
