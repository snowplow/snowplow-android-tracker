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

import java.util.List;

public class EcommerceTransaction extends Event {

    private final String orderId;
    private final Double totalValue;
    private final String affiliation;
    private final Double taxValue;
    private final Double shipping;
    private final String city;
    private final String state;
    private final String country;
    private final String currency;
    private final List<EcommerceTransactionItem> items;

    public static abstract class Builder<T extends Builder<T>> extends Event.Builder<T> {

        private String orderId;
        private Double totalValue;
        private String affiliation;
        private Double taxValue;
        private Double shipping;
        private String city;
        private String state;
        private String country;
        private String currency;
        private List<EcommerceTransactionItem> items;

        /**
         * @param orderId ID of the eCommerce transaction
         */
        public T orderId(String orderId) {
            this.orderId = orderId;
            return self();
        }

        /**
         * @param totalValue Total transaction value
         */
        public T totalValue(Double totalValue) {
            this.totalValue = totalValue;
            return self();
        }

        /**
         * @param affiliation Transaction affiliation
         */
        public T affiliation(String affiliation) {
            this.affiliation = affiliation;
            return self();
        }

        /**
         * @param taxValue Transaction tax value
         */
        public T taxValue(Double taxValue) {
            this.taxValue = taxValue;
            return self();
        }

        /**
         * @param shipping Delivery cost charged
         */
        public T shipping(Double shipping) {
            this.shipping = shipping;
            return self();
        }

        /**
         * @param city Delivery address city
         */
        public T city(String city) {
            this.city = city;
            return self();
        }

        /**
         * @param state Delivery address state
         */
        public T state(String state) {
            this.state = state;
            return self();
        }

        /**
         * @param country Delivery address country
         */
        public T country(String country) {
            this.country = country;
            return self();
        }

        /**
         * @param currency The currency the price is expressed in
         */
        public T currency(String currency) {
            this.currency = currency;
            return self();
        }

        /**
         * @param items The items in the transaction
         */
        public T items(List<EcommerceTransactionItem> items) {
            this.items = items;
            return self();
        }

        public EcommerceTransaction build() {
            return new EcommerceTransaction(this);
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

    protected EcommerceTransaction(Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.affiliation);
        Preconditions.checkNotNull(builder.city);
        Preconditions.checkNotNull(builder.state);
        Preconditions.checkNotNull(builder.country);
        Preconditions.checkNotNull(builder.currency);
        Preconditions.checkArgument(!builder.orderId.isEmpty(), "orderId cannot be empty");
        Preconditions.checkArgument(!builder.affiliation.isEmpty(), "affiliation cannot be empty");
        Preconditions.checkArgument(!builder.city.isEmpty(), "city cannot be empty");
        Preconditions.checkArgument(!builder.state.isEmpty(), "state cannot be empty");
        Preconditions.checkArgument(!builder.country.isEmpty(), "country cannot be empty");
        Preconditions.checkArgument(!builder.currency.isEmpty(), "currency cannot be empty");

        this.orderId = builder.orderId;
        this.totalValue = builder.totalValue;
        this.affiliation = builder.affiliation;
        this.taxValue = builder.taxValue;
        this.shipping = builder.shipping;
        this.city = builder.city;
        this.state = builder.state;
        this.country = builder.country;
        this.currency = builder.currency;
        this.items = builder.items;
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @return the payload to be sent.
     */
    public TrackerPayload getPayload() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.EVENT, TrackerConstants.EVENT_ECOMM);
        payload.add(Parameters.TR_ID, this.orderId);
        payload.add(Parameters.TR_TOTAL, Double.toString(this.totalValue));
        payload.add(Parameters.TR_AFFILIATION, this.affiliation);
        payload.add(Parameters.TR_TAX, Double.toString(this.taxValue));
        payload.add(Parameters.TR_SHIPPING, Double.toString(this.shipping));
        payload.add(Parameters.TR_CITY, this.city);
        payload.add(Parameters.TR_STATE, this.state);
        payload.add(Parameters.TR_COUNTRY, this.country);
        payload.add(Parameters.TR_CURRENCY, this.currency);
        return putDefaultParams(payload);
    }

    /**
     * The list of Transaction Items passed with the event.
     *
     * @return the items.
     */
    public List<EcommerceTransactionItem> getItems() {
        return this.items;
    }
}