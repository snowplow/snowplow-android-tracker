/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceTransaction extends AbstractPrimitive {

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

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

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
         * @return itself
         */
        @NonNull
        public T orderId(@NonNull String orderId) {
            this.orderId = orderId;
            return self();
        }

        /**
         * @param totalValue Total transaction value
         * @return itself
         */
        @NonNull
        public T totalValue(@NonNull Double totalValue) {
            this.totalValue = totalValue;
            return self();
        }

        /**
         * @param affiliation Transaction affiliation
         * @return itself
         */
        @NonNull
        public T affiliation(@NonNull String affiliation) {
            this.affiliation = affiliation;
            return self();
        }

        /**
         * @param taxValue Transaction tax value
         * @return itself
         */
        @NonNull
        public T taxValue(@NonNull Double taxValue) {
            this.taxValue = taxValue;
            return self();
        }

        /**
         * @param shipping Delivery cost charged
         * @return itself
         */
        @NonNull
        public T shipping(@NonNull Double shipping) {
            this.shipping = shipping;
            return self();
        }

        /**
         * @param city Delivery address city
         * @return itself
         */
        @NonNull
        public T city(@NonNull String city) {
            this.city = city;
            return self();
        }

        /**
         * @param state Delivery address state
         * @return itself
         */
        @NonNull
        public T state(@NonNull String state) {
            this.state = state;
            return self();
        }

        /**
         * @param country Delivery address country
         * @return itself
         */
        @NonNull
        public T country(@NonNull String country) {
            this.country = country;
            return self();
        }

        /**
         * @param currency The currency the price is expressed in
         * @return itself
         */
        @NonNull
        public T currency(@NonNull String currency) {
            this.currency = currency;
            return self();
        }

        /**
         * @param items The items in the transaction
         * @return itself
         */
        @NonNull
        public T items(@NonNull List<EcommerceTransactionItem> items) {
            this.items = items;
            return self();
        }

        /**
         * @param itemArgs The items as a varargs argument
         * @return itself
         */
        @NonNull
        public T items(EcommerceTransactionItem... itemArgs) {
            List<EcommerceTransactionItem> items = new ArrayList<>();
            Collections.addAll(items, itemArgs);
            this.items = items;
            return self();
        }

        @NonNull
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

    protected EcommerceTransaction(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.orderId);
        Preconditions.checkNotNull(builder.totalValue);
        Preconditions.checkNotNull(builder.items);
        Preconditions.checkArgument(!builder.orderId.isEmpty(), "orderId cannot be empty");

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

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>(9);
        payload.put(Parameters.TR_ID, this.orderId);
        payload.put(Parameters.TR_TOTAL, Double.toString(this.totalValue));
        payload.put(Parameters.TR_AFFILIATION, this.affiliation);
        payload.put(Parameters.TR_TAX,
                this.taxValue != null ? Double.toString(this.taxValue) : null);
        payload.put(Parameters.TR_SHIPPING,
                this.shipping != null ? Double.toString(this.shipping) : null);
        payload.put(Parameters.TR_CITY, this.city);
        payload.put(Parameters.TR_STATE, this.state);
        payload.put(Parameters.TR_COUNTRY, this.country);
        payload.put(Parameters.TR_CURRENCY, this.currency);
        return payload;
    }

    @Override
    public @NonNull String getName() {
        return TrackerConstants.EVENT_ECOMM;
    }

    /**
     * The list of Transaction Items passed with the event.
     *
     * @return the items.
     */
    public List<EcommerceTransactionItem> getItems() {
        return this.items;
    }

    @Override
    public void endProcessing(Tracker tracker) {
        // Track each item individually
        for(EcommerceTransactionItem item : items) {
            item.setDeviceCreatedTimestamp(getDeviceCreatedTimestamp());
            tracker.track(item);
        }
    }
}
