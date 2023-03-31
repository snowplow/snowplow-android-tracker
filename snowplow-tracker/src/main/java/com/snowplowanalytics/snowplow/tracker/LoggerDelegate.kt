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
package com.snowplowanalytics.snowplow.tracker

/**
 * Provide a LoggerDelegate when configuring a new tracker to log to a custom destination 
 * (rather than STDOUT).
 * 
 * @see com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
 */
interface LoggerDelegate {
    /**
     * Error Level Logging
     * @param tag the log tag
     * @param msg the log message
     */
    fun error(tag: String, msg: String)

    /**
     * Debug Level Logging
     * @param tag the log tag
     * @param msg the log message
     */
    fun debug(tag: String, msg: String)

    /**
     * Verbose Level Logging
     * @param tag the log tag
     * @param msg the log message
     */
    fun verbose(tag: String, msg: String)
}
