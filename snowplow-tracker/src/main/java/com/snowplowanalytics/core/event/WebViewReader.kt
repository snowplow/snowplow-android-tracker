/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.event

import com.snowplowanalytics.core.constants.Parameters
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.event.AbstractPrimitive

/**
 * A PageView event. This event has been designed for web trackers, and is not suitable for mobile apps.
 * @param pageUrl The page URL.
 */
class WebViewReader(
    val eventName: String,
    val trackerVersion: String,
    val useragent: String,
    val pageUrl: String? = null,
    val pageTitle: String? = null,
    val referrer: String? = null,
    val category: String? = null,
    val action: String? = null,
    val label: String? = null,
    val property: String? = null,
    val value: Double? = null,
    val ping_xoffset_min: Int? = null,
    val ping_xoffset_max: Int? = null,
    val ping_yoffset_min: Int? = null,
    val ping_yoffset_max: Int? = null
) : AbstractPrimitive() {
    

    // Public methods
    override val dataPayload: Map<String, Any?>
        get() {
            val payload = HashMap<String, Any?>()
            payload[Parameters.EVENT] = eventName
            payload[Parameters.TRACKER_VERSION] = trackerVersion
            payload[Parameters.USERAGENT] = useragent
            if (pageUrl != null) payload[Parameters.PAGE_URL] = pageUrl
            if (pageTitle != null) payload[Parameters.PAGE_TITLE] = pageTitle
            if (referrer != null) payload[Parameters.PAGE_REFR] = referrer
            if (category != null) payload[Parameters.SE_CATEGORY] = category
            if (action != null) payload[Parameters.SE_ACTION] = action
            if (label != null) payload[Parameters.SE_LABEL] = label
            if (property != null) payload[Parameters.SE_PROPERTY] = property
            if (value != null) payload[Parameters.SE_VALUE] = value
            if (ping_xoffset_min != null) payload[Parameters.PING_XOFFSET_MIN] = ping_xoffset_min
            if (ping_xoffset_max != null) payload[Parameters.PING_XOFFSET_MAX] = ping_xoffset_max
            if (ping_yoffset_min != null) payload[Parameters.PING_YOFFSET_MIN] = ping_yoffset_min
            if (ping_yoffset_max != null) payload[Parameters.PING_YOFFSET_MAX] = ping_yoffset_max
            return payload
        }
    
    override val name: String
        get() = "internal_use_only"
}
