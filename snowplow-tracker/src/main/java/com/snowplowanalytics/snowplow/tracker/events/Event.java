/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

// Java
import java.util.List;

// This library
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

/**
 * The event interface
 */
public interface Event {

    /**
     * @return the events custom context
     */
    List<SelfDescribingJson> getContext();

    /**
     * @return the events timestamp
     */
    long getDeviceCreatedTimestamp();

    /**
     * @return the optional true events timestamp
     */
    long getTrueTimestamp();

    /**
     * @return the event id
     */
    String getEventId();

    /**
     * @return the event payload
     */
    Payload getPayload();
}
