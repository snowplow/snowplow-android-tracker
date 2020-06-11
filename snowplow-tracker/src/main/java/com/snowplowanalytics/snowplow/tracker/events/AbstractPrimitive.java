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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

public abstract class AbstractPrimitive extends AbstractEvent {

    AbstractPrimitive(Builder<?> builder) {
        super(builder);
    }

    protected AbstractPrimitive() { super(); }

    /**
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * replaceable by use of {@link #getDataPayload()} and {@link #getName()}.
     *
     * @return the event payload
     */
    @Override
    @Deprecated
    public @NonNull TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, getName());
        payload.addMap(getDataPayload());
        return putDefaultParams(payload);
    }

    /**
     * @return The name of the event.
     */
    public abstract @NonNull String getName();
}
