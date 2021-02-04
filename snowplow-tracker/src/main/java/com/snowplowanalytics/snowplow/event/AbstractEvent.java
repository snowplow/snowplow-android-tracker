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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.internal.utils.Util;

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

    public final List<SelfDescribingJson> customContexts;
    @Nullable
    public Long trueTimestamp;

    public static abstract class Builder<T extends Builder<T>> {

        private final List<SelfDescribingJson> customContexts = new LinkedList<>();
        private Long trueTimestamp;

        @NonNull
        protected abstract T self();

        /**
         * Adds a list of custom contexts.
         *
         * @param contexts the list of contexts
         * @return itself
         */
        @NonNull
        public T contexts(@NonNull List<SelfDescribingJson> contexts) {
            customContexts.addAll(contexts);
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
        @NonNull
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

        this.customContexts = builder.customContexts;
        this.trueTimestamp = builder.trueTimestamp;
    }

    // Builder methods

    @NonNull
    public AbstractEvent contexts(@Nullable List<SelfDescribingJson> contexts) {
        if (contexts != null) customContexts.addAll(contexts);
        return this;
    }

    @NonNull
    public AbstractEvent trueTimestamp(@Nullable Long trueTimestamp) {
        this.trueTimestamp = trueTimestamp;
        return this;
    }

    // Public methods

    /**
     * @return the event custom context
     */
    @Override
    public @NonNull List<SelfDescribingJson> getContexts() {
        return new ArrayList<>(this.customContexts);
    }

    /**
     * @return the optional true events timestamp
     */
    @Override
    @Nullable
    public Long getTrueTimestamp() {
        return this.trueTimestamp;
    }

    @Override
    public void beginProcessing(@NonNull Tracker tracker) {}

    @Override
    public void endProcessing(@NonNull Tracker tracker) {}
}
