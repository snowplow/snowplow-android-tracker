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

import androidx.core.util.Consumer
import com.snowplowanalytics.core.session.Session
import com.snowplowanalytics.core.session.SessionConfigurationInterface
import com.snowplowanalytics.core.tracker.TrackerDefaults
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.TimeMeasure
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * The [SessionConfiguration] is used to configure session behaviour. 
 * Session data is stored as [SessionState], and is appended to every event as an entity (`client_session`), by default. 
 * 
 * Session data is maintained for the life of the application being installed on a device.
 * A new session will be created if the session information is not accessed within a configurable timeout.
 *
 * A new session will be created based on:
 * - the timeout set for the inactivity of app when in foreground;
 * - the timeout set for the inactivity of app when in background.
 *
 * @see [TrackerConfiguration.sessionContext]
 */
open class SessionConfiguration : SessionConfigurationInterface, Configuration {

    private var _isPaused: Boolean? = null
    internal var isPaused: Boolean
        get() = _isPaused ?: sourceConfig?.isPaused ?: false
        set(value) { _isPaused = value }

    /**
     * Fallback configuration to read from in case requested values are not present in this configuration.
     */
    internal var sourceConfig: SessionConfiguration? = null

    private var _foregroundTimeout: TimeMeasure? = null
    override var foregroundTimeout: TimeMeasure
        get() = _foregroundTimeout ?: sourceConfig?.foregroundTimeout ?: TimeMeasure(TrackerDefaults.foregroundTimeout, TimeUnit.SECONDS)
        set(value) { _foregroundTimeout = value }

    private var _backgroundTimeout: TimeMeasure? = null
    override var backgroundTimeout: TimeMeasure
        get() = _backgroundTimeout ?: sourceConfig?.backgroundTimeout ?: TimeMeasure(TrackerDefaults.backgroundTimeout, TimeUnit.SECONDS)
        set(value) { _backgroundTimeout = value }

    private var _onSessionUpdate: Consumer<SessionState>? = null
    /**
     * The callback called every time the session is updated.
     */
    override var onSessionUpdate: Consumer<SessionState>?
        get() = _onSessionUpdate ?: sourceConfig?.onSessionUpdate
        set(value) { _onSessionUpdate = value }

    /**
     * This will set up the session behaviour of the tracker.
     * @param foregroundTimeout The timeout set for the inactivity of app when in foreground.
     * @param backgroundTimeout The timeout set for the inactivity of app when in background.
     */
    constructor(foregroundTimeout: TimeMeasure? = null, backgroundTimeout: TimeMeasure? = null) {
        foregroundTimeout?.let { this._foregroundTimeout = it }
        backgroundTimeout?.let { this._backgroundTimeout = it }
    }

    // Builders
    
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
    /**
     * This constructor is used in remote configuration.
     */
    constructor(jsonObject: JSONObject) : this(
        TimeMeasure(30, TimeUnit.MINUTES),
        TimeMeasure(30, TimeUnit.MINUTES)
    ) {
        if (jsonObject.has("foregroundTimeout")) {
            val foregroundTimeout = jsonObject.getInt("foregroundTimeout")
            this._foregroundTimeout = TimeMeasure(foregroundTimeout.toLong(), TimeUnit.SECONDS)
        }
        if (jsonObject.has("backgroundTimeout")) {
            val backgroundTimeout = jsonObject.getInt("backgroundTimeout")
            this._backgroundTimeout = TimeMeasure(backgroundTimeout.toLong(), TimeUnit.SECONDS)
        }
    }
}
