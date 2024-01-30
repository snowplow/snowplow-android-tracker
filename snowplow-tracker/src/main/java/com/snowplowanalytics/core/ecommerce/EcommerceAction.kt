/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.ecommerce

/**
 * Available types of ecommerce action. Each one is a different event type.
 */
enum class EcommerceAction {
    // lowercase to match the schema
    add_to_cart,
    remove_from_cart,
    product_view,
    list_click,
    list_view,
    promo_click,
    promo_view,
    checkout_step,
    transaction,
    trns_error,
    refund
}
