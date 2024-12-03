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
 * This V2 interface works with the WebView tracker v0.3.0+.
 */
class TrackerWebViewInterfaceV2 {
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackWebViewEvent(
        atomicProperties: String,
        selfDescribingEventData: String? = null,
        entities: String? = null,
        trackers: Array<String>? = null
    ) {
        val atomic = JSONObject(atomicProperties)

        val event = WebViewReader(
            selfDescribingEventData = parseSelfDescribingEventData(selfDescribingEventData),
            eventName = getProperty(atomic, "eventName")?.toString(),
            trackerVersion = getProperty(atomic, "trackerVersion")?.toString(),
            useragent = getProperty(atomic, "useragent")?.toString(),
            pageUrl = getProperty(atomic, "pageUrl")?.toString(),
            pageTitle = getProperty(atomic, "pageTitle")?.toString(),
            referrer = getProperty(atomic, "referrer")?.toString(),
            category = getProperty(atomic, "category")?.toString(),
            action = getProperty(atomic, "action")?.toString(),
            label = getProperty(atomic, "label")?.toString(),
            property = getProperty(atomic, "property")?.toString(),
            value = getProperty(atomic, "value")?.toString()?.toDoubleOrNull(),
            pingXOffsetMin = getProperty(atomic, "pingXOffsetMin")?.toString()?.toIntOrNull(),
            pingXOffsetMax = getProperty(atomic, "pingXOffsetMax")?.toString()?.toIntOrNull(),
            pingYOffsetMin = getProperty(atomic, "pingYOffsetMin")?.toString()?.toIntOrNull(),
            pingYOffsetMax = getProperty(atomic, "pingYOffsetMax")?.toString()?.toIntOrNull(),
        )
        trackEvent(event, entities, trackers)
    }

    private fun getProperty(atomicProperties: JSONObject, property: String) = try {
        if (atomicProperties.has(property)) {
            atomicProperties.get(property)
        } else {
            null
        }
    } catch (e: JSONException) {
        null
    }

    @Throws(JSONException::class)
    private fun trackEvent(event: AbstractEvent, contextEntities: String?, trackers: Array<String>?) {
        if (contextEntities != null) {
            val entities = parseEntities(contextEntities)
            if (entities.isNotEmpty()) {
                event.entities(entities)
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

    private fun createSelfDescribingJson(map: Map<String, Any?>): SelfDescribingJson? {
        val schema = map["schema"] as? String?
        val data = map["data"]
        return if (schema != null && data != null) {
            SelfDescribingJson(schema, data)
        } else {
            null
        }
    }

    @Throws(JSONException::class)
    private fun parseEntities(serialisedEntities: String): List<SelfDescribingJson> {
        val entities: MutableList<SelfDescribingJson> = ArrayList()
        val entitiesJson = JSONArray(serialisedEntities)
        for (i in 0 until entitiesJson.length()) {
            val itemJson = entitiesJson.getJSONObject(i)
            val item = jsonToMap(itemJson)
            val selfDescribingJson = createSelfDescribingJson(item)
            if (selfDescribingJson != null) {
                entities.add(selfDescribingJson)
            }
        }
        return entities
    }

    @Throws(JSONException::class)
    private fun parseSelfDescribingEventData(serialisedEvent: String?): SelfDescribingJson? {
        return serialisedEvent?.let {
            val eventJson = JSONObject(it)
            createSelfDescribingJson(jsonToMap(eventJson))
        }
    }

    companion object {
        const val TAG = "SnowplowWebInterfaceV2"
    }
}
