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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import java.util.HashMap;
import java.util.Map;

public class Timing extends AbstractSelfDescribing {

    private final String category;
    private final String variable;
    private final Integer timing;
    private final String label;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String category;
        private String variable;
        private Integer timing;
        private String label;

        /**
         * @param category The category of the timed event
         * @return itself
         */
        @NonNull
        public T category(@NonNull String category) {
            this.category = category;
            return self();
        }

        /**
         * @param variable Identify the timing being recorded
         * @return itself
         */
        @NonNull
        public T variable(@NonNull String variable) {
            this.variable = variable;
            return self();
        }

        /**
         * @param timing The number of milliseconds in elapsed time to report
         * @return itself
         */
        @NonNull
        public T timing(@NonNull Integer timing) {
            this.timing = timing;
            return self();
        }

        /**
         * @param label Optional description of this timing
         * @return itself
         */
        @NonNull
        public T label(@NonNull String label) {
            this.label = label;
            return self();
        }

        @NonNull
        public Timing build() {
            return new Timing(this);
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

    protected Timing(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.category);
        Preconditions.checkNotNull(builder.timing);
        Preconditions.checkNotNull(builder.variable);
        Preconditions.checkArgument(!builder.category.isEmpty(), "category cannot be empty");
        Preconditions.checkArgument(!builder.variable.isEmpty(), "variable cannot be empty");

        this.category = builder.category;
        this.variable = builder.variable;
        this.label = builder.label;
        this.timing = builder.timing;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @deprecated As of release 1.5.0, it will be removed in version 2.0.0.
     * replaced by {@link #getDataPayload()}.
     *
     * @return the payload to be sent.
     */
    @Deprecated
    @NonNull
    public HashMap<String,Object> getData() {
        HashMap<String,Object> payload = new HashMap<>();
        payload.put(Parameters.UT_CATEGORY, this.category);
        payload.put(Parameters.UT_VARIABLE, this.variable);
        payload.put(Parameters.UT_TIMING, this.timing);
        if (this.label != null && !this.label.isEmpty()) {
            payload.put(Parameters.UT_LABEL, this.label);
        }
        return payload;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return getData();
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_USER_TIMINGS;
    }
}
