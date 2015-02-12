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

package com.snowplowanalytics.snowplow.tracker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.events.TransactionItem;
import com.snowplowanalytics.snowplow.tracker.generated.Version;

public class Tracker {

    private final static String TAG = Tracker.class.getSimpleName();
    private final String trackerVersion = Version.TRACKER;

    private Emitter emitter;
    private Subject subject;

    private String namespace;
    private String appId;
    private boolean base64Encoded;

    private DevicePlatforms devicePlatform;

    /**
     * Creates a Tracker object
     * @param builder The builder that constructs a tracker
     */
    private Tracker(TrackerBuilder builder) {
        this.emitter = builder.emitter;
        this.appId = builder.appId;
        this.base64Encoded = builder.base64Encoded;
        this.namespace = builder.namespace;
        this.subject = builder.subject;
        this.devicePlatform = builder.devicePlatform;
    }

    public static class TrackerBuilder {
        private final Emitter emitter; // Required
        private final String namespace; // Required
        private final String appId; // Required
        private Subject subject = null; // Optional
        private boolean base64Encoded = true; // Optional
        private DevicePlatforms devicePlatform = DevicePlatforms.Mobile; // Optional

        /**
         * @param emitter Emitter to which events will be sent
         * @param namespace Identifier for the Tracker instance
         * @param appId Application ID
         */
        public TrackerBuilder(Emitter emitter, String namespace, String appId) {
            this.emitter = emitter;
            this.namespace = namespace;
            this.appId = appId;
        }

        /**
         * @param subject Subject to be tracked
         */
        public TrackerBuilder subject(Subject subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @param base64 Whether JSONs in the payload should be base-64 encoded
         */
        public TrackerBuilder base64(Boolean base64) {
            this.base64Encoded = base64;
            return this;
        }

        /**
         * @param platform The device platform the tracker is running on
         */
        public TrackerBuilder platform(DevicePlatforms platform) {
            this.devicePlatform = platform;
            return this;
        }

        /**
         * Creates a new Tracker
         */
        public Tracker build(){
            return new Tracker(this);
        }
    }

    /**
     * Adds a complete payload to the EventStore
     * @param payload The complete payload to be
     *                sent to a collector
     */
    private void addEventPayload(Payload payload) {

        Logger.ifDebug(TAG, "Adding Payload to event storage...", payload);

        emitter.add(payload);
    }

    /**
     * Builds a final payload by joining the event payload with
     * the custom context and an optional timestamp.
     * @param payload Payload builder
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     * @return A completed Payload
     */
    private Payload completePayload(Payload payload, List<SelfDescribingJson> context,
                                      long timestamp) {

        // Add default parameters to the payload
        payload.add(Parameters.PLATFORM, this.devicePlatform.toString());
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);
        payload.add(Parameters.EID, Util.getEventId());

        // If there is a subject present for the Tracker add it
        if (this.subject != null) {
            payload.addMap(new HashMap<String,Object>(subject.getSubject()));
        }

        // If timestamp is set to 0, generate one and add it
        payload.add(Parameters.TIMESTAMP,
                (timestamp == 0 ? Util.getTimestamp() : Long.toString(timestamp)));

        // Add default information to the custom context
        List<SelfDescribingJson> final_context = addDefaultContextData(context);

        // Convert context into a List<Map> object
        List<Map> contextDataList = new LinkedList<>();
        for (SelfDescribingJson selfDescribingJson : final_context) {
            contextDataList.add(selfDescribingJson.getMap());
        }

        // Encodes context data and sets the data
        SelfDescribingJson envelope = new SelfDescribingJson(
                TrackerConstants.SCHEMA_CONTEXTS, contextDataList);

        payload.addMap(envelope.getMap(), this.base64Encoded, Parameters.CONTEXT_ENCODED,
                Parameters.CONTEXT);

        Logger.ifDebug(TAG, "Complete Payload: %s", payload);

