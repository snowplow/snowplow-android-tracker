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
}
