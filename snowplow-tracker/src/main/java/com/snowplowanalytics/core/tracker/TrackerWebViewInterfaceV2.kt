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
package com.snowplowanalytics.core.tracker

import android.webkit.JavascriptInterface
import com.snowplowanalytics.core.event.WebViewReader
import com.snowplowanalytics.core.tracker.Logger.e
import com.snowplowanalytics.core.utils.JsonUtils.jsonToMap
import com.snowplowanalytics.snowplow.Snowplow.defaultTracker
import com.snowplowanalytics.snowplow.Snowplow.getTracker
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * JavaScript interface used to provide an API for tracking events from WebViews.
 */
class TrackerWebViewInterfaceV2 {
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackWebViewEvent(
        eventName: String,
        trackerVersion: String,
        useragent: String,
        pageUrl: String? = null,
        pageTitle: String? = null,
        referrer: String? = null,
        category: String? = null,
        action: String? = null,
        label: String? = null,
        property: String? = null,
        value: Double? = null,
        pingXOffsetMin: Int? = null,
        pingXOffsetMax: Int? = null,
        pingYOffsetMin: Int? = null,
        pingYOffsetMax: Int? = null,
        entities: String? = null,
        trackers: Array<String>? = null
    ) {
        val event = WebViewReader(
            eventName,
            trackerVersion,
            useragent,
            pageUrl,
            pageTitle,
            referrer,
            category,
            action,
            label,
            property,
            value,
            pingXOffsetMin,
            pingXOffsetMax,
            pingYOffsetMin,
            pingYOffsetMax
        )
        trackEvent(event, entities, trackers)
    }
    
    @Throws(JSONException::class)
    private fun trackEvent(event: AbstractEvent, context: String?, trackers: Array<String>?) {
        if (context != null) {
            val contextEntities = parseContext(context)
            if (contextEntities.isNotEmpty()) {
                event.entities(contextEntities)
            }
        }
        if (trackers.isNullOrEmpty()) {
            val tracker = defaultTracker
            if (tracker != null) {
                tracker.track(event)
            } else {
                e(TAG, "Tracker not initialized.")
            }
        } else {
            for (namespace in trackers) {
                val tracker = getTracker(namespace)
                if (tracker != null) {
                    tracker.track(event)
                } else {
                    e(TAG, String.format("Tracker with namespace %s not found.", namespace))
                }
            }
        }
    }

    @Throws(JSONException::class)
    private fun parseContext(context: String): List<SelfDescribingJson> {
        val entities: MutableList<SelfDescribingJson> = ArrayList()
        val contextJson = JSONArray(context)
        for (i in 0 until contextJson.length()) {
            val itemJson = contextJson.getJSONObject(i)
            val item = jsonToMap(itemJson)
            val schema = item["schema"] as? String?
            val data = item["data"]
            if (schema != null && data != null) {
                entities.add(SelfDescribingJson(schema, data))
            }
        }
        return entities
    }

    companion object {
        const val TAG = "SnowplowWebInterface"
    }
}
