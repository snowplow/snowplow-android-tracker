/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
 * JavaScript interface used to provide an API for tracking events from Web views.
 */
class TrackerWebViewInterface {
    /**
     * Track a self-describing event from the Web view
     * @param schema Schema of the self-describing event
     * @param data JSON string with the self-describing event data
     * @param context Optional JSON string with a list of context entities
     * @param trackers Optional list of tracker namespaces to track with
     * @throws JSONException In case of JSON parsing failures
     */
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackSelfDescribingEvent(
        schema: String,
        data: String,
        context: String?,
        trackers: Array<String>?
    ) {
        val json = JSONObject(data)
        val payload = jsonToMap(json)
        val event = SelfDescribing(schema, payload)
        trackEvent(event, context, trackers)
    }

    /**
     * Track a structured event from the Web view
     * @param category Name you for the group of objects you want to track e.g. "media", "ecomm"
     * @param action Defines the type of user interaction for the web object
     * @param label Identifies the specific object being actioned
     * @param property Describes the object or the action performed on it
     * @param value Quantifies or further describes the user action
     * @param context Optional JSON string with a list of context entities
     * @param trackers Optional list of tracker namespaces to track with
     * @throws JSONException In case of JSON parsing failures
     */
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackStructEvent(
        category: String,
        action: String,
        label: String?,
        property: String?,
        value: Double?,
        context: String?,
        trackers: Array<String>?
    ) {
        val event = Structured(category, action)
        event.label = label
        event.property = property
        event.value = value
        trackEvent(event, context, trackers)
    }

    /**
     * Track a screen view event from the Web view
     * @param name The name of the screen viewed
     * @param id The id (UUID v4) of screen that was viewed
     * @param type The type of screen that was viewed
     * @param previousName The name of the previous screen that was viewed
     * @param previousId The id (UUID v4) of the previous screen that was viewed
     * @param previousType The type of screen that was viewed
     * @param transitionType The type of transition that led to the screen being viewed
     * @param context Optional JSON string with a list of context entities
     * @param trackers Optional list of tracker namespaces to track with
     * @throws JSONException In case of JSON parsing failures
     */
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackScreenView(
        name: String,
        id: String,
        type: String?,
        previousName: String?,
        previousId: String?,
        previousType: String?,
        transitionType: String?,
        context: String?,
        trackers: Array<String>?
    ) {
        val event = ScreenView(name, UUID.fromString(id))
            .type(type)
            .previousId(previousId)
            .previousName(previousName)
            .previousType(previousType)
            .transitionType(transitionType)
        trackEvent(event, context, trackers)
    }

    /**
     * Track a page view event from the Web view
     * @param pageUrl Page URL
     * @param pageTitle Page title
     * @param referrer Referrer URL
     * @param context Optional JSON string with a list of context entities
     * @param trackers Optional list of tracker namespaces to track with
     * @throws JSONException In case of JSON parsing failures
     */
    @JavascriptInterface
    @Throws(JSONException::class)
    fun trackPageView(
        pageUrl: String,
        pageTitle: String?,
        referrer: String?,
        context: String?,
        trackers: Array<String>?
    ) {
        val event = PageView(pageUrl)
            .pageTitle(pageTitle)
            .referrer(referrer)
        trackEvent(event, context, trackers)
    }

    @Throws(JSONException::class)
    private fun trackEvent(event: AbstractEvent, context: String?, trackers: Array<String>?) {
        if (context != null) {
            val contextEntities = parseContext(context)
            if (contextEntities.isNotEmpty()) {
                event.entities(contextEntities)
            }
        }
        if (trackers == null || trackers.isEmpty()) {
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
