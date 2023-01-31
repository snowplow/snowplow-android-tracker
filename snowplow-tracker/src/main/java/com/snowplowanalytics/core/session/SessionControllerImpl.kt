package com.snowplowanalytics.core.session

import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import com.snowplowanalytics.core.Controller
import com.snowplowanalytics.core.tracker.Logger
import com.snowplowanalytics.core.tracker.ServiceProviderInterface
import com.snowplowanalytics.core.tracker.Tracker
import com.snowplowanalytics.snowplow.controller.SessionController
import com.snowplowanalytics.snowplow.tracker.SessionState
import com.snowplowanalytics.snowplow.util.TimeMeasure
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
class SessionControllerImpl  // Constructors
    (serviceProvider: ServiceProviderInterface) : Controller(serviceProvider), SessionController {
    private val TAG = SessionControllerImpl::class.java.name

    // Control methods
    override fun pause() {
        dirtyConfig.isPaused = true
        tracker.pauseSessionChecking()
    }

    override fun resume() {
        dirtyConfig.isPaused = false
        tracker.resumeSessionChecking()
    }

    override fun startNewSession() {
        val session = session
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled")
            return
        }
        session.startNewSession()
    }

    
    // Getters and Setters
    override val sessionIndex: Int?
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return -1
            }
            return session.sessionIndex
        }
    
    override val sessionId: String?
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return ""
            }
            return session.state?.sessionId
        }
    
    override val userId: String
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return ""
            }
            return session.userId
        }
    
    override val isInBackground: Boolean
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return false
            }
            return session.isBackground
        }
    
    override val backgroundIndex: Int
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return -1
            }
            return session.backgroundIndex
        }
    
    override val foregroundIndex: Int
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return -1
            }
            return session.foregroundIndex
        }

    override var foregroundTimeout: TimeMeasure
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return TimeMeasure(0, TimeUnit.SECONDS)
            }
            return TimeMeasure(session.foregroundTimeout, TimeUnit.MILLISECONDS)
        }
        set(foregroundTimeout) {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return
            }
            dirtyConfig.foregroundTimeout = foregroundTimeout
            session.foregroundTimeout = foregroundTimeout.convert(TimeUnit.MILLISECONDS)
        }
    
    override var backgroundTimeout: TimeMeasure
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return TimeMeasure(0, TimeUnit.SECONDS)
            }
            return TimeMeasure(session.backgroundTimeout, TimeUnit.MILLISECONDS)
        }
        set(backgroundTimeout) {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return
            }
            dirtyConfig.backgroundTimeout = backgroundTimeout
            session.backgroundTimeout =
                backgroundTimeout.convert(TimeUnit.MILLISECONDS)
        }
    
    override var onSessionUpdate: Consumer<SessionState>?
        get() {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return null
            }
            return session.onSessionUpdate
        }
        set(onSessionUpdate) {
            val session = session
            if (session == null) {
                Logger.track(TAG, "Attempt to access SessionController fields when disabled")
                return
            }
            session.onSessionUpdate = onSessionUpdate
        }

    // Service method
    val isEnabled: Boolean
        get() = tracker.session != null

    // Private methods
    private val tracker: Tracker
        get() = serviceProvider.getOrMakeTracker()
    private val session: Session?
        get() = serviceProvider.getOrMakeTracker().session
    private val dirtyConfig: SessionConfigurationUpdate
        get() = serviceProvider.sessionConfigurationUpdate
}
