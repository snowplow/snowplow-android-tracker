package com.snowplowanalytics.snowplow.tracker;

import android.content.Context;

public class LiteEventStore extends EventStore {

    public LiteEventStore(Context context) {
        super(context);
    }

    public void add(final Payload payload) {
        Executor.executor.execute(new Runnable() {
            public void run() {
                insertEvent(payload);
            }
        });
    }
}
