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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Constructs a PageView event object.
 */
public class PageView extends AbstractPrimitive {

    @NonNull
    private final String pageUrl;
    @Nullable
    private String pageTitle;
    @Nullable
    private String referrer;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String pageUrl;
        private String pageTitle;
        private String referrer;

        /**
         * @param pageUrl URL of the viewed page
         * @return itself
         */
        @NonNull
        public T pageUrl(@NonNull String pageUrl) {
            this.pageUrl = pageUrl;
            return self();
        }

        /**
         * @param pageTitle Title of the viewed page
         * @return itself
         */
        @NonNull
        public T pageTitle(@NonNull String pageTitle) {
            this.pageTitle = pageTitle;
            return self();
        }

        /**
         * @param referrer Referrer of the page
         * @return itself
         */
        @NonNull
        public T referrer(@NonNull String referrer) {
            this.referrer = referrer;
            return self();
        }

        @NonNull
        public PageView build() {
            return new PageView(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @NonNull
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    @NonNull
    public static Builder<?> builder() {
        return new Builder2();
    }

    protected PageView(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.pageUrl);
        Preconditions.checkArgument(!builder.pageUrl.isEmpty(), "pageUrl cannot be empty");

        this.pageUrl = builder.pageUrl;
        this.pageTitle = builder.pageTitle;
        this.referrer = builder.referrer;
    }

    public PageView(@NonNull String pageUrl) {
        Preconditions.checkNotNull(pageUrl);
        Preconditions.checkArgument(!pageUrl.isEmpty(), "pageUrl cannot be empty");
        this.pageUrl = pageUrl;
    }

    // Builder methods

    @NonNull
    public PageView pageTitle(@Nullable String pageTitle) {
        this.pageTitle = pageTitle;
        return this;
    }

    @NonNull
    public PageView referrer(@Nullable String referrer) {
        this.referrer = referrer;
        return this;
    }

    // Public methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>();
        if (pageUrl != null) payload.put(Parameters.PAGE_URL, pageUrl);
        if (pageTitle != null) payload.put(Parameters.PAGE_TITLE, pageTitle);
        if (referrer != null) payload.put(Parameters.PAGE_REFR, referrer);
        return payload;
    }

    @Override
    public @NonNull String getName() {
        return TrackerConstants.EVENT_PAGE_VIEW;
    }
}
