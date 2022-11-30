package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.session.SessionConfigurationInterface

interface SessionController : SessionConfigurationInterface {
    /**
     * The session index.
     * A increasing number which helps to order the sequence of sessions.
     */
    val sessionIndex: Int

    /**
     * The session identifier.
     * A unique identifier which is used to identify the session.
     */
    val sessionId: String

    /**
     * The session user identifier.
     * It identifies this app installation and it doesn't change for the life of the app.
     * It will change only when the app is uninstalled and installed again.
     * An app update doesn't change the value.
     */
    val userId: String

    /**
     * Whether the app is currently in background state or in foreground state.
     */
    val isInBackground: Boolean

    /**
     * Count the number of background transitions in the current session.
     */
    val backgroundIndex: Int

    /**
     * Count the number of foreground transitions in the current session.
     */
    val foregroundIndex: Int

    /**
     * Pause the session tracking.
     * Meanwhile the session is paused it can't expire and can't be updated.
     */
    fun pause()

    /**
     * Resume the session tracking.
     */
    fun resume()

    /**
     * Expire the current session also if the timeout is not triggered.
     */
    fun startNewSession()
}