        return payload;
    }

    /**
     * Adds the default Android Tracker contextual
     * information to the context.
     * @param context Custom context for the event
     * @return A final custom context
     */
    private List<SelfDescribingJson> addDefaultContextData(List<SelfDescribingJson> context) {
        if (context == null) {
            Logger.i(TAG, "No user context passed in");
            context = new LinkedList<>();
        }
        if (!subject.getSubjectLocation().isEmpty()) {
            SelfDescribingJson locationPayload = new SelfDescribingJson(
                    TrackerConstants.GEOLOCATION_SCHEMA, this.subject.getSubjectLocation());
            context.add(locationPayload);
        }
        if (!subject.getSubjectMobile().isEmpty()) {
            SelfDescribingJson mobilePayload = new SelfDescribingJson(
                    TrackerConstants.MOBILE_SCHEMA, this.subject.getSubjectMobile());
            context.add(mobilePayload);
        }
        return context;
    }

    // Event Tracking Functions

    /**
     * @param pageUrl URL of the viewed page
     * @param pageTitle Title of the viewed page
     * @param referrer Referrer of the page
     */
    public void trackPageView(String pageUrl, String pageTitle, String referrer) {
        trackPageView(pageUrl, pageTitle, referrer, null, 0);
    }

    /**
     * @param pageUrl URL of the viewed page
     * @param pageTitle Title of the viewed page
     * @param referrer Referrer of the page
     * @param context Custom context for the event
     */
    public void trackPageView(String pageUrl, String pageTitle, String referrer,
                              List<SelfDescribingJson> context) {
        trackPageView(pageUrl,pageTitle, referrer, context, 0);
    }

    /**
     * @param pageUrl URL of the viewed page
     * @param pageTitle Title of the viewed page
     * @param referrer Referrer of the page
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackPageView(String pageUrl, String pageTitle, String referrer,
                              long timestamp) {
        trackPageView(pageUrl, pageTitle, referrer, null, timestamp);
    }

    /**
     * @param pageUrl URL of the viewed page
     * @param pageTitle Title of the viewed page
     * @param referrer Referrer of the page
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackPageView(String pageUrl, String pageTitle, String referrer,
                              List<SelfDescribingJson> context, long timestamp) {
        // Precondition checks
        Preconditions.checkNotNull(pageUrl);
        Preconditions.checkArgument(!pageUrl.isEmpty(), "pageUrl cannot be empty");
        Preconditions.checkArgument(!pageTitle.isEmpty(), "pageTitle cannot be empty");
        Preconditions.checkArgument(!referrer.isEmpty(), "referrer cannot be empty");

        Payload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_PAGE_VIEW);
        payload.add(Parameters.PAGE_URL, pageUrl);
        payload.add(Parameters.PAGE_TITLE, pageTitle);
        payload.add(Parameters.PAGE_REFR, referrer);

        completePayload(payload, context, timestamp);

        addEventPayload(payload);
    }

    /**
     * @param category Category of the event
     * @param action The event itself
     * @param label Refer to the object the action is performed on
     * @param property Property associated with either the action or the object
     * @param value A value associated with the user action
     */
    public void trackStructuredEvent(String category, String action, String label, String property,
                                     int value) {
        trackStructuredEvent(category, action, label, property, value, null, 0);
    }

    /**
     * @param category Category of the event
     * @param action The event itself
     * @param label Refer to the object the action is performed on
     * @param property Property associated with either the action or the object
     * @param value A value associated with the user action
     * @param context Custom context for the event
     */
    public void trackStructuredEvent(String category, String action, String label, String property,
                                     int value, List<SelfDescribingJson> context) {
        trackStructuredEvent(category, action, label, property, value, context, 0);
    }

