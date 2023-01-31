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
