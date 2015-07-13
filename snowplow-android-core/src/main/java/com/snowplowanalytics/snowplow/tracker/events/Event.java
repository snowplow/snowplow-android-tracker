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

package com.snowplowanalytics.snowplow.tracker.events;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.List;

/**
 * Base Event class which contains common
 * elements to all events:
 * - Custom Context: list of custom contexts or null
 * - Timestamp: user defined event timestamp or 0
 */
public class Event {

    private final List<SelfDescribingJson> context;
    private final long timestamp;

    public static abstract class Builder<T extends Builder<T>> {

        private List<SelfDescribingJson> context = null;
        private long timestamp = 0;

        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @param context the list of contexts
         */
        public T customContext(List<SelfDescribingJson> context) {
            this.context = context;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @param timestamp the event timestamp as
         *                  unix epoch
         */
        public T timestamp(long timestamp) {
            this.timestamp = timestamp;
            return self();
        }

        public Event build() {
            return new Event(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    protected Event(Builder<?> builder) {
        this.context = builder.context;
        this.timestamp = builder.timestamp;
    }

    /**
     * @return the events custom context
     */
    public List<SelfDescribingJson> getContext() {
        return this.context;
    }

    /**
     * @return the events timestamp
     */
    public long getTimestamp() {
        return this.timestamp;
    }
}
