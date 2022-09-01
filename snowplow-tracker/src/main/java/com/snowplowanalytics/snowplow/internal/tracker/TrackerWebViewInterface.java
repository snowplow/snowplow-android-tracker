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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.Snowplow;
import com.snowplowanalytics.snowplow.controller.TrackerController;
import com.snowplowanalytics.snowplow.event.AbstractEvent;
import com.snowplowanalytics.snowplow.event.PageView;
import com.snowplowanalytics.snowplow.event.ScreenView;
import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.event.Structured;
import com.snowplowanalytics.snowplow.internal.utils.JsonUtils;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JavaScript interface used to provide an API for tracking events from Web views.
 */
public class TrackerWebViewInterface {

    public final static String TAG = "SnowplowWebInterface";

    /**
     * Track a self-describing event from the Web view
     * @param schema Schema of the self-describing event
     * @param data JSON string with the self-describing event data
     * @param context Optional JSON string with a list of context entities
     * @param trackers Optional list of tracker namespaces to track with
     * @throws JSONException In case of JSON parsing failures
     */
    @JavascriptInterface
    public void trackSelfDescribingEvent(@NonNull String schema, @NonNull String data, @Nullable String context, @Nullable String[] trackers) throws JSONException {
        JSONObject json = new JSONObject(data);
        Map<String, Object> payload = JsonUtils.jsonToMap(json);

        SelfDescribing event = new SelfDescribing(schema, payload);
        trackEvent(event, context, trackers);
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
    public void trackStructEvent(@NonNull String category, @NonNull String action, @Nullable String label, @Nullable String property, @Nullable Double value, @Nullable String context, @Nullable String[] trackers) throws JSONException {
        Structured event = new Structured(category, action);
        event.label = label;
        event.property = property;
        event.value = value;
        trackEvent(event, context, trackers);
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
    public void trackScreenView(
            @NonNull String name,
            @NonNull String id,
            @Nullable String type,
            @Nullable String previousName,
            @Nullable String previousId,
            @Nullable String previousType,
            @Nullable String transitionType,
            @Nullable String context,
            @Nullable String[] trackers) throws JSONException {
        ScreenView event = new ScreenView(name, UUID.fromString(id))
                .type(type)
                .previousId(previousId)
                .previousName(previousName)
                .previousType(previousType)
                .transitionType(transitionType);
        trackEvent(event, context, trackers);
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
    public void trackPageView(@NonNull String pageUrl, @Nullable String pageTitle, @Nullable String referrer, @Nullable String context, @Nullable String[] trackers) throws JSONException {
        PageView event = new PageView(pageUrl)
                .pageTitle(pageTitle)
                .referrer(referrer);
        trackEvent(event, context, trackers);
    }

    private void trackEvent(@NonNull AbstractEvent event, @Nullable String context, @Nullable String[] trackers) throws JSONException {
        if (context != null) {
            List<SelfDescribingJson> contextEntities = parseContext(context);
            if (!contextEntities.isEmpty()) {
                event.contexts(contextEntities);
            }
        }

        if (trackers == null || trackers.length == 0) {
            TrackerController tracker = Snowplow.getDefaultTracker();
            if (tracker != null) {
                tracker.track(event);
            } else {
                Logger.e(TAG,"Tracker not initialized.");
            }
        } else {
            for (String namespace : trackers) {
                TrackerController tracker = Snowplow.getTracker(namespace);
                if (tracker != null) {
                    tracker.track(event);
                } else {
                    Logger.e(TAG, String.format("Tracker with namespace %s not found.", namespace));
                }
            }
        }
    }

    private List<SelfDescribingJson> parseContext(@NonNull String context) throws JSONException {
        List<SelfDescribingJson> entities = new ArrayList<>();
        JSONArray contextJson = new JSONArray(context);

        for (int i = 0; i < contextJson.length(); i++) {
            JSONObject itemJson = contextJson.getJSONObject(i);
            Map<String, Object> item = JsonUtils.jsonToMap(itemJson);

            String schema = (String) item.get("schema");
            Object data = item.get("data");

            if (schema != null && data != null) {
                entities.add(new SelfDescribingJson(schema, data));
            }
        }

        return entities;
    }

}
