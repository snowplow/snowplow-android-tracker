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

/**
 * Controller for managing Ecommerce entities.
 */
interface EcommerceController {

    /**
     * Add an ecommerce Screen/Page entity to all subsequent events.
     * @param type The type of screen that was visited, e.g. homepage, product details, cart, checkout, etc.
     * @param language The language that the screen is based in.
     * @param locale The locale version of the app that is running.
     */
    fun setEcommerceScreen(type: String, language: String? = null, locale: String? = null)

    /**
     * Add an ecommerce User entity to all subsequent events.
     * @param id The user ID.
     * @param isGuest Whether or not the user is a guest.
     * @param email The user's email address.
     */
    fun setEcommerceUser(id: String, isGuest: Boolean? = null, email: String? = null)

    /**
     * Stop adding a Screen/Page entity to events.
     */
    fun removeEcommerceScreen()

    /**
     * Stop adding a User entity to events.
     */
    fun removeEcommerceUser()
}
