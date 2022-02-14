package com.snowplowanalytics.snowplow.internal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.snowplowanalytics.snowplow.tracker.SessionState;
import com.snowplowanalytics.snowplow.util.TimeMeasure;

public interface SessionConfigurationInterface {

    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * foreground.
     */
    @NonNull
    TimeMeasure getForegroundTimeout();

    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * foreground.
     */
    void setForegroundTimeout(@NonNull TimeMeasure foregroundTimeout);

    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * background.
     */
    @NonNull
    TimeMeasure getBackgroundTimeout();

    /**
     * The amount of time that can elapse before the
     * session id is updated while the app is in the
     * background.
     */
    void setBackgroundTimeout(@NonNull TimeMeasure backgroundTimeout);

    /**
     * The callback called everytime the session is updated.
     */
    @Nullable
    Consumer<SessionState> getOnSessionUpdate();

    /**
     * The callback called everytime the session is updated.
     */
    void setOnSessionUpdate(@Nullable Consumer<SessionState> onSessionUpdate);
}
