package com.snowplowanalytics.snowplow.internal.session;

import androidx.annotation.NonNull;

import com.snowplowanalytics.snowplow.util.TimeMeasure;

public interface SessionConfigurationInterface {
    @NonNull
    TimeMeasure getForegroundTimeout();

    void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout);

    @NonNull
    TimeMeasure getBackgroundTimeout();

    void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout);
}
