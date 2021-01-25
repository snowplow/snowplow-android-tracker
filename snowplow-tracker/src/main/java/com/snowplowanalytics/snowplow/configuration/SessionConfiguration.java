package com.snowplowanalytics.snowplow.configuration;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.util.TimeMeasure;

public class SessionConfiguration implements Configuration {

    @NonNull
    public TimeMeasure foregroundTimeout;
    @NonNull
    public TimeMeasure backgroundTimeout;

    public SessionConfiguration(@NonNull TimeMeasure foregroundTimeout, @NonNull TimeMeasure backgroundTimeout) {
        this.foregroundTimeout = foregroundTimeout;
        this.backgroundTimeout = backgroundTimeout;
    }

    // Copyable

    @Override
    @NonNull
    public SessionConfiguration copy() {
        return new SessionConfiguration(foregroundTimeout, backgroundTimeout);
    }
}
