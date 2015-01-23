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

package com.snowplowanalytics.snowplow.tracker.android;

import android.util.Log;

import com.snowplowanalytics.snowplow.tracker.android.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.android.generic_utils.Util;
import com.snowplowanalytics.snowplow.tracker.android.payload_utils.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.android.payload_utils.TrackerPayload;

import com.snowplowanalytics.snowplow.tracker.android.generic_utils.Preconditions;

import com.snowplowanalytics.snowplow.tracker.android.constants.DevicePlatforms;
import com.snowplowanalytics.snowplow.tracker.android.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.android.tracker_events.TransactionItem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Tracker {

    private Emitter emitter;
    private String namespace;
    private String appId;
    private Subject subject;
    private boolean base64Encoded = true;

    private String trackerVersion;
    private DevicePlatforms platform;

    private String TAG = Tracker.class.getName();

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
        setTrackerVersion(Version.TRACKER);
        setPlatform(DevicePlatforms.Mobile);
    }

    public static class TrackerBuilder {
        private final Emitter emitter; // Required
        private final String namespace; // Required
        private final String appId; // Required
        private Subject subject = null; // Optional
        private boolean base64Encoded = true; // Optional

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
         * @return
         */
        public TrackerBuilder subject(Subject subject) {
            this.subject = subject;
            return this;
        }

        /**
         * @param base64 Whether JSONs in the payload should be base-64 encoded
         * @return
         */
        public TrackerBuilder base64(Boolean base64) {
            this.base64Encoded = base64;
            return this;
        }

        /**
         * @return a new Tracker object
         */
        public Tracker build(){
            return new Tracker(this);
        }
    }

    protected Payload completePayload(Payload payload, List<SchemaPayload> context,
                                      long timestamp) {

        if (this.subject != null) {

            if (context == null) {
                Log.d(TAG, "No list of user context passed in");
                context = new LinkedList<SchemaPayload>();
            }

            if (!this.subject.getSubjectLocation().isEmpty()) {
                SchemaPayload locationPayload = new SchemaPayload();
                locationPayload.setSchema(TrackerConstants.GEOLOCATION_SCHEMA);
                locationPayload.setData(this.subject.getSubjectLocation());
                context.add(locationPayload);
            }

            if (!this.subject.getSubjectMobile().isEmpty()) {
                SchemaPayload mobilePayload = new SchemaPayload();
                mobilePayload.setSchema(TrackerConstants.MOBILE_SCHEMA);
                mobilePayload.setData(this.subject.getSubjectMobile());
                context.add(mobilePayload);
            }
        }

        return completePayload2(payload, context, timestamp);
    }

    /**
     * @param payload Payload builder
     * @param context Custom context for the event
     * @param timestamp Optional user-provided timestamp for the event
     * @return A completed Payload
     */
    protected Payload completePayload2(Payload payload, List<SchemaPayload> context,
                                      long timestamp) {

        payload.add(Parameters.PLATFORM, this.platform.toString());
        payload.add(Parameters.APPID, this.appId);
        payload.add(Parameters.NAMESPACE, this.namespace);
        payload.add(Parameters.TRACKER_VERSION, this.trackerVersion);
        payload.add(Parameters.EID, Util.getEventId());

        // If timestamp is set to 0, generate one
        payload.add(Parameters.TIMESTAMP,
                (timestamp == 0 ? Util.getTimestamp() : Long.toString(timestamp)));

        // Encodes context data
        if (context != null) {
            SchemaPayload envelope = new SchemaPayload();
            envelope.setSchema(TrackerConstants.SCHEMA_CONTEXTS);

            // We can do better here, rather than re-iterate through the list
            List<Map> contextDataList = new LinkedList<Map>();
            for (SchemaPayload schemaPayload : context) {
                contextDataList.add(schemaPayload.getMap());
            }

            envelope.setData(contextDataList);
            payload.addMap(envelope.getMap(), this.base64Encoded, Parameters.CONTEXT_ENCODED,
                    Parameters.CONTEXT);
        }

        if (this.subject != null) {
            payload.addMap(new HashMap<String, Object>(subject.getSubject()));
        }

        return payload;
    }

    public void setPlatform(DevicePlatforms platform) {
        this.platform = platform;
    }

    public DevicePlatforms getPlatform() {
        return this.platform;
    }

    protected void setTrackerVersion(String version) {
        this.trackerVersion = version;
    }

    private void addTrackerPayload(Payload payload) {
        this.emitter.addToBuffer(payload);
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        return this.subject;
    }

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
                              List<SchemaPayload> context) {
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
                              List<SchemaPayload> context, long timestamp) {
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

        addTrackerPayload(payload);
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
                                     int value, List<SchemaPayload> context) {
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
                                     int value, List<SchemaPayload> context, long timestamp) {
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

        addTrackerPayload(payload);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                  A "schema" field identifying the schema against which the data is validated
     */
    public void trackUnstructuredEvent(SchemaPayload eventData) {
        trackUnstructuredEvent(eventData, null, 0);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                   A "schema" field identifying the schema against which the data is validated
     * @param context Custom context for the event
     */
    public void trackUnstructuredEvent(SchemaPayload eventData, List<SchemaPayload> context) {
        trackUnstructuredEvent(eventData, context, 0);
    }

    /**
     *
     * @param eventData The properties of the event. Has two field:
     *                   A "data" field containing the event properties and
     *                   A "schema" field identifying the schema against which the data is validated
     * @param timestamp Optional user-provided timestamp for the event
     */
    public void trackUnstructuredEvent(SchemaPayload eventData, long timestamp) {
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
    public void trackUnstructuredEvent(SchemaPayload eventData, List<SchemaPayload> context,
                                       long timestamp) {
        Payload payload = new TrackerPayload();
        SchemaPayload envelope = new SchemaPayload();

        envelope.setSchema(TrackerConstants.SCHEMA_UNSTRUCT_EVENT);
        envelope.setData(eventData.getMap());

        payload.add(Parameters.EVENT, TrackerConstants.EVENT_UNSTRUCTURED);
        payload.addMap(envelope.getMap(), base64Encoded,
                Parameters.UNSTRUCTURED_ENCODED, Parameters.UNSTRUCTURED);

        completePayload(payload, context, timestamp);

        addTrackerPayload(payload);
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
                                                 String currency, List<SchemaPayload> context,
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

        addTrackerPayload(payload);
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
                                          List<TransactionItem> items, List<SchemaPayload> context) {
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
                                          List<TransactionItem> items, List<SchemaPayload> context,
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
                    (List<SchemaPayload>) item.get(Parameters.CONTEXT),
                    timestamp);
        }

        addTrackerPayload(payload);
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
    public void trackScreenView(String name, String id, List<SchemaPayload> context) {
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
    public void trackScreenView(String name, String id, List<SchemaPayload> context,
                                long timestamp) {
        Preconditions.checkArgument(name != null || id != null);
        TrackerPayload trackerPayload = new TrackerPayload();

        trackerPayload.add(Parameters.SV_NAME, name);
        trackerPayload.add(Parameters.SV_ID, id);

        SchemaPayload payload = new SchemaPayload();
        payload.setSchema(TrackerConstants.SCHEMA_SCREEN_VIEW);
        payload.setData(trackerPayload);

        trackUnstructuredEvent(payload, context, timestamp);
    }
}
