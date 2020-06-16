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

// Java
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Map;

// This library
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.payload.Payload;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

/**
 * The event interface
 */
public interface Event {

    /**
     * @deprecated As of release 1.5.0, replaced by {@link #getContexts()}
     *
     * @return the event custom contexts
     */
    @Deprecated
    @NonNull
    List<SelfDescribingJson> getContext();

    /**
     * @return the event custom contexts
     */
    @NonNull List<SelfDescribingJson> getContexts();

    /**
     * Get the timestamp of the event.
     * @apiNote If the timestamp is not set, it sets one as a side effect.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event timestamp
     */
    @Deprecated
    long getDeviceCreatedTimestamp();

    /**
     * Get the actual timestamp of the event.
     * @apiNote It doesn't have the side effect of {@link #getDeviceCreatedTimestamp()}.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event timestamp
     */
    @Deprecated
    Long getActualDeviceCreatedTimestamp();

    /**
     * @return the optional true events timestamp
     */
    Long getTrueTimestamp();

    /**
     * Get the event id of the event.
     * @apiNote If the eventId is not set, it sets one as a side effect.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event id
     */
    @Deprecated
    @NonNull String getEventId();

    /**
     * Get the actual event id of the event.
     * @apiNote It doesn't have the side effect of {@link #getEventId()}.
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * @return the event id if it exist.
     */
    @Deprecated
    String getActualEventId();

    /**
     * @deprecated As of release 1.5.0, it will be removed in the version 2.0.0.
     * replaceable by use of {@link #getDataPayload()} without information about
     * schema or event name.
     *
     * @return the event payload
     */
    @Deprecated
    Payload getPayload();

    /**
     * @return the event data payload
     */
    @NonNull Map<String, Object> getDataPayload();

    /**
     * Hook method called just before the event processing in order to execute special operations.
     *
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    void beginProcessing(Tracker tracker);

    /**
     * Hook method called just after the event processing in order to execute special operations.
     *
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    void endProcessing(Tracker tracker);
}
