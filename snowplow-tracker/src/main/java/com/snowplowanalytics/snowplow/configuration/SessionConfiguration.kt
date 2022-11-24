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
    @JvmField
    var foregroundTimeout: TimeMeasure,
    
    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * background.
     */
    @JvmField
    var backgroundTimeout: TimeMeasure
) : SessionConfigurationInterface, Configuration {
    
    /**
     * The callback called everytime the session is updated.
     */
    @JvmField
    var onSessionUpdate: Consumer<SessionState>? = null
    
    // Getters and Setters
    /**
     * @see .foregroundTimeout
     */
    override fun getForegroundTimeout(): TimeMeasure {
        return foregroundTimeout
    }

    /**
     * @see .foregroundTimeout
     */
    override fun setForegroundTimeout(foregroundTimeout: TimeMeasure) {
        this.foregroundTimeout = foregroundTimeout
    }

    /**
     * @see .backgroundTimeout
     */
    override fun getBackgroundTimeout(): TimeMeasure {
        return backgroundTimeout
    }

    /**
     * @see .backgroundTimeout
     */
    override fun setBackgroundTimeout(backgroundTimeout: TimeMeasure) {
        this.backgroundTimeout = backgroundTimeout
    }

    /**
     * @see .onSessionUpdate
     */
    override fun getOnSessionUpdate(): Consumer<SessionState>? {
        return onSessionUpdate
    }

    /**
     * @see .onSessionUpdate
     */
    override fun setOnSessionUpdate(onSessionUpdate: Consumer<SessionState>?) {
        this.onSessionUpdate = onSessionUpdate
    }
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
