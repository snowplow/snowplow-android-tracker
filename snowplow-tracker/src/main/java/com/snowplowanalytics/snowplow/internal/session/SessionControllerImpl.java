package com.snowplowanalytics.snowplow.internal.session;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.controller.SessionController;
import com.snowplowanalytics.snowplow.internal.Controller;
import com.snowplowanalytics.snowplow.internal.tracker.ServiceProviderInterface;
import com.snowplowanalytics.snowplow.internal.tracker.Tracker;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
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
        getTracker().pauseSessionChecking();
    }

    @Override
    public void resume() {
        getTracker().resumeSessionChecking();
    }

    @Override
    public void startNewSession() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        getSession().startNewSession();
    }

    // Getters and Setters

    @Override
    public int getSessionIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return getSession().getSessionIndex();
    }

    @NonNull
    @Override
    public String getSessionId() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return getSession().getCurrentSessionId();
    }

    @NonNull
    @Override
    public String getUserId() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return "";
        }
        return getSession().getUserId();
    }

    @Override
    public boolean isInBackground() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return false;
        }
        return getSession().isBackground();
    }

    @Override
    public int getBackgroundIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return getSession().getBackgroundIndex();
    }

    @Override
    public int getForegroundIndex() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return -1;
        }
        return getSession().getForegroundIndex();
    }

    @NonNull
    @Override
    public TimeMeasure getForegroundTimeout() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(getSession().getForegroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout) {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        getSession().setForegroundTimeout(foregroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    @NonNull
    @Override
    public TimeMeasure getBackgroundTimeout() {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return new TimeMeasure(0, TimeUnit.SECONDS);
        }
        return new TimeMeasure(getSession().getBackgroundTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout) {
        if (!isEnabled()) {
            Logger.track(TAG, "Attempt to access SessionController fields when disabled");
            return;
        }
        getSession().setBackgroundTimeout(backgroundTimeout.convert(TimeUnit.MILLISECONDS));
    }

    // Service method

    public boolean isEnabled() {
        return getTracker().getSession() != null;
    }

    // Private methods

    private Tracker getTracker() {
        return serviceProvider.getTracker();
    }

    private Session getSession() {
        return serviceProvider.getTracker().getSession();
    }
}
