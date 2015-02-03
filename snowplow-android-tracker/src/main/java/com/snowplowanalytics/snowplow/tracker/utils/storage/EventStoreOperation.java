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

package com.snowplowanalytics.snowplow.tracker.utils.storage;

import com.snowplowanalytics.snowplow.tracker.Payload;

/**
 * An object for all addition and removal
 * of events to the database.
 */
public class EventStoreOperation {

    private final Payload payload;
    private final boolean isAdd;
    private final long eventId;

    public EventStoreOperation(Payload payload, boolean isAdd, Long eventId) {
        this.payload = payload;
        this.isAdd = isAdd;
        this.eventId = eventId;
    }

    public Payload getPayload() {
        return this.payload;
    }

    public boolean getIsAdd() {
        return this.isAdd;
    }

    public long getEventId() {
        return this.eventId;
    }
}
