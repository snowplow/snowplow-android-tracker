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

// Java
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

// This library
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

/**
 * The event interface
 */
public interface Event {

    /**
     * @return the event custom contexts
     */
    @NonNull List<SelfDescribingJson> getContexts();

    /**
     * @return the optional true events timestamp
     */
    @Nullable
    Long getTrueTimestamp();

    /**
     * @return the event data payload
     */
    @NonNull Map<String, Object> getDataPayload();

    /**
     * Hook method called just before the event processing in order to execute special operations.
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    void beginProcessing(@NonNull Tracker tracker);

    /**
     * Hook method called just after the event processing in order to execute special operations.
     * @apiNote Internal use only - Don't use in production, it can change without notice.
     */
    void endProcessing(@NonNull Tracker tracker);
}
