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
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

/**
 * Constructs an Unstructured event object.
 */
public class Unstructured extends Event {

    private final SelfDescribingJson eventData;

    public static abstract class Builder<T extends Builder<T>> extends Event.Builder<T> {

        private SelfDescribingJson eventData;

        /**
         * @param eventData The properties of the event. Has two field:
         *                  A "data" field containing the event properties and
         *                  A "schema" field identifying the schema against which the data is validated
         */
        public T eventData(SelfDescribingJson eventData) {
            this.eventData = eventData;
            return self();
        }

        public Unstructured build() {
            return new Unstructured(this);
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

    protected Unstructured(Builder<?> builder) {
        super(builder);
        this.eventData = builder.eventData;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @param base64Encoded whether or not to encode in base64
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload(boolean base64Encoded) {
        TrackerPayload payload = new TrackerPayload();
        SelfDescribingJson envelope = new SelfDescribingJson(
                TrackerConstants.SCHEMA_UNSTRUCT_EVENT, this.eventData.getMap());
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED);
        payload.addMap(envelope.getMap(), base64Encoded,
                Parameters.UNSTRUCTURED_ENCODED, Parameters.UNSTRUCTURED);
        return payload;
    }
}
