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

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import java.util.HashMap;
import java.util.Map;

/** A timing event. */
public class Timing extends AbstractSelfDescribing {

    @NonNull
    public final String category;
    @NonNull
    public final String variable;
    @NonNull
    public final Integer timing;
    @Nullable
    public String label;

    public Timing(@NonNull String category, @NonNull String variable, @NonNull Integer timing) {
        Preconditions.checkNotNull(category);
        Preconditions.checkNotNull(timing);
        Preconditions.checkNotNull(variable);
        Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");
        Preconditions.checkArgument(!variable.isEmpty(), "variable cannot be empty");
        this.category = category;
        this.variable = variable;
        this.timing = timing;
    }

    // Builder methods

    @NonNull
    public Timing label(@Nullable String label) {
        this.label = label;
        return this;
    }

    // Tracker methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
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
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_USER_TIMINGS;
    }
}
