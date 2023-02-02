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
package com.snowplowanalytics.snowplow.controller

import com.snowplowanalytics.core.session.SessionConfigurationInterface

interface SessionController : SessionConfigurationInterface {
    /**
     * The session index.
     * A increasing number which helps to order the sequence of sessions.
     */
    val sessionIndex: Int?

    /**
     * The session identifier.
     * A unique identifier which is used to identify the session.
     */
    val sessionId: String?

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
