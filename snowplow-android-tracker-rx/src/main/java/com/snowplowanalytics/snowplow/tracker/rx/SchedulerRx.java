package com.snowplowanalytics.snowplow.tracker.rx;

import rx.schedulers.Schedulers;
import rx.Scheduler;

/**
 * Creates a single Scheduler for use
 * by the Tracker and Emitter.
 */
public class SchedulerRx {

    private static final Scheduler scheduler = Schedulers.io();

    public static Scheduler getScheduler() {
        return scheduler;
    }
}
