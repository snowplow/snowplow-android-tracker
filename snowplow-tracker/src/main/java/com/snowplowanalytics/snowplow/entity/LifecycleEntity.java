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

package com.snowplowanalytics.snowplow.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import java.util.HashMap;

public class LifecycleEntity extends SelfDescribingJson {

    public final static String SCHEMA_LIFECYCLEENTITY = "iglu:com.snowplowanalytics.mobile/application_lifecycle/jsonschema/1-0-0";

    public final static String PARAM_LIFECYCLEENTITY_INDEX = "index";
    public final static String PARAM_LIFECYCLEENTITY_ISVISIBLE = "isVisible";

    private final HashMap<String, Object> parameters = new HashMap<>();

    public LifecycleEntity(boolean isVisible) {
        super(SCHEMA_LIFECYCLEENTITY);
        parameters.put(PARAM_LIFECYCLEENTITY_ISVISIBLE, isVisible);
        setData(parameters);
        // Set here further checks about the arguments.
    }

    // Builder methods
    @NonNull
    public LifecycleEntity index(@Nullable Integer index) {
        if (index != null) {
            parameters.put(PARAM_LIFECYCLEENTITY_INDEX, index);
        }
        setData(parameters);
        return this;
    }
}

