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

package com.snowplowanalytics.snowplow.event;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.HashMap;
import java.util.Map;

/** An ecommerce item event. */
public class EcommerceTransactionItem extends AbstractPrimitive {

    /** Stock Keeping Unit of the item. */
    @NonNull
    public final String sku;
    /** Price of the item. */
    @NonNull
    public final Double price;
    /** Quantity of the item. */
    @NonNull
    public final Integer quantity;
    /** Name of the item. */
    @Nullable
    public String name;
    /** Category of the item. */
    @Nullable
    public String category;
    /** Currency used for the price of the item. */
    @Nullable
    public String currency;
    /** OrderID of the order that contains this item. */
    @Nullable
    public String orderId;

    /**
     Creates an ecommerce item event.
     @param sku Stock Keeping Unit of the item.
     @param price Price of the item.
     @param quantity Quantity of the item.
     */
    public EcommerceTransactionItem(@NonNull String sku, double price, int quantity) {
        Preconditions.checkNotNull(sku);
        Preconditions.checkArgument(!sku.isEmpty(), "sku cannot be empty");
        this.sku = sku;
        this.price = price;
        this.quantity = quantity;
    }

    // Builder methods

    /** Name of the item. */
    @NonNull
    public EcommerceTransactionItem name(@Nullable String name) {
        this.name = name;
        return this;
    }

    /** Category of the item. */
    @NonNull
    public EcommerceTransactionItem category(@Nullable String category) {
        this.category = category;
        return this;
    }

    /** Currency used for the price of the item. */
    @NonNull
    public EcommerceTransactionItem currency(@Nullable String currency) {
        this.currency = currency;
        return this;
    }

    /** OrderID of the order that contains this item. */
    @NonNull
    public EcommerceTransactionItem orderId(@Nullable String orderId) {
        this.orderId = orderId;
        return this;
    }

    // Public methods

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
