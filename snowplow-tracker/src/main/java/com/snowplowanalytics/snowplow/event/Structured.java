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

/** A Structured event. */
public class Structured extends AbstractPrimitive {

    @NonNull
    public final String category;
    @NonNull
    public final String action;
    @Nullable
    public String label;
    @Nullable
    public String property;
    @Nullable
    public Double value;

    public Structured(@NonNull String category, @NonNull String action) {
        Preconditions.checkNotNull(category);
        Preconditions.checkNotNull(action);
        Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");
        Preconditions.checkArgument(!action.isEmpty(), "action cannot be empty");
        this.category = category;
        this.action = action;
    }

    // Builder methods

    @NonNull
    public Structured label(@Nullable String label) {
        this.label = label;
        return this;
    }

    @NonNull
    public Structured property(@Nullable String property) {
        this.property = property;
        return this;
    }

    @NonNull
    public Structured value(@Nullable Double value) {
        this.value = value;
        return this;
    }

    // Tracker methods

    @NonNull
    @Override
    public Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>(5);
        payload.put(Parameters.SE_CATEGORY, category);
        payload.put(Parameters.SE_ACTION, action);
        if (label != null) payload.put(Parameters.SE_LABEL, label);
        if (property != null) payload.put(Parameters.SE_PROPERTY, property);
        if (value != null) payload.put(Parameters.SE_VALUE, Double.toString(value));
        return payload;
    }

    @NonNull
    @Override
    public String getName() {
        return TrackerConstants.EVENT_STRUCTURED;
    }
}
