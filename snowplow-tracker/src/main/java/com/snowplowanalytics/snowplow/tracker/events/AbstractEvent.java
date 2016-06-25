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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Base AbstractEvent class which contains common
 * elements to all events:
 * - Custom Context: list of custom contexts or null
 * - Timestamp: user defined event timestamp or 0
 * - AbstractEvent Id: a unique id for the event
 */
public abstract class AbstractEvent implements Event {

    private final List<SelfDescribingJson> context;
    private final String eventId;
    protected long deviceCreatedTimestamp;
    private Long trueTimestamp;

    public static abstract class Builder<T extends Builder<T>> {

        private List<SelfDescribingJson> context = new LinkedList<>();
        private String eventId = Util.getEventId();
        private long deviceCreatedTimestamp = System.currentTimeMillis();
        private Long trueTimestamp = null;

        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @param context the list of contexts
         * @return itself
         */
        public T customContext(List<SelfDescribingJson> context) {
            this.context = context;
            return self();
        }

        /**
         * A custom eventId for the event.
         *
         * @param eventId the eventId
         * @return itself
         */
        public T eventId(String eventId) {
            this.eventId = eventId;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @param timestamp the event timestamp as
         *                  unix epoch
         * @return itself
         */
        @Deprecated
        public T timestamp(long timestamp) {
            this.deviceCreatedTimestamp = timestamp;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @param deviceCreatedTimestamp the event timestamp as
         *                               unix epoch
         * @return itself
         */
        public T deviceCreatedTimestamp(long deviceCreatedTimestamp) {
            this.deviceCreatedTimestamp = deviceCreatedTimestamp;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @param trueTimestamp the true event timestamp as
         *                      unix epoch
         * @return itself
         */
        public T trueTimestamp(long trueTimestamp) {
            this.trueTimestamp = trueTimestamp;
            return self();
        }
    }

    AbstractEvent(Builder<?> builder) {

        // Precondition checks
        Preconditions.checkNotNull(builder.context);
        Preconditions.checkNotNull(builder.eventId);
        Preconditions.checkArgument(!builder.eventId.isEmpty(), "eventId cannot be empty");

        this.context = builder.context;
        this.deviceCreatedTimestamp = builder.deviceCreatedTimestamp;
        this.trueTimestamp = builder.trueTimestamp;
        this.eventId = builder.eventId;
    }

    /**
     * @return the events custom context
     */
    @Override
    public List<SelfDescribingJson> getContext() {
        return new ArrayList<>(this.context);
    }

    /**
     * @return the events timestamp
     */
    @Override
    public long getDeviceCreatedTimestamp() {
        return this.deviceCreatedTimestamp;
    }

    /**
     * @return the optional true events timestamp
     */
    @Override
    public long getTrueTimestamp() {
        return this.trueTimestamp;
    }

    /**
     * @return the event id
     */
    @Override
    public String getEventId() {
        return this.eventId;
    }

    /**
     * @return the event payload
     */
    @Override
    public abstract Payload getPayload();

    /**
     * Adds the default parameters to a TrackerPayload object.
     *
     * @param payload the payload to add too.
     * @return the TrackerPayload with appended values.
     */
    TrackerPayload putDefaultParams(TrackerPayload payload) {
        payload.add(Parameters.EID, getEventId());
        payload.add(Parameters.DEVICE_TIMESTAMP, Long.toString(getDeviceCreatedTimestamp()));
        if (this.trueTimestamp != null) {
            payload.add(Parameters.TRUE_TIMESTAMP, Long.toString(getTrueTimestamp()));
        }
        return payload;
    }
}
