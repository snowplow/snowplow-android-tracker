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
package com.snowplowanalytics.snowplow.ecommerce

import com.snowplowanalytics.snowplow.ecommerce.entities.EcommScreenEntity
import com.snowplowanalytics.snowplow.ecommerce.entities.EcommUserEntity

/**
 * Controller for managing Ecommerce entities.
 */
interface EcommerceController {

    /**
     * Add an ecommerce Screen/Page entity to all subsequent events.
     * @param screen A EcommScreenEntity.
     */
    fun setEcommerceScreen(screen: EcommScreenEntity)

    /**
     * Add an ecommerce User entity to all subsequent events.
     * @param user A EcommUserEntity.
     */
    fun setEcommerceUser(user: EcommUserEntity)

    /**
     * Stop adding a Screen/Page entity to events.
     */
    fun removeEcommerceScreen()

    /**
     * Stop adding a User entity to events.
     */
    fun removeEcommerceUser()
}
