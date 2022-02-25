/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
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
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

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

    /** List of custom contexts associated to the event. */
    public final List<SelfDescribingJson> customContexts = new LinkedList<>();
    /** Custom timestamp of the event. */
    @Nullable
    public Long trueTimestamp;

    // Builder methods

    /** Adds a list of contexts. */
    @NonNull
    public AbstractEvent contexts(@Nullable List<SelfDescribingJson> contexts) {
        if (contexts != null) customContexts.addAll(contexts);
        return this;
    }

    /** Set the custom timestamp of the event. */
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
