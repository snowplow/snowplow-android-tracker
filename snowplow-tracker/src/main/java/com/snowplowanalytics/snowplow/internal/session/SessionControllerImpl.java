package com.snowplowanalytics.snowplow.internal.session;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

import java.util.concurrent.TimeUnit;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SessionControllerImpl implements SessionController {

    private final String TAG = SessionControllerImpl.class.getName();

    @NonNull
    private Tracker tracker;

    // Constructors

    public SessionControllerImpl(@NonNull Tracker tracker) {
        this.tracker = tracker;
    }

    // Control methods

    @Override
    public void pause() {
        tracker.pauseSessionChecking();
    }

    @Override
    public void resume() {
        tracker.resumeSessionChecking();
    }

    @Override
    public void startNewSession() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        tracker.getSession().startNewSession();
    }

    // Getters and Setters

    @Override
    public int getSessionIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return tracker.getSession().getSessionIndex();
    }

    @NonNull
    @Override
    public String getSessionId() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return tracker.getSession().getCurrentSessionId();
    }

    @NonNull
    @Override
    public String getUserId() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return tracker.getSession().getUserId();
    }

    @Override
    public boolean isInBackground() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return false;
        }
        return tracker.getSession().isBackground();
    }

    @Override
    public int getBackgroundIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return tracker.getSession().getBackgroundIndex();
    }

    @Override
    public int getForegroundIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return tracker.getSession().getForegroundIndex();
    }

    @NonNull
    @Override
    public TimeMeasure getForegroundTimeout() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(tracker.getSession().getForegroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout) {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        tracker.getSession().setForegroundTimeout(foregroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    @NonNull
    @Override
    public TimeMeasure getBackgroundTimeout() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(tracker.getSession().getBackgroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout) {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        tracker.getSession().setBackgroundTimeout(backgroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    // Service method

    public boolean isEnabled() {
        return tracker.getSession() != null;
    }
}
