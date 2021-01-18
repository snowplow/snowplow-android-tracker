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
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;

import java.util.Map;

/**
 * Constructs an SelfDescribing event object.
 */
public class SelfDescribing extends AbstractSelfDescribing {

    private final SelfDescribingJson eventData;
    private boolean base64Encode;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private SelfDescribingJson eventData;

        /**
         * @param eventData The properties of the event. Has two field:
         *                  A "data" field containing the event properties and
         *                  A "schema" field identifying the schema against which the data is validated
         * @return itself
         */
        @NonNull
        public T eventData(@NonNull SelfDescribingJson eventData) {
            this.eventData = eventData;
            return self();
        }

        @NonNull
        public SelfDescribing build() {
            return new SelfDescribing(this);
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

    protected SelfDescribing(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.eventData);

        this.eventData = builder.eventData;
    }

    /**
     * @param base64Encode whether to base64Encode the event data
     */
    public void setBase64Encode(boolean base64Encode) {
        this.base64Encode = base64Encode;
    }

    /**
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     *
     * This replace the {@link #getPayload()} returning a TrackerPayload.
     * Do not use in production code as it's an internal deprecated method.
     */
    @Deprecated
    @NonNull
    public TrackerPayload getTrackerPayload() {
        TrackerPayload payload = new TrackerPayload();
        SelfDescribingJson envelope = new SelfDescribingJson(
                TrackerConstants.SCHEMA_UNSTRUCT_EVENT, this.eventData.getMap());
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED);
        payload.addMap(envelope.getMap(), this.base64Encode,
                Parameters.UNSTRUCTURED_ENCODED, Parameters.UNSTRUCTURED);
        return putDefaultParams(payload);
    }

    /**
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     *
     * This method is for internal use and deprecated. Do not use in production code.
     * In case it has been already used, it's replaceable by use of {@link #getTrackerPayload()}.
     */
    @Override
    @Deprecated
    @NonNull
    public SelfDescribingJson getPayload() {
        return super.getPayload();
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        try {
            return (Map<String, Object>) this.eventData.getMap().get(Parameters.DATA);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @NonNull String getSchema() {
        return (String) eventData.getMap().get(Parameters.SCHEMA);
    }
}
