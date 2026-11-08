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

package com.snowplowanalytics.snowplow.configuration

import com.snowplowanalytics.snowplow.util.TimeMeasure
import java.util.concurrent.TimeUnit

/**
 * Configures the tracker to read attributes from, and receive interventions from, Snowplow Signals.
 * Pass it to [com.snowplowanalytics.snowplow.Snowplow.createTracker] to enable `tracker.signals`.
 *
 * Signals is read with the user and session identifiers from the `client_session` entity,
 * so session tracking must be enabled (default) and user anonymisation turned off.
 *
 * @param endpoint The Signals API endpoint, e.g. `https://signals.example.com`. `https://` is assumed if the scheme is omitted.
 */
open class SignalsConfiguration(
    /** The Signals API endpoint. `https://` is assumed if the scheme is omitted. */
    open var endpoint: String
) : Configuration {

    /**
     * Timeout for reading service attributes and for opening the interventions stream.
     * Default: 5 seconds.
     */
    open var timeout: TimeMeasure = TimeMeasure(5, TimeUnit.SECONDS)

    // Builders

    /**
     * Timeout for reading service attributes and for opening the interventions stream.
     * Default: 5 seconds.
     */
    fun timeout(timeout: TimeMeasure): SignalsConfiguration {
        this.timeout = timeout
        return this
    }

    // Copyable

    override fun copy(): Configuration {
        return SignalsConfiguration(endpoint).timeout(timeout)
    }
}
