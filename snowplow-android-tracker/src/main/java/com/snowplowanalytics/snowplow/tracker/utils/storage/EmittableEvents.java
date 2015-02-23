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

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * An object containing currently Pending
 * events and their associated eventIds.
 */
public class EmittableEvents {

    private final ArrayList<Payload> events;
    private final LinkedList<Long> eventIds;

    public EmittableEvents(ArrayList<Payload> events, LinkedList<Long> eventIds) {
        this.events = events;
        this.eventIds = eventIds;
    }

    /**
     * @return the objects ArrayList of payloads
     */
    public ArrayList<Payload> getEvents() {
        return this.events;
    }

    /**
     * @return the object LinkedList of event ids
     */
    public LinkedList<Long> getEventIds() {
        return this.eventIds;
    }
}
