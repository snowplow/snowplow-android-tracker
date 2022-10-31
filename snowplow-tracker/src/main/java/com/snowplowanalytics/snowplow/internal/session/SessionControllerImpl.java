package com.snowplowanalytics.snowplow.internal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.tracker.SessionState;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import java.util.concurrent.TimeUnit;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionControllerImpl extends Controller implements SessionController {
    private final String TAG = SessionControllerImpl.class.getName();

    // Constructors

    public SessionControllerImpl(@NonNull ServiceProviderInterface serviceProvider) {
        super(serviceProvider);
    }

    // Control methods

    @Override
    public void pause() {
        getDirtyConfig().isPaused = true;
        getTracker().pauseSessionChecking();
    }

    @Override
    public void resume() {
        getDirtyConfig().isPaused = false;
        getTracker().resumeSessionChecking();
    }

    @Override
    public void startNewSession() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        session.startNewSession();
    }

    // Getters and Setters

    @Override
    public int getSessionIndex() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return session.getSessionIndex();
    }

    @NonNull
    @Override
    public String getSessionId() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return session.getState().getSessionId();
    }

    @NonNull
    @Override
    public String getUserId() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return session.getUserId();
    }

    @Override
    public boolean isInBackground() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return false;
        }
        return session.isBackground();
    }

    @Override
    public int getBackgroundIndex() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return session.getBackgroundIndex();
    }

    @Override
    public int getForegroundIndex() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return session.getForegroundIndex();
    }

    @NonNull
    @Override
    public TimeMeasure getForegroundTimeout() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(session.getForegroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout) {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        getDirtyConfig().foregroundTimeout = foregroundTimeout;
        getDirtyConfig().foregroundTimeoutUpdated = true;
        session.setForegroundTimeout(foregroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    @NonNull
    @Override
    public TimeMeasure getBackgroundTimeout() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(session.getBackgroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout) {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        getDirtyConfig().backgroundTimeout = backgroundTimeout;
        getDirtyConfig().backgroundTimeoutUpdated = true;
        session.setBackgroundTimeout(backgroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    /**
     * The callback called everytime the session is updated.
     */
    @Nullable
    @Override
    public Consumer<SessionState> getOnSessionUpdate() {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return null;
        }
        return session.onSessionUpdate;
    }

    /**
     * The callback called everytime the session is updated.
     */
    @Override
    public void setOnSessionUpdate(@Nullable Consumer<SessionState> onSessionUpdate) {
        Session session = getSession();
        if (session == null) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        session.onSessionUpdate = onSessionUpdate;
    }

    // Service method

    public boolean isEnabled() {
        return getTracker().getSession() != null;
    }

    // Private methods

    @NonNull
    private Tracker getTracker() {
        return serviceProvider.getOrMakeTracker();
    }

    @Nullable
    private Session getSession() {
        return serviceProvider.getOrMakeTracker().getSession();
    }

    @NonNull
    private SessionConfigurationUpdate getDirtyConfig() {
        return serviceProvider.getSessionConfigurationUpdate();
    }
}
