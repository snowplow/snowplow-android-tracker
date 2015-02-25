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

import android.content.Context;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.Scheduler;

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;

public class EventStore extends com.snowplowanalytics.snowplow.tracker.EventStore {

    private String TAG = EventStore.class.getSimpleName();

    private final Scheduler scheduler = Schedulers.io();

    public EventStore(Context context) {
        super(context);
    }

    /**
     * Creates a new subscription to an observable
     * operation which is loaded into a buffered
     * queue.
     *
     * @param payload the event payload that is
     *                being added.
     */
    public void add(Payload payload) {
        addObservable(payload)
            .subscribeOn(scheduler)
            .unsubscribeOn(scheduler)
            .subscribe();
    }

    /**
     * An observable object for inserting an event
     * into the database.
     *
     * @param payload the event payload that is
     *                being added.
     * @return an observable event add
     */
    private Observable<Long> addObservable(final Payload payload) {
        return Observable.<Long>create(subscriber -> {
            subscriber.onNext(insertEvent(payload));
            subscriber.onCompleted();
        })
        .onBackpressureBuffer(TrackerConstants.BACK_PRESSURE_LIMIT);
    }
}
