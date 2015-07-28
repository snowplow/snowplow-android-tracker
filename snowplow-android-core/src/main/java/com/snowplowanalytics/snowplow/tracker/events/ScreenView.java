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
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public class ScreenView extends Event {

    private final String name;
    private final String id;

    public static abstract class Builder<T extends Builder<T>> extends Event.Builder<T> {

        private String name;
        private String id;

        /**
         * @param name The name of the screen view event
         * @return itself
         */
        public T name(String name) {
            this.name = name;
            return self();
        }

        /**
         * @param id Screen view ID
         * @return itself
         */
        public T id(String id) {
            this.id = id;
            return self();
        }

        public ScreenView build() {
            return new ScreenView(this);
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

    protected ScreenView(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkArgument(builder.name != null || builder.id != null);

        this.name = builder.name;
        this.id = builder.id;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.SV_NAME, this.name);
        payload.add(Parameters.SV_ID, this.id);
        return putDefaultParams(payload);
    }

    /**
     * Return the payload wrapped into a SelfDescribingJson.
     *
     * @return the payload as a SelfDescribingJson.
     */
    public SelfDescribingJson getSelfDescribingJson() {
        return new SelfDescribingJson(TrackerConstants.SCHEMA_SCREEN_VIEW, getPayload());
    }
}
