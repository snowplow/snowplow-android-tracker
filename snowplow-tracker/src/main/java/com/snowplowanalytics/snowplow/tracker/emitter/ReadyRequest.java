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

package com.snowplowanalytics.snowplow.tracker.emitter;

import okhttp3.Request;

import java.util.LinkedList;

/**
 * Class to house a request that is ready
 * to be sent.  Allows for easier abstraction
 * between synchronous and asynchronous
 * sending.
 */
public class ReadyRequest {
    private final boolean oversize;
    private final Request request;
    private final LinkedList<Long> ids;

    /**
     * Constructs a ReadyRequest
     *
     * @param oversize if the request is over-sized
     * @param request the request to send
     * @param ids the ids associated with the request
     */
    public ReadyRequest(boolean oversize, Request request, LinkedList<Long> ids) {
        this.oversize = oversize;
        this.request = request;
        this.ids = ids;
    }

    /**
     * @return the request for sending
     */
    public Request getRequest() {
        return this.request;
    }

    /**
     * @return the list of event ids
     */
    public LinkedList<Long> getEventIds() {
        return this.ids;
    }

    /**
     * @return if the request is over-sized
     */
    public boolean isOversize() {
        return this.oversize;
    }
}
