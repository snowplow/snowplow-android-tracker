/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import android.support.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
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

    private final List<SelfDescribingJson> customContexts;
    private final String eventId;
    protected long deviceCreatedTimestamp;
    private Long trueTimestamp;

    public static abstract class Builder<T extends Builder<T>> {

        private List<SelfDescribingJson> customContexts = new LinkedList<>();
        private String eventId = Util.getUUIDString();
        private long deviceCreatedTimestamp = System.currentTimeMillis();
        private Long trueTimestamp = null;

        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @deprecated As of release 1.4.2, replaced by {@link #contexts}
         *
         * @param contexts the list of contexts
         * @return itself
         */
        @Deprecated
        public T customContext(List<SelfDescribingJson> contexts) {
            return contexts(contexts);
        }

        /**
         * Adds a list of custom contexts.
         *
         * @param contexts the list of contexts
         * @return itself
         */
        public T contexts(List<SelfDescribingJson> contexts) {
            this.customContexts = contexts;
            return self();
        }

        /**
         * A custom eventId for the event.
         *
         * @deprecated As of release 1.4.2, it will be removed in the version 2.0.0.
         * The eventId can be specified only by the tracker.
         *
         * @param eventId the eventId
         * @return itself
         */
        @Deprecated
        public T eventId(String eventId) {
            this.eventId = eventId;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @deprecated As of release 1.0.0, replaced by {@link #trueTimestamp(long trueTimestamp)}
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
         * @deprecated As of release 1.4.2, it will be removed in the version 2.0.0.
         *
         * @param deviceCreatedTimestamp the event timestamp as
         *                               unix epoch
         * @return itself
         */
        @Deprecated
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

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    protected AbstractEvent() {
        this(new Builder2());
    }

    AbstractEvent(Builder<?> builder) {

        // Precondition checks
        Preconditions.checkNotNull(builder.customContexts);
        Preconditions.checkNotNull(builder.eventId);
        Preconditions.checkArgument(!builder.eventId.isEmpty(), "eventId cannot be empty");

        this.customContexts = builder.customContexts;
        this.deviceCreatedTimestamp = builder.deviceCreatedTimestamp;
        this.trueTimestamp = builder.trueTimestamp;
        this.eventId = builder.eventId;
    }

    /**
     * @deprecated As of release 1.4.2, replaced by {@link #getContexts()}
     *
     * @return the events custom context
     */
    @Override
    @Deprecated
    public @NonNull List<SelfDescribingJson> getContext() {
        return getContexts();
    }

    /**
     * @return the events custom context
     */
    @Override
    public @NonNull List<SelfDescribingJson> getContexts() {
        return new ArrayList<>(this.customContexts);
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
    public Long getTrueTimestamp() {
        return this.trueTimestamp;
    }

    /**
     * @return the event id
     */
    @Override
    public @NonNull String getEventId() {
        return this.eventId;
    }

    /**
     * Adds the default parameters to a TrackerPayload object.
     *
     * @deprecated As of release 1.4.2, it will be removed in the version 2.0.0
     *
     * @param payload the payload to add too.
     * @return the TrackerPayload with appended values.
     */
    @Deprecated
    @NonNull TrackerPayload putDefaultParams(@NonNull TrackerPayload payload) {
        payload.add(Parameters.EID, getEventId());
        payload.add(Parameters.DEVICE_TIMESTAMP, Long.toString(getDeviceCreatedTimestamp()));
        if (this.trueTimestamp != null) {
            payload.add(Parameters.TRUE_TIMESTAMP, Long.toString(getTrueTimestamp()));
        }
        return payload;
    }

    @Override
    public void beginProcessing(Tracker tracker) {}

    @Override
    public void endProcessing(Tracker tracker) {}
}
