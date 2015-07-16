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
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransaction;
import com.snowplowanalytics.snowplow.tracker.events.EcommerceTransactionItem;
import com.snowplowanalytics.snowplow.tracker.events.PageView;
import com.snowplowanalytics.snowplow.tracker.events.ScreenView;
import com.snowplowanalytics.snowplow.tracker.events.Structured;
import com.snowplowanalytics.snowplow.tracker.events.TimingWithCategory;
import com.snowplowanalytics.snowplow.tracker.events.Unstructured;

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
     *
     * @param interval the checking interval
     */
    protected void startSessionChecker(final long interval) {
        final Session session = this.trackerSession;
        if (sessionExecutor == null) {
            sessionExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        sessionExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                session.checkAndUpdateSession();
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
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
    public void track(final PageView event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }

    @Override
    public void track(final Structured event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }

    @Override
    public void track(final Unstructured event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }

    @Override
    public void track(final EcommerceTransaction event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }

    protected void trackEcommerceItem(final EcommerceTransactionItem event, final long timestamp) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event, timestamp);
            }
        });
    }

    @Override
    public void track(final ScreenView event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }

    @Override
    public void track(final TimingWithCategory event) {
        Executor.execute(new Runnable() {
            public void run() {
                Tracker.super.track(event);
            }
        });
    }
}
