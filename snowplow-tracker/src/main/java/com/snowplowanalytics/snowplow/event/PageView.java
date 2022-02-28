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

/**
 * A pageview event.
 * @deprecated This event has been designed for web trackers, not suitable for mobile apps. Use `DeepLinkReceived` event to track deep-link received in the app.
 */
public class PageView extends AbstractPrimitive {

    /** Page URL. */
    @NonNull
    private final String pageUrl;
    /** Page title. */
    @Nullable
    private String pageTitle;
    /** Page referrer URL. */
    @Nullable
    private String referrer;

    /**
     * Creates a pageview event.
     * @param pageUrl The page URL.
     */
    public PageView(@NonNull String pageUrl) {
        Preconditions.checkNotNull(pageUrl);
        Preconditions.checkArgument(!pageUrl.isEmpty(), "pageUrl cannot be empty");
        this.pageUrl = pageUrl;
    }

    // Builder methods

    /** Page title. */
    @NonNull
    public PageView pageTitle(@Nullable String pageTitle) {
        this.pageTitle = pageTitle;
        return this;
    }

    /** Page referrer URL. */
    @NonNull
    public PageView referrer(@Nullable String referrer) {
        this.referrer = referrer;
        return this;
    }

    // Public methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put(Parameters.PAGE_URL, pageUrl);
        if (pageTitle != null) payload.put(Parameters.PAGE_TITLE, pageTitle);
        if (referrer != null) payload.put(Parameters.PAGE_REFR, referrer);
        return payload;
    }

    @Override
    public @NonNull String getName() {
        return TrackerConstants.EVENT_PAGE_VIEW;
    }
}
