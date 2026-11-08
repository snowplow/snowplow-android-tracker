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
 * Why a request to Signals failed.
 *
 * @property status The HTTP status code, or 0 if no request was made (e.g. no identifier was available) or no response was received.
 * @property message A description of the error, taken from the Signals response where it gives one.
 */
data class SignalsError(
    val status: Int,
    val message: String
)

/**
 * The outcome of [SignalsController.getServiceAttributes].
 * Exactly one of [attributes] and [error] is set.
 *
 * @property attributes The service's attributes by name, with `null` for attributes that have no value yet.
 * @property error Why the request failed.
 */
class ServiceAttributesResult private constructor(
    val attributes: Map<String, Any?>?,
    val error: SignalsError?
) {
    /** Whether the attributes were read. */
    val isSuccess: Boolean
        get() = attributes != null

    override fun toString(): String {
        return if (isSuccess) "ServiceAttributesResult(attributes=$attributes)"
        else "ServiceAttributesResult(error=$error)"
    }

    companion object {
        @JvmStatic
        fun success(attributes: Map<String, Any?>): ServiceAttributesResult {
            return ServiceAttributesResult(attributes, null)
        }

        @JvmStatic
        fun failure(status: Int, message: String): ServiceAttributesResult {
            return ServiceAttributesResult(null, SignalsError(status, message))
        }
    }
}
