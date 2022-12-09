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
    
    fun onSessionUpdate(): Consumer<SessionState>? {
        return if (sourceConfig == null) null else sourceConfig!!.onSessionUpdate
    }

    // foregroundTimeout flag
    var foregroundTimeoutUpdated = false
    fun foregroundTimeout(): TimeMeasure {
        return if (sourceConfig == null || foregroundTimeoutUpdated) super.foregroundTimeout else sourceConfig!!.foregroundTimeout
    }

    // backgroundTimeout flag
    var backgroundTimeoutUpdated = false
    fun backgroundTimeout(): TimeMeasure {
        return if (sourceConfig == null || backgroundTimeoutUpdated) super.backgroundTimeout else sourceConfig!!.backgroundTimeout
    }
}
