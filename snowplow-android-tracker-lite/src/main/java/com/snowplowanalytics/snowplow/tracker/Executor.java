package com.snowplowanalytics.snowplow.tracker;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class Executor {

    static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
}
