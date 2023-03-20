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
package com.snowplowanalytics.snowplow.configuration

import androidx.core.util.Consumer
import com.snowplowanalytics.core.session.SessionConfigurationInterface
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.TimeMeasure
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * This class represents the configuration of the applications session.
 * The SessionConfiguration can be used to setup the behaviour of sessions.
 *
 * A session is a context which is appended to each event sent.
 * The values it brings can change based on:
 * - the timeout set for the inactivity of app when in foreground;
 * - the timeout set for the inactivity of app when in background.
 *
 * Session data is maintained for the life of the application being installed on a device.
 * A new session will be created if the session information is not accessed within a configurable timeout.
 */
open class SessionConfiguration
/**
 * This will setup the session behaviour of the tracker.
 * @param foregroundTimeout The timeout set for the inactivity of app when in foreground.
 * @param backgroundTimeout The timeout set for the inactivity of app when in background.
 */(
    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * foreground.
     */
    override var foregroundTimeout: TimeMeasure,
    
    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * background.
     */
    override var backgroundTimeout: TimeMeasure
) : SessionConfigurationInterface, Configuration {
    
    /**
     * The callback called everytime the session is updated.
     */
    override var onSessionUpdate: Consumer<SessionState>? = null
    
    // Builders
    
    /**
     * @see .onSessionUpdate
     */
    fun onSessionUpdate(onSessionUpdate: Consumer<SessionState>?): SessionConfiguration {
        this.onSessionUpdate = onSessionUpdate
        return this
    }

    // Copyable
    override fun copy(): Configuration {
        return SessionConfiguration(foregroundTimeout, backgroundTimeout)
            .onSessionUpdate(onSessionUpdate)
    }

    // JSON Formatter
    constructor(jsonObject: JSONObject) : this(
        TimeMeasure(30, TimeUnit.MINUTES),
        TimeMeasure(30, TimeUnit.MINUTES)
    ) {
        val foregroundTimeout = jsonObject.optInt("foregroundTimeout", 1800)
        val backgroundTimeout = jsonObject.optInt("backgroundTimeout", 1800)
        this.foregroundTimeout = TimeMeasure(foregroundTimeout.toLong(), TimeUnit.SECONDS)
        this.backgroundTimeout = TimeMeasure(backgroundTimeout.toLong(), TimeUnit.SECONDS)
    }
}