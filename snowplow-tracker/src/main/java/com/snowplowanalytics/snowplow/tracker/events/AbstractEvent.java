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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Base AbstractEvent class which contains common
 * elements to all events:
 * - Custom Context: list of custom contexts or null
 * - Timestamp: user defined event timestamp or 0
 * - AbstractEvent Id: a unique id for the event
 */
public abstract class AbstractEvent implements Event {

    protected final List<SelfDescribingJson> customContexts;
    private String eventId;
    protected Long deviceCreatedTimestamp;
    private Long trueTimestamp;

    public static abstract class Builder<T extends Builder<T>> {

        private List<SelfDescribingJson> customContexts = new LinkedList<>();
        private String eventId;
        private Long deviceCreatedTimestamp;
        private Long trueTimestamp;

        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @deprecated As of release 1.5.0, replaced by {@link #contexts}
         *
         * @param contexts the list of contexts
         * @return itself
         */
        @Deprecated
        @NonNull
        public T customContext(@NonNull List<SelfDescribingJson> contexts) {
            return contexts(contexts);
        }

        /**
         * Adds a list of custom contexts.
         *
         * @param contexts the list of contexts
         * @return itself
         */
        @NonNull
        public T contexts(@NonNull List<SelfDescribingJson> contexts) {
            this.customContexts = contexts;
            return self();
        }

        /**
         * A custom eventId for the event.
         *
         * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
         * The eventId can be specified only by the tracker.
         *
         * @param eventId the eventId
         * @return itself
         */
        @Deprecated
        @NonNull
        public T eventId(@NonNull String eventId) {
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
        @NonNull
        public T timestamp(long timestamp) {
            this.deviceCreatedTimestamp = timestamp;
            return self();
        }

        /**
         * A custom event timestamp.
         *
         * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
         *
         * @param deviceCreatedTimestamp the event timestamp as
         *                               unix epoch
         * @return itself
         */
        @Deprecated
        @NonNull
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
        @NonNull
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
        if (builder.eventId != null) {
            Preconditions.checkArgument(!builder.eventId.isEmpty(), "eventId cannot be empty");
            try {
                UUID.fromString(builder.eventId);
            } catch(IllegalArgumentException e) {
                Preconditions.fail("eventId has to be a valid UUID");
            }
            eventId = builder.eventId;
        }

        this.customContexts = builder.customContexts;
        this.deviceCreatedTimestamp = builder.deviceCreatedTimestamp;
        this.trueTimestamp = builder.trueTimestamp;
    }

    /**
     * @deprecated As of release 1.5.0, replaced by {@link #getContexts()}
     *
     * @return the event custom context
     */
    @Override
    @Deprecated
    public @NonNull List<SelfDescribingJson> getContext() {
        return getContexts();
    }

    /**
     * @return the event custom context
     */
    @Override
    public @NonNull List<SelfDescribingJson> getContexts() {
        return new ArrayList<>(this.customContexts);
    }

    /**
     * Get the timestamp of the event.
     * @apiNote If the timestamp is not set, it sets one as a side effect.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event timestamp
     */
    @Override
    @Deprecated
    public long getDeviceCreatedTimestamp() {
        if (deviceCreatedTimestamp == null) {
            deviceCreatedTimestamp = System.currentTimeMillis();
        }
        return deviceCreatedTimestamp;
    }

    /**
     * Get the actual timestamp of the event.
     * @apiNote It doesn't have the side effect of {@link #getDeviceCreatedTimestamp()}.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event timestamp
     */
    @Override
    @Deprecated
    @Nullable
    public Long getActualDeviceCreatedTimestamp() {
        return deviceCreatedTimestamp;
    }

    /**
     * @return the optional true events timestamp
     */
    @Override
    @Nullable
    public Long getTrueTimestamp() {
        return this.trueTimestamp;
    }

    /**
     * Get the event id of the event.
     * @apiNote If the eventId is not set, it sets one as a side effect.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event id
     */
    @Override
    @Deprecated
    @NonNull
    public String getEventId() {
        if (eventId == null) {
            eventId = Util.getUUIDString();
        }
        return eventId;
    }

    /**
     * Get the actual event id of the event.
     * @apiNote It doesn't have the side effect of {@link #getEventId()}.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event id if it exist.
     */
    @Override
    @Deprecated
    @NonNull
    public String getActualEventId() {
        return eventId;
    }

    /**
     * Adds the default parameters to a TrackerPayload object.
     *
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0
     *
     * @param payload the payload to add too.
     * @return the TrackerPayload with appended values.
     */
    @Deprecated
    @NonNull
    TrackerPayload putDefaultParams(@NonNull TrackerPayload payload) {
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
