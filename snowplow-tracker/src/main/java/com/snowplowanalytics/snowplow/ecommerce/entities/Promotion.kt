/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.snowplow.ecommerce.entities

data class Promotion(
    /**
     * The ID of the promotion.
     */
    val id: String,
    
    /**
     * The name of the promotion.
     */
    val name: String? = null,
    
    /**
     * List of SKUs or product IDs showcased in the promotion.
     */
    val productIds: List<String>? = null,
    
    /**
     * The position the promotion was presented in a list of promotions E.g. banner, slider.
     */
    val position: Int? = null,
    
    /**
     * Identifier/Name/Url for the creative presented on the promotion.
     */
    val creativeId: String? = null,
    
    /**
     * Type of the promotion delivery mechanism. E.g. popup, banner, intra-content
     */
    val type: String? = null,
    
    /**
     * The website slot in which the promotional content was added to.
     */
    val slot: String? = null
)
