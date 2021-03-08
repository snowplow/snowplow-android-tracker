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

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.HashMap;
import java.util.Map;

public class EcommerceTransactionItem extends AbstractPrimitive {

    @Nullable
    private String orderId;
    @NonNull
    public final String sku;
    @NonNull
    public final Double price;
    @NonNull
    public final Integer quantity;
    @Nullable
    public String name;
    @Nullable
    public String category;
    @Nullable
    public String currency;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String itemId;
        private String sku;
        private Double price;
        private Integer quantity;
        private String name;
        private String category;
        private String currency;

        /**
         * @param itemId Item ID
         * @return itself
         */
        @NonNull
        public T itemId(@NonNull String itemId) {
            this.itemId = itemId;
            return self();
        }

        /**
         * @param sku Item SKU
         * @return itself
         */
        @NonNull
        public T sku(@NonNull String sku) {
            this.sku = sku;
            return self();
        }

        /**
         * @param price Item price
         * @return itself
         */
        @NonNull
        public T price(@NonNull Double price) {
            this.price = price;
            return self();
        }

        /**
         * @param quantity Item quantity
         * @return itself
         */
        @NonNull
        public T quantity(@NonNull Integer quantity) {
            this.quantity = quantity;
            return self();
        }

        /**
         * @param name Item name
         * @return itself
         */
        @NonNull
        public T name(@NonNull String name) {
            this.name = name;
            return self();
        }

        /**
         * @param category Item category
         * @return itself
         */
        @NonNull
        public T category(@NonNull String category) {
            this.category = category;
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

        @NonNull
        public EcommerceTransactionItem build() {
            return new EcommerceTransactionItem(this);
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

    private EcommerceTransactionItem(@NonNull Builder<?> builder) {
        super(builder);

        // Precondition checks
        Preconditions.checkNotNull(builder.sku);
        Preconditions.checkNotNull(builder.price);
        Preconditions.checkNotNull(builder.quantity);
        Preconditions.checkArgument(!builder.sku.isEmpty(), "sku cannot be empty");

        this.orderId = builder.itemId;
        this.sku = builder.sku;
        this.price = builder.price;
        this.quantity = builder.quantity;
        this.name = builder.name;
        this.category = builder.category;
        this.currency = builder.currency;
    }

    public EcommerceTransactionItem(@NonNull String sku, double price, int quantity) {
        Preconditions.checkNotNull(sku);
        Preconditions.checkArgument(!sku.isEmpty(), "sku cannot be empty");
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
    }

    // Builder methods

    @NonNull
    public EcommerceTransactionItem name(@Nullable String name) {
        this.name = name;
        return this;
    }

    @NonNull
    public EcommerceTransactionItem category(@Nullable String category) {
        this.category = category;
        return this;
    }

    @NonNull
    public EcommerceTransactionItem currency(@Nullable String currency) {
        this.currency = currency;
        return this;
    }

    // Public methods

    public void setOrderId(@NonNull String orderId) {
        Preconditions.checkNotNull(orderId);
        Preconditions.checkArgument(!orderId.isEmpty(), "orderId cannot be empty");
        this.orderId = orderId;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String, Object> payload = new HashMap<>();
        if (orderId != null) payload.put(Parameters.TI_ITEM_ID, orderId);
        payload.put(Parameters.TI_ITEM_SKU, sku);
        payload.put(Parameters.TI_ITEM_PRICE, Double.toString(price));
        payload.put(Parameters.TI_ITEM_QUANTITY, Integer.toString(quantity));
        if (name != null) payload.put(Parameters.TI_ITEM_NAME, name);
        if (category != null) payload.put(Parameters.TI_ITEM_CATEGORY, category);
        if (currency != null) payload.put(Parameters.TI_ITEM_CURRENCY, currency);
        return payload;
    }

    @Override
    public @NonNull String getName() {
        return TrackerConstants.EVENT_ECOMM_ITEM;
    }
}
