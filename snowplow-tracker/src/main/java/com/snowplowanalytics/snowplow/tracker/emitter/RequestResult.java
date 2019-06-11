/*
 * Copyright (c) 2015-2019 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.emitter;

import java.util.LinkedList;

/**
 * Stores the result of a Request Attempt
 */
public class RequestResult {

    private final boolean success;
    private final LinkedList<Long> eventIds;

    /**
     * Builds a result from a request attempt.
     *
     * @param success if the event returned a 200
     * @param eventIds a list of event ids involved in the sending
     */
    public RequestResult(boolean success, LinkedList<Long> eventIds) {
        this.success = success;
        this.eventIds = eventIds;
    }

    /**
     * @return the requests success status
     */
    public boolean getSuccess() {
        return this.success;
    }

    /**
     * @return the requests LinkedList of eventIds
     */
    public LinkedList<Long> getEventIds() {
        return this.eventIds;
    }
}
