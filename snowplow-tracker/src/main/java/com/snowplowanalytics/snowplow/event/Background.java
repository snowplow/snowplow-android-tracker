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

import java.util.HashMap;
import java.util.Map;

/** A background transition event. */
public class Background extends AbstractSelfDescribing {

    public final static String SCHEMA = "iglu:com.snowplowanalytics.snowplow/application_background/jsonschema/1-0-0";

    public final static String PARAM_INDEX = "backgroundIndex";

    /** Index indicating the current transition. */
    @Nullable
    public Integer backgroundIndex;

    // Builder methods

    /** Index indicating the current transition. */
    @NonNull
    public Background backgroundIndex(@Nullable Integer backgroundIndex) {
        this.backgroundIndex = backgroundIndex;
        return this;
    }

    // Tracker methods

    @NonNull
    @Override
    public String getSchema() {
        return SCHEMA;
    }

    @NonNull
    @Override
    public Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        if (backgroundIndex != null) {
            payload.put(PARAM_INDEX, backgroundIndex);
        }
        return payload;
    }
}
