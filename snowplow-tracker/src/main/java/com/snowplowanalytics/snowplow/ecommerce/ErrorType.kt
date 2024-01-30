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

package com.snowplowanalytics.snowplow.ecommerce

/** Type of transaction error. */
enum class ErrorType {
    /// The customer must provide another form of payment e.g. the card has expired.
    Hard,
    /// Temporary issues where retrying might be successful e.g. processor declined the transaction.
    Soft;

    override fun toString(): String {
        return when (this) {
            Hard -> "hard"
            Soft -> "soft"
        }
    }
}
