/*
 * Copyright (c) 2015-2021 Snowplow Analytics Ltd. All rights reserved.
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

public class Background extends AbstractSelfDescribing {


    public final static String SCHEMA_BACKGROUND = "iglu:com.snowplowanalytics.snowplow/application_background/jsonschema/1-0-0";

    public final static String PARAM_BACKGROUNDINDEX = "backgroundIndex";

    /// It's the property for `backgroundIndex` JSON key
    @Nullable
    public Integer backgroundIndex;

    // Builder methods

    @NonNull
    public Background backgroundIndex(@Nullable Integer backgroundIndex) {
        this.backgroundIndex = backgroundIndex;
        return this;
    }

    // Tracker methods

    @NonNull
    @Override
    public String getSchema() {
        return SCHEMA_BACKGROUND;
    }

    @NonNull
    @Override
    public Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        if (backgroundIndex != null) {
            payload.put(PARAM_BACKGROUNDINDEX, backgroundIndex);
        }
        return payload;
    }
}
