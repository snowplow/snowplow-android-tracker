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

package com.snowplowanalytics.snowplow.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.Scheduler;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;

public class RxEventStore extends EventStore {

    private String TAG = RxEventStore.class.getSimpleName();

    private final Scheduler scheduler = Schedulers.io();

    public RxEventStore(Context context) {
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
