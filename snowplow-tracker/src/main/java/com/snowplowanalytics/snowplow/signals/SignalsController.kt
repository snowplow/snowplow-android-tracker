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

package com.snowplowanalytics.snowplow.signals

import androidx.core.util.Consumer

/**
 * Reads attributes from Snowplow Signals for the current user or session.
 * Available as `tracker.signals`; requires a [com.snowplowanalytics.snowplow.configuration.SignalsConfiguration].
 */
interface SignalsController {

    /**
     * Reads the attributes of a public Signals service for the current user,
     * keyed on [SignalsAttributeKeys.CLIENT_SESSION_USER_ID] (`tracker.session.userId`).
     *
     * @param service Name of the service, which must be public in Signals.
     * @param callback Called on a background thread with the result.
     */
    fun getServiceAttributes(service: String, callback: Consumer<ServiceAttributesResult>)

    /**
     * Reads the attributes of a public Signals service for the given attribute key.
     * The identifier is filled in for [SignalsAttributeKeys.CLIENT_SESSION_USER_ID] (`tracker.session.userId`)
     * and [SignalsAttributeKeys.CLIENT_SESSION_SESSION_ID] (`tracker.session.sessionId`).
     * Other attribute keys need an explicit identifier.
     *
     * @param service Name of the service, which must be public in Signals.
     * @param attributeKey Name of the attribute key to read the attributes for.
     * @param callback Called on a background thread with the result.
     */
    fun getServiceAttributes(
        service: String,
        attributeKey: String,
        callback: Consumer<ServiceAttributesResult>
    )

    /**
     * Reads the attributes of a public Signals service for the given attribute key and identifier.
     *
     * @param service Name of the service, which must be public in Signals.
     * @param attributeKey Name of the attribute key to read the attributes for.
     * @param identifier Value of the attribute key, a UUID. If null, it is filled in for the
     * [SignalsAttributeKeys] built-in keys from the current session.
     * @param callback Called on a background thread with the result.
     */
    fun getServiceAttributes(
        service: String,
        attributeKey: String,
        identifier: String?,
        callback: Consumer<ServiceAttributesResult>
    )

    /**
     * Starts calling the handler with interventions Signals sends for the current user and session.
     * Adding a handler with an existing ID replaces it.
     *
     * The tracker subscribes to interventions while at least one handler is registered and the app is
     * in the foreground, using `client_session_user_id` and `client_session_session_id` from the session.
     * It subscribes after the first event is tracked, and resubscribes when the session changes.
     * Each intervention is tracked as an `intervention_receive` event, and each handler's outcome as
     * `intervention_handle` or `intervention_handle_error`.
     *
     * @param id Identifies the handler, e.g. to remove it later.
     * @param handler Called on the main thread with each intervention.
     */
    fun addInterventionHandler(id: String, handler: InterventionHandler)

    /**
     * Stops calling the handler with the given ID. Unsubscribes when no handlers are left.
     *
     * @param id ID the handler was added with.
     */
    fun removeInterventionHandler(id: String)
}
