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

import com.snowplowanalytics.snowplow.tracker.Payload;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;

public class TimingWithCategory extends Event {

    private final String category;
    private final String variable;
    private final int timing;
    private final String label;

    public static abstract class Builder<T extends Builder<T>> extends Event.Builder<T> {

        private String category;
        private String variable;
        private int timing;
        private String label;

        /**
         * @param category The category of the timed event
         */
        public T category(String category) {
            this.category = category;
            return self();
        }

        /**
         * @param variable Identify the timing being recorded
         */
        public T variable(String variable) {
            this.variable = variable;
            return self();
        }

        /**
         * @param timing The number of milliseconds in elapsed time to report
         */
        public T timing(int timing) {
            this.timing = timing;
            return self();
        }

        /**
         * @param label Optional description of this timing
         */
        public T label(String label) {
            this.label = label;
            return self();
        }

        public TimingWithCategory build() {
            return new TimingWithCategory(this);
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

    protected TimingWithCategory(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.category);
        Preconditions.checkNotNull(builder.variable);
        Preconditions.checkNotNull(builder.label);

        this.category = builder.category;
        this.variable = builder.variable;
        this.label = builder.label;
        this.timing = builder.timing;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.UT_CATEGORY, this.category);
        payload.add(Parameters.UT_VARIABLE, this.variable);
        payload.add(Parameters.UT_TIMING, this.timing);
        payload.add(Parameters.UT_LABEL, this.label);
        return payload;
    }

    /**
     * Return the payload wrapped into a SelfDescribingJson.
     *
     * @return the payload as a SelfDescribingJson.
     */
    public SelfDescribingJson getSelfDescribingJson() {
        return new SelfDescribingJson(TrackerConstants.SCHEMA_USER_TIMINGS, getPayload());
    }
}
