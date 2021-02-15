package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.internal.session.SessionConfigurationInterface;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

public class SessionConfiguration implements SessionConfigurationInterface, Configuration {

    @NonNull
    public TimeMeasure foregroundTimeout;
    @NonNull
    public TimeMeasure backgroundTimeout;

    // Constructors

    public SessionConfiguration(@NonNull TimeMeasure foregroundTimeout, @NonNull TimeMeasure backgroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
        this.backgroundTimeout = backgroundTimeout;
    }

    // Getters and Setters

    @Override
    @NonNull
    public TimeMeasure getForegroundTimeout() {
        return foregroundTimeout;
    }

    @Override
    public void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
    }

    @Override
    @NonNull
    public TimeMeasure getBackgroundTimeout() {
        return backgroundTimeout;
    }

    @Override
    public void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout) {
        this.backgroundTimeout = backgroundTimeout;
    }

    // Copyable

    @Override
    @NonNull
    public Configuration copy() {
        return new SessionConfiguration(foregroundTimeout, backgroundTimeout);
    }
}
