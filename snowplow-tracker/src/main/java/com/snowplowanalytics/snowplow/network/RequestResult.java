/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.network;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Stores the result of a Request Attempt
 */
public class RequestResult {

    private final int statusCode;
    private final boolean oversize;
    private final List<Long> eventIds;

    /**
     * Builds a result from a request attempt.
     *
     * @param statusCode HTTP status code from Collector response
     * @param oversize was the request oversize
     * @param eventIds a list of event ids involved in the sending
     */
    public RequestResult(int statusCode, boolean oversize, @NonNull List<Long> eventIds) {
        this.statusCode = statusCode;
        this.oversize = oversize;
        this.eventIds = eventIds;
    }

    /**
     * @return the requests success status
     */
    public boolean isSuccessful() {
        return this.statusCode >= 200 && this.statusCode < 300;
    }

    /**
     * @return whether the request was oversize
     */
    public boolean isOversize() {
        return this.oversize;
    }

    /**
     * @return HTTP status code from Collector
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the requests LinkedList of eventIds
     */
    @NonNull
    public List<Long> getEventIds() {
        return this.eventIds;
    }

    public boolean shouldRetry(Map<Integer, Boolean> customRetryForStatusCodes) {
        // don't retry if successful
        if (isSuccessful()) {
            return false;
        }

        // don't retry if request is larger than max byte limit
        if (isOversize()) {
            return false;
        }

        // status code has a custom retry rule
        if (customRetryForStatusCodes.containsKey(statusCode)) {
            return Objects.requireNonNull(customRetryForStatusCodes.get(statusCode));
        }

        // retry if status code is not in the list of no-retry status codes
        Set<Integer> dontRetryStatusCodes = new HashSet<>(Arrays.asList(400, 401, 403, 410, 422));
        return !dontRetryStatusCodes.contains(statusCode);
    }
}
