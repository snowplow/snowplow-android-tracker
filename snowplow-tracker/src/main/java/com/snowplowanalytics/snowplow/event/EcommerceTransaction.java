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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EcommerceTransaction extends AbstractPrimitive {

    @NonNull
    public final String orderId;
    @NonNull
    public final Double totalValue;
    @NonNull
    public final List<EcommerceTransactionItem> items;
    @Nullable
    public String affiliation;
    @Nullable
    public Double taxValue;
    @Nullable
    public Double shipping;
    @Nullable
    public String city;
    @Nullable
    public String state;
    @Nullable
    public String country;
    @Nullable
    public String currency;

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
        public T items(@NonNull EcommerceTransactionItem... itemArgs) {
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
        @NonNull
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    @NonNull
    public static Builder<?> builder() {
        return new Builder2();
    }

    protected EcommerceTransaction(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.orderId);
        Preconditions.checkArgument(!builder.orderId.isEmpty(), "orderId cannot be empty");
        Preconditions.checkNotNull(builder.totalValue);
        Preconditions.checkNotNull(builder.items);

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

    public EcommerceTransaction(@NonNull String orderId, @NonNull Double totalValue, @NonNull List<EcommerceTransactionItem> items) {
        Preconditions.checkNotNull(orderId);
        Preconditions.checkArgument(!orderId.isEmpty(), "orderId cannot be empty");
        Preconditions.checkNotNull(totalValue);
        Preconditions.checkNotNull(items);
        this.orderId = orderId;
        this.totalValue = totalValue;
        this.items = new ArrayList<>(items);
    }

    // Builder methods

    @NonNull
    public EcommerceTransaction affiliation(@Nullable String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    @NonNull
    public EcommerceTransaction taxValue(@Nullable Double taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    @NonNull
    public EcommerceTransaction shipping(@Nullable Double shipping) {
        this.shipping = shipping;
        return this;
    }

    @NonNull
    public EcommerceTransaction city(@Nullable String city) {
        this.city = city;
        return this;
    }

    @NonNull
    public EcommerceTransaction state(@Nullable String state) {
        this.state = state;
        return this;
    }

    @NonNull
    public EcommerceTransaction country(@Nullable String country) {
        this.country = country;
        return this;
    }

    @NonNull
    public EcommerceTransaction currency(@Nullable String currency) {
        this.currency = currency;
        return this;
    }


    // Public methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>(9);
        payload.put(Parameters.TR_ID, orderId);
        payload.put(Parameters.TR_TOTAL, Double.toString(totalValue));
        if (affiliation != null) payload.put(Parameters.TR_AFFILIATION, affiliation);
        if (taxValue != null) payload.put(Parameters.TR_TAX, Double.toString(taxValue));
        if (shipping != null) payload.put(Parameters.TR_SHIPPING, Double.toString(shipping));
        if (city != null) payload.put(Parameters.TR_CITY, city);
        if (state != null) payload.put(Parameters.TR_STATE, state);
        if (country != null) payload.put(Parameters.TR_COUNTRY, country);
        if (currency != null) payload.put(Parameters.TR_CURRENCY, currency);
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
    @NonNull
    public List<EcommerceTransactionItem> getItems() {
        return this.items;
    }

    @Override
    public void endProcessing(@NonNull Tracker tracker) {
        for(EcommerceTransactionItem item : items) {
            item.setOrderId(orderId);
            tracker.track(item);
        }
    }
}
