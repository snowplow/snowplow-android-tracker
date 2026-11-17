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

/**
 * An intervention pushed by Signals when its criteria are met.
 *
 * @property interventionId Unique ID of this intervention instance.
 * @property name Name of the intervention definition.
 * @property version Version of the intervention definition.
 * @property attributes Attribute values sent with the intervention, by name.
 * @property targetAttributeKey The attribute key the intervention was sent for.
 */
data class Intervention(
    val interventionId: String,
    val name: String,
    val version: Int,
    val attributes: Map<String, Any?>,
    val targetAttributeKey: InterventionAttributeKey
)

/**
 * The attribute key an intervention was sent for.
 *
 * @property name Name of the attribute key, e.g. `client_session_user_id`.
 * @property id Value of the attribute key.
 */
data class InterventionAttributeKey(
    val name: String,
    val id: String
)

/**
 * Receives interventions from Signals. Called on the main thread.
 * Returning normally counts as handled; throwing counts as a handling error.
 */
fun interface InterventionHandler {
    fun handle(intervention: Intervention)
}
