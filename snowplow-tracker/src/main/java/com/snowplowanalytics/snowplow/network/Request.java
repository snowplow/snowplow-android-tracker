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

package com.snowplowanalytics.snowplow.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.Payload;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request class that contains the payloads to send
 * to the collector.
 */
public class Request {
    public final Payload payload;
    public final List<Long> emitterEventIds;
    public final boolean oversize;
    public final String customUserAgent;

    /**
     * Create a request object.
     * @param payload to send to the collector.
     * @param id as reference of the event to send.
     */
    public Request(@NonNull Payload payload, long id) {
        this(payload, id, false);
    }

    /**
     * Create a request object.
     * @param payload to send to the collector.
     * @param id as reference of the event to send.
     * @param oversize indicates if the payload exceeded the maximum size allowed.
     */
    public Request(@NonNull Payload payload, long id, boolean oversize) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        emitterEventIds = ids;
        this.payload = payload;
        this.oversize = oversize;
        customUserAgent = getUserAgent(payload);
    }

    /**
     * Create a request object.
     * @param payloads to send to the collector as a payload bundle.
     * @param emitterEventIds as reference of the events to send.
     */
    public Request(@NonNull List<Payload> payloads, @NonNull List<Long> emitterEventIds) {
        String tempUserAgent = null;
        ArrayList<Map> payloadData = new ArrayList<>();
        for (Payload payload : payloads) {
            payloadData.add(payload.getMap());
            tempUserAgent = getUserAgent(payload);
        }
        payload = new TrackerPayload();
        SelfDescribingJson payloadBundle = new SelfDescribingJson(TrackerConstants.SCHEMA_PAYLOAD_DATA, payloadData);
        payload.addMap(payloadBundle.getMap());
        this.emitterEventIds = emitterEventIds;
        customUserAgent = tempUserAgent;
        oversize = false;
    }

    /**
     * Get the User-Agent string for the request's header.
     *
     * @param payload The payload where to get the `ua` parameter.
     * @return User-Agent string from subject settings or the default one.
     */
    @Nullable
    private String getUserAgent(@NonNull Payload payload) {
        HashMap hashMap = (HashMap) payload.getMap();
        return (String) hashMap.get(Parameters.USERAGENT);
    }
}
