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

import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An ecommerce event. */
public class EcommerceTransaction extends AbstractPrimitive {

    /** Identifier of the order. */
    @NonNull
    public final String orderId;
    /** Total amount of the order. */
    @NonNull
    public final Double totalValue;
    /** Items purchased. */
    @NonNull
    public final List<EcommerceTransactionItem> items;
    /** Identifies an affiliation. */
    @Nullable
    public String affiliation;
    /** Taxes applied to the purchase. */
    @Nullable
    public Double taxValue;
    /** Total amount for shipping. */
    @Nullable
    public Double shipping;
    /** City for shipping. */
    @Nullable
    public String city;
    /** State for shipping. */
    @Nullable
    public String state;
    /** Country for shipping. */
    @Nullable
    public String country;
    /** Currency used for totalValue and taxValue. */
    @Nullable
    public String currency;

    /**
     * Creates an ecommerce event.
     * @param orderId Identifier of the order.
     * @param totalValue Total amount of the order.
     * @param items Items purchased.
     */
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

    /** Identifies an affiliation. */
    @NonNull
    public EcommerceTransaction affiliation(@Nullable String affiliation) {
        this.affiliation = affiliation;
        return this;
    }

    /** Taxes applied to the purchase. */
    @NonNull
    public EcommerceTransaction taxValue(@Nullable Double taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    /** Total amount for shipping. */
    @NonNull
    public EcommerceTransaction shipping(@Nullable Double shipping) {
        this.shipping = shipping;
        return this;
    }

    /** City for shipping. */
    @NonNull
    public EcommerceTransaction city(@Nullable String city) {
        this.city = city;
        return this;
    }

    /** State for shipping. */
    @NonNull
    public EcommerceTransaction state(@Nullable String state) {
        this.state = state;
        return this;
    }

    /** Country for shipping. */
    @NonNull
    public EcommerceTransaction country(@Nullable String country) {
        this.country = country;
        return this;
    }

    /** Currency used for totalValue and taxValue. */
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

    /** The list of Transaction Items passed with the event. */
    @NonNull
    public List<EcommerceTransactionItem> getItems() {
        return this.items;
    }

    @Override
    public void endProcessing(@NonNull Tracker tracker) {
        for (EcommerceTransactionItem item : items) {
            item.orderId = orderId;
            tracker.track(item);
        }
    }
}