    /**
     * @param category Category of the event
     * @param action The event itself
     * @param label Refer to the object the action is performed on
     * @param property Property associated with either the action or the object
     * @param value A value associated with the user action
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackStructuredEvent(String category, String action, String label, String property,
                                     int value, long timestamp) {
        trackStructuredEvent(category, action, label, property, value, null, timestamp);
    }

    /**
     * @param category Category of the event
     * @param action The event itself
     * @param label Refer to the object the action is performed on
     * @param property Property associated with either the action or the object
     * @param value A value associated with the user action
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackStructuredEvent(String category, String action, String label, String property,
                                     int value, List<SelfDescribingJson> context, long timestamp) {
        // Precondition checks
        Preconditions.checkNotNull(label);
        Preconditions.checkNotNull(property);
        Preconditions.checkArgument(!label.isEmpty(), "label cannot be empty");
        Preconditions.checkArgument(!property.isEmpty(), "property cannot be empty");
        Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");
        Preconditions.checkArgument(!action.isEmpty(), "action cannot be empty");

        Payload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_STRUCTURED);
        payload.add(Parameters.SE_CATEGORY, category);
        payload.add(Parameters.SE_ACTION, action);
        payload.add(Parameters.SE_LABEL, label);
        payload.add(Parameters.SE_PROPERTY, property);
        payload.add(Parameters.SE_VALUE, Double.toString(value));

        completePayload(payload, context, timestamp);

        addEventPayload(payload);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                  A "schema" field identifying the schema against which the data is validated
     */
    public void trackUnstructuredEvent(SelfDescribingJson eventData) {
        trackUnstructuredEvent(eventData, null, 0);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                   A "schema" field identifying the schema against which the data is validated
     * @param context Custom context for the event
     */
    public void trackUnstructuredEvent(SelfDescribingJson eventData, List<SelfDescribingJson> context) {
        trackUnstructuredEvent(eventData, context, 0);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                   A "schema" field identifying the schema against which the data is validated
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackUnstructuredEvent(SelfDescribingJson eventData, long timestamp) {
        trackUnstructuredEvent(eventData, null, timestamp);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                   A "schema" field identifying the schema against which the data is validated
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackUnstructuredEvent(SelfDescribingJson eventData, List<SelfDescribingJson> context,
                                       long timestamp) {
        Payload payload = new TrackerPayload();
        SelfDescribingJson envelope = new SelfDescribingJson(
                TrackerConstants.SCHEMA_UNSTRUCT_EVENT, eventData.getMap());
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED);
        payload.addMap(envelope.getMap(), base64Encoded,
                Parameters.UNSTRUCTURED_ENCODED, Parameters.UNSTRUCTURED);

        completePayload(payload, context, timestamp);

        addEventPayload(payload);
    }

    /**
     * This is an internal method called by track_ecommerce_transaction. It is not for public use.
     * @param order_id Order ID
     * @param sku Item SKU
     * @param price Item price
     * @param quantity Item quantity
     * @param name Item name
     * @param category Item category
     * @param currency The currency the price is expressed in
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    protected void trackEcommerceTransactionItem(String order_id, String sku, Double price,
                                                 Integer quantity, String name, String category,
                                                 String currency, List<SelfDescribingJson> context,
                                                 long timestamp) {
        // Precondition checks
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(category);
        Preconditions.checkNotNull(currency);
        Preconditions.checkArgument(!order_id.isEmpty(), "order_id cannot be empty");
        Preconditions.checkArgument(!sku.isEmpty(), "sku cannot be empty");
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        Preconditions.checkArgument(!category.isEmpty(), "category cannot be empty");
        Preconditions.checkArgument(!currency.isEmpty(), "currency cannot be empty");

        Payload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_ECOMM_ITEM);
        payload.add(Parameters.TI_ITEM_ID, order_id);
        payload.add(Parameters.TI_ITEM_SKU, sku);
        payload.add(Parameters.TI_ITEM_NAME, name);
        payload.add(Parameters.TI_ITEM_CATEGORY, category);
        payload.add(Parameters.TI_ITEM_PRICE, Double.toString(price));
        payload.add(Parameters.TI_ITEM_QUANTITY, Double.toString(quantity));
        payload.add(Parameters.TI_ITEM_CURRENCY, currency);

        completePayload(payload, context, timestamp);

        addEventPayload(payload);
    }

    /**
     * @param order_id ID of the eCommerce transaction
     * @param total_value Total transaction value
     * @param affiliation Transaction affiliation
     * @param tax_value Transaction tax value
     * @param shipping Delivery cost charged
     * @param city Delivery address city
     * @param state Delivery address state
     * @param country Delivery address country
     * @param currency The currency the price is expressed in
     * @param items The items in the transaction
     */
    public void trackEcommerceTransaction(String order_id, Double total_value, String affiliation,
                                          Double tax_value, Double shipping, String city,
                                          String state, String country, String currency,
                                          List<TransactionItem> items) {
        trackEcommerceTransaction(order_id, total_value, affiliation, tax_value, shipping, city,
                state, country, currency, items, null, 0);
    }

    /**
     * @param order_id ID of the eCommerce transaction
     * @param total_value Total transaction value
     * @param affiliation Transaction affiliation
     * @param tax_value Transaction tax value
     * @param shipping Delivery cost charged
     * @param city Delivery address city
     * @param state Delivery address state
     * @param country Delivery address country
     * @param currency The currency the price is expressed in
     * @param items The items in the transaction
     * @param context Custom context for the event
     */
    public void trackEcommerceTransaction(String order_id, Double total_value, String affiliation,
                                          Double tax_value, Double shipping, String city,
                                          String state, String country, String currency,
                                          List<TransactionItem> items, List<SelfDescribingJson> context) {
        trackEcommerceTransaction(order_id, total_value, affiliation, tax_value, shipping, city,
                state, country, currency, items, context, 0);
    }

    /**
     * @param order_id ID of the eCommerce transaction
     * @param total_value Total transaction value
     * @param affiliation Transaction affiliation
     * @param tax_value Transaction tax value
     * @param shipping Delivery cost charged
     * @param city Delivery address city
     * @param state Delivery address state
     * @param country Delivery address country
     * @param currency The currency the price is expressed in
     * @param items The items in the transaction
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackEcommerceTransaction(String order_id, Double total_value, String affiliation,
                                          Double tax_value, Double shipping, String city,
                                          String state, String country, String currency,
                                          List<TransactionItem> items, long timestamp) {
        trackEcommerceTransaction(order_id, total_value, affiliation, tax_value, shipping, city,
                state, country, currency, items, null, timestamp);
    }

    /**
     * @param order_id ID of the eCommerce transaction
     * @param total_value Total transaction value
     * @param affiliation Transaction affiliation
     * @param tax_value Transaction tax value
     * @param shipping Delivery cost charged
     * @param city Delivery address city
     * @param state Delivery address state
     * @param country Delivery address country
     * @param currency The currency the price is expressed in
     * @param items The items in the transaction
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    @SuppressWarnings("unchecked")
    public void trackEcommerceTransaction(String order_id, Double total_value, String affiliation,
                                          Double tax_value, Double shipping, String city,
                                          String state, String country, String currency,
                                          List<TransactionItem> items, List<SelfDescribingJson> context,
                                          long timestamp) {
        // Precondition checks
        Preconditions.checkNotNull(affiliation);
        Preconditions.checkNotNull(city);
        Preconditions.checkNotNull(state);
        Preconditions.checkNotNull(country);
        Preconditions.checkNotNull(currency);
        Preconditions.checkArgument(!order_id.isEmpty(), "order_id cannot be empty");
        Preconditions.checkArgument(!affiliation.isEmpty(), "affiliation cannot be empty");
        Preconditions.checkArgument(!city.isEmpty(), "city cannot be empty");
        Preconditions.checkArgument(!state.isEmpty(), "state cannot be empty");
        Preconditions.checkArgument(!country.isEmpty(), "country cannot be empty");
        Preconditions.checkArgument(!currency.isEmpty(), "currency cannot be empty");

        Payload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_ECOMM);
        payload.add(Parameters.TR_ID, order_id);
        payload.add(Parameters.TR_TOTAL, Double.toString(total_value));
        payload.add(Parameters.TR_AFFILIATION, affiliation);
        payload.add(Parameters.TR_TAX, Double.toString(tax_value));
        payload.add(Parameters.TR_SHIPPING, Double.toString(shipping));
        payload.add(Parameters.TR_CITY, city);
        payload.add(Parameters.TR_STATE, state);
        payload.add(Parameters.TR_COUNTRY, country);
        payload.add(Parameters.TR_CURRENCY, currency);

        completePayload(payload, context, timestamp);

        for (TransactionItem item : items) {
            trackEcommerceTransactionItem(
                    (String) item.get(Parameters.TI_ITEM_ID),
                    (String) item.get(Parameters.TI_ITEM_SKU),
                    (Double) item.get(Parameters.TI_ITEM_PRICE),
                    (Integer) item.get(Parameters.TI_ITEM_QUANTITY),
                    (String) item.get(Parameters.TI_ITEM_NAME),
                    (String) item.get(Parameters.TI_ITEM_CATEGORY),
                    (String) item.get(Parameters.TI_ITEM_CURRENCY),
                    (List<SelfDescribingJson>) item.get(Parameters.CONTEXT),
                    timestamp);
        }

        addEventPayload(payload);
    }

    /**
     * @param name The name of the screen view event
     * @param id Screen view ID
     */
    public void trackScreenView(String name, String id) {
        trackScreenView(name, id, null, 0);
    }

    /**
     * @param name The name of the screen view event
     * @param id Screen view ID
     * @param context Custom context for the event
     */
    public void trackScreenView(String name, String id, List<SelfDescribingJson> context) {
        trackScreenView(name, id, context, 0);
    }

    /**
     * @param name The name of the screen view event
     * @param id Screen view ID
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackScreenView(String name, String id, long timestamp) {
        trackScreenView(name, id, null, timestamp);
    }

    /**
     * @param name The name of the screen view event
     * @param id Screen view ID
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackScreenView(String name, String id, List<SelfDescribingJson> context,
                                long timestamp) {
        Preconditions.checkArgument(name != null || id != null);
        TrackerPayload trackerPayload = new TrackerPayload();

        trackerPayload.add(Parameters.SV_NAME, name);
        trackerPayload.add(Parameters.SV_ID, id);

        SelfDescribingJson payload = new SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN_VIEW, trackerPayload);

        trackUnstructuredEvent(payload, context, timestamp);
    }

    // Get & Set Functions

    /**
     * @param subject a valid subject object
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * @param emitter a valid emitter object
     */
    public void setEmitter(Emitter emitter) {
        // Need to shutdown prior emitter before updating
        this.emitter.shutdown();

        // Set the new emitter
        this.emitter = emitter;
    }

    /**
     * @param platform a valid DevicePlatforms object
     */
    public void setPlatform(DevicePlatforms platform) {
        this.devicePlatform = platform;
    }

    /**
     * @return the tracker version that was set
     */
    public String getTrackerVersion() {
        return this.trackerVersion;
    }

    /**
     * @return the trackers subject object
     */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * @return the emitter associated with the tracker
     */
    public Emitter getEmitter() {
        return this.emitter;
    }

    /**
     * @return the trackers namespace
     */
    public String getNamespace() {
        return this.namespace;
    }

    /**
     * @return the trackers set Application ID
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * @return the base64 setting of the tracker
     */
    public boolean getBase64Encoded() {
        return this.base64Encoded;
    }

    /**
     * @return the trackers device platform
     */
    public DevicePlatforms getPlatform() {
        return this.devicePlatform;
    }
}
