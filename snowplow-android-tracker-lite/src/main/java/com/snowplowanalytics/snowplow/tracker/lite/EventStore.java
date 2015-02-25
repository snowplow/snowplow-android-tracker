package com.snowplowanalytics.snowplow.tracker.lite;

import android.content.Context;

import com.snowplowanalytics.snowplow.tracker.Payload;

public class EventStore extends com.snowplowanalytics.snowplow.tracker.EventStore {

    public EventStore(Context context) {
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
