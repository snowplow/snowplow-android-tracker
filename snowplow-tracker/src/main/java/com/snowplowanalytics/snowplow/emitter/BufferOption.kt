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
package com.snowplowanalytics.snowplow.emitter

/**
 * BufferOption is used to set how many events will be in one request to the collector.
 */
enum class BufferOption(val code: Int) {
    /**
     * Sends both GET and POST requests with only a single event.
     * This is the default setting.
     * Can cause a spike in network traffic if used in correlation with a large amount of events.
     */
    Single(1),

    /**
     * Sends POST requests in groups of 10 events.
     * All GET requests will still emit one at a time.
     */
    SmallGroup(10),

    /**
     * Sends POST requests in groups of 25 events.
     * Useful for situations where many events need to be sent.
     * All GET requests will still emit one at a time.
     */
    LargeGroup(25);

    companion object {
        fun fromString(string: String): BufferOption? {
            return when (string) {
                "Single" -> Single
                "SmallGroup" -> SmallGroup
                "DefaultGroup" -> SmallGroup
                "LargeGroup" -> LargeGroup
                "HeavyGroup" -> LargeGroup
                else -> null
            }
        }
    }
}
