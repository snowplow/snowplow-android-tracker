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

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.Map;

/**
 * Constructs an SelfDescribing event object.
 * @deprecated It will be removed in version 3.0. Use {@link #Unstructured} instead.
 */
@Deprecated
public class SelfDescribing extends Unstructured {

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
        @NonNull
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    @NonNull
    public static Builder<?> builder() {
        return new Builder2();
    }

    protected SelfDescribing(@NonNull Builder<?> builder) {
        super(builder.eventData);
    }
}
