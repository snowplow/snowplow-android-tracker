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

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

/**
 * Constructs a PageView event object.
 */
public class PageView extends Event {

    private final String pageUrl;
    private final String pageTitle;
    private final String referrer;

    public static abstract class Builder<T extends Builder<T>> extends Event.Builder<T> {

        private String pageUrl;
        private String pageTitle;
        private String referrer;

        /**
         * @param pageUrl URL of the viewed page
         * @return itself
         */
        public T pageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
            return self();
        }

        /**
         * @param pageTitle Title of the viewed page
         * @return itself
         */
        public T pageTitle(String pageTitle) {
            this.pageTitle = pageTitle;
            return self();
        }

        /**
         * @param referrer Referrer of the page
         * @return itself
         */
        public T referrer(String referrer) {
            this.referrer = referrer;
            return self();
        }

        public PageView build() {
            return new PageView(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    protected PageView(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.pageUrl);
        Preconditions.checkArgument(!builder.pageUrl.isEmpty(), "pageUrl cannot be empty");

        this.pageUrl = builder.pageUrl;
        this.pageTitle = builder.pageTitle;
        this.referrer = builder.referrer;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_PAGE_VIEW);
        payload.add(Parameters.PAGE_URL, this.pageUrl);
        payload.add(Parameters.PAGE_TITLE, this.pageTitle);
        payload.add(Parameters.PAGE_REFR, this.referrer);
        return putDefaultParams(payload);
    }
}
