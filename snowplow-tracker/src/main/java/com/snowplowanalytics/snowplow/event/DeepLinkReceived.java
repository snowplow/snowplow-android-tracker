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

public class DeepLinkReceived extends AbstractSelfDescribing {

    public final static String SCHEMA_DEEPLINKRECEIVED = "iglu:com.snowplowanalytics.mobile/deep_link_received/jsonschema/1-0-0";

    public final static String PARAM_DEEPLINKRECEIVED_REFERRER = "referrer";
    public final static String PARAM_DEEPLINKRECEIVED_URL = "url";

    /// It's the property for `referrer` JSON key
    @Nullable
    public String referrer;
    /// It's the property for `url` JSON key
    @NonNull
    public final String url;

    public DeepLinkReceived(@NonNull String url) {
        this.url = url;
        // Set here further checks about the arguments.
    }

    // Builder methods
    @NonNull
    public DeepLinkReceived referrer(@Nullable String referrer) {
        this.referrer = referrer;
        return this;
    }

    // Tracker methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        payload.put(PARAM_DEEPLINKRECEIVED_URL, url);
        if (referrer != null) {
            payload.put(PARAM_DEEPLINKRECEIVED_REFERRER, referrer);
        }
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return SCHEMA_DEEPLINKRECEIVED;
    }
}

