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
package com.snowplowanalytics.core.session

import androidx.core.util.Consumer
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.TimeMeasure

interface SessionConfigurationInterface {
    /**
     * The amount of time that can elapse before the session id is updated 
     * while the app is in the foreground.
     */
    var foregroundTimeout: TimeMeasure
    
    /**
     * The amount of time that can elapse before the session id is updated 
     * while the app is in the background.
     */
    var backgroundTimeout: TimeMeasure
    

    /**
     * The callback called every time the session is updated.
     */
    var onSessionUpdate: Consumer<SessionState>?


    /**
     * If enabled, will be able to continue the previous session when the app is closed and reopened (if it doesn't timeout).
     * Disabled by default, which means that every restart of the app starts a new session.
     * When enabled, every event will result in the session being updated in the UserDefaults.
     */
    var continueSessionOnRestart: Boolean
}
