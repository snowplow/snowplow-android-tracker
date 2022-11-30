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
package com.snowplowanalytics.snowplow.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.core.utils.Preconditions

/**
 * A pageview event.
 * @param pageUrl The page URL.
 */
@Deprecated("This event has been designed for web trackers, not suitable for mobile apps. Use `DeepLinkReceived` event to track deep-link received in the app.")
class PageView(pageUrl: String) : AbstractPrimitive() {
    /** Page URL.  */
    private val pageUrl: String

    /** Page title.  */
    private var pageTitle: String? = null

    /** Page referrer URL.  */
    private var referrer: String? = null
    
    init {
        Preconditions.checkArgument(pageUrl.isNotEmpty(), "pageUrl cannot be empty")
        this.pageUrl = pageUrl
    }
    
    // Builder methods
    
    /** Page title.  */
    fun pageTitle(pageTitle: String?): PageView {
        this.pageTitle = pageTitle
        return this
    }

    /** Page referrer URL.  */
    fun referrer(referrer: String?): PageView {
        this.referrer = referrer
        return this
    }

    // Public methods
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.PAGE_URL] = pageUrl
            if (pageTitle != null) payload[Parameters.PAGE_TITLE] = pageTitle
            if (referrer != null) payload[Parameters.PAGE_REFR] = referrer
            return payload
        }
    
    override val name: String
        get() = TrackerConstants.EVENT_PAGE_VIEW
}
