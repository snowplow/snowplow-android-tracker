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
package com.snowplowanalytics.core.session

import androidx.core.util.Consumer
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.TimeMeasure
import java.util.concurrent.TimeUnit

class SessionConfigurationUpdate @JvmOverloads constructor(
    foregroundTimeout: TimeMeasure = TimeMeasure(30, TimeUnit.MINUTES), 
    backgroundTimeout: TimeMeasure = TimeMeasure(30, TimeUnit.MINUTES)
) : SessionConfiguration(foregroundTimeout, backgroundTimeout) {
    
    var sourceConfig: SessionConfiguration? = null
    var isPaused = false
    private var foregroundTimeoutUpdated = false
    private var backgroundTimeoutUpdated = false

    override var foregroundTimeout: TimeMeasure
        get() = if (sourceConfig == null || foregroundTimeoutUpdated) super.foregroundTimeout else sourceConfig!!.foregroundTimeout
        set(value) {
            super.foregroundTimeout = value
            foregroundTimeoutUpdated = true
        }
    
    override var backgroundTimeout: TimeMeasure
        get() = if (sourceConfig == null || backgroundTimeoutUpdated) super.backgroundTimeout else sourceConfig!!.backgroundTimeout
        set(value) {
            super.backgroundTimeout = value
            backgroundTimeoutUpdated = true
        }
    
    override var onSessionUpdate: Consumer<SessionState>?
        get() = if (sourceConfig == null) null else sourceConfig!!.onSessionUpdate
        set(value) {
            // Can't update this
        }
}
