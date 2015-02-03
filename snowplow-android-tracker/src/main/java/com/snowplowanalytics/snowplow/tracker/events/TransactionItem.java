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

import com.snowplowanalytics.snowplow.tracker.utils.Util;
import com.snowplowanalytics.snowplow.tracker.utils.payload.SchemaPayload;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;

import java.util.HashMap;
import java.util.List;

public class TransactionItem extends HashMap {

    public TransactionItem (String order_id, String sku, double price, int quantity, String name,
                            String category, String currency) {
        this(order_id,sku, price, quantity, name, category, currency, null);
    }

    public TransactionItem (String order_id, String sku, double price, int quantity, String name,
                            String category, String currency, List<SchemaPayload> context) {
        put(Parameters.EVENT, "ti");
        put(Parameters.TI_ITEM_ID, order_id);
        put(Parameters.TI_ITEM_SKU, sku);
        put(Parameters.TI_ITEM_NAME, name);
        put(Parameters.TI_ITEM_CATEGORY, category);
        put(Parameters.TI_ITEM_PRICE, price);
        put(Parameters.TI_ITEM_QUANTITY, quantity);
        put(Parameters.TI_ITEM_CURRENCY, currency);

        put(Parameters.CONTEXT, context);

        put(Parameters.TIMESTAMP, Util.getTimestamp());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public Object put(Object key, Object value) {
        if (value != null || value != "")
            return super.put(key, value);
        else
            return null;
    }
}
